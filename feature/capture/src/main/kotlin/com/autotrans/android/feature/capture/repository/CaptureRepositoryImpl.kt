package com.autotrans.android.feature.capture.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.autotrans.android.domain.model.ImageData
import com.autotrans.android.domain.repository.CaptureRepository
import com.autotrans.android.feature.capture.store.ImageStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [CaptureRepository] using the Android MediaProjection API.
 *
 * ## Architecture
 * ```
 * MediaProjection
 *   └─> VirtualDisplay  (renders screen pixels into a Surface)
 *         └─> ImageReader (acquires Image frames from the Surface)
 *               └─> callbackFlow { trySend(ImageData) }
 *                     └─> .conflate()  ← drops frames if OCR is slow
 * ```
 *
 * ## Thread safety
 * [ImageReader.OnImageAvailableListener] fires on an arbitrary background thread.
 * [kotlinx.coroutines.channels.Channel.trySend] is thread-safe.
 *
 * ## Memory
 * Bitmaps are registered in [ImageStore] which enforces LRU eviction.
 * Raw [android.media.Image] is closed immediately after pixel extraction.
 */
@Singleton
class CaptureRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageStore: ImageStore,
) : CaptureRepository {

    private val _isCapturing = MutableStateFlow(false)
    override val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    override suspend fun captureScreen(): Result<ImageData> {
        return runCatching {
            val reader = imageReader ?: error("captureScreen() called before startContinuousCapture()")
            val image = reader.acquireLatestImage()
                ?: error("No image available")
            image.use { img ->
                img.planes[0].let { plane ->
                    val buffer = plane.buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val width = img.width
                    val height = img.height
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(plane.buffer.rewind())
                    imageStore.register(bitmap)
                }
            }
        }
    }

    /**
     * Starts the continuous capture session.
     *
     * This overload is for the legacy intervalMs-based API kept for interface compatibility.
     * The real MediaProjection flow is started via [startContinuousCapture(resultCode, data)].
     */
    override fun startContinuousCapture(intervalMs: Long): Flow<Result<ImageData>> {
        error("Use startContinuousCapture(resultCode, data) instead")
    }

    /**
     * Starts a [MediaProjection]-backed continuous capture flow.
     *
     * The flow:
     * 1. Obtains a [MediaProjection] token from the system.
     * 2. Creates an [ImageReader] sized to the screen.
     * 3. Creates a [VirtualDisplay] that feeds into the [ImageReader] surface.
     * 4. On each frame, acquires the latest [android.media.Image], converts to [Bitmap],
     *    registers in [ImageStore], and emits the resulting [ImageData].
     * 5. Uses `.conflate()` so slow OCR consumers drop stale frames.
     *
     * @param resultCode Result from [MediaProjectionManager.createScreenCaptureIntent].
     * @param data Intent from the permission result.
     */
    fun startContinuousCapture(resultCode: Int, data: Intent): Flow<Result<ImageData>> =
        callbackFlow {
            val metrics = getDisplayMetrics()
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, /* maxImages= */ 2)
            imageReader = reader

            val mgr = context.getSystemService(MediaProjectionManager::class.java)
            val projection = mgr.getMediaProjection(resultCode, data)
            mediaProjection = projection

            // Register a stop callback so we know when the user revokes permission
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Timber.i("MediaProjection stopped by system.")
                    _isCapturing.value = false
                    close() // close the callbackFlow
                }
            }, null)

            reader.setOnImageAvailableListener({ imageReader ->
                val image = imageReader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val plane = image.planes[0]
                    val rowStride = plane.rowStride
                    val pixelStride = plane.pixelStride
                    val w = image.width
                    val h = image.height
                    val bitmap = Bitmap.createBitmap(rowStride / pixelStride, h, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(plane.buffer)
                    // Crop to actual screen width if row padding exists
                    val cropped = if (bitmap.width > w) {
                        Bitmap.createBitmap(bitmap, 0, 0, w, h).also { bitmap.recycle() }
                    } else bitmap
                    val imageData = imageStore.register(cropped)
                    trySend(Result.success(imageData))
                } catch (e: Exception) {
                    Timber.e(e, "Error acquiring image frame")
                    trySend(Result.failure(e))
                } finally {
                    image.close()
                }
            }, null)

            val display = projection.createVirtualDisplay(
                "AutoTransCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null, null,
            )
            virtualDisplay = display
            _isCapturing.value = true
            Timber.i("VirtualDisplay started: ${width}x${height} @ ${density}dpi")

            awaitClose {
                Timber.d("callbackFlow closed — releasing capture resources")
                releaseResources()
            }
        }.conflate()

    override fun stopCapture() {
        releaseResources()
    }

    override suspend fun requestPermission(): Result<Unit> {
        // Permission is handled by CapturePermissionViewModel / UI layer
        return Result.success(Unit)
    }

    override fun releaseResources() {
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
        imageReader?.close()
        imageReader = null
        imageStore.clear()
        _isCapturing.value = false
        Timber.d("CaptureRepositoryImpl: resources released")
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    @Suppress("DEPRECATION")
    private fun getDisplayMetrics(): DisplayMetrics {
        val wm = context.getSystemService(WindowManager::class.java)
        return DisplayMetrics().also { wm.defaultDisplay.getMetrics(it) }
    }
}
