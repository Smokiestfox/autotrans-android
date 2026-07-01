package com.autotrans.android.feature.capture.store

import android.graphics.Bitmap
import com.autotrans.android.domain.model.ImageData
import timber.log.Timber
import java.util.LinkedHashMap
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe in-memory registry that manages [Bitmap] lifecycles.
 *
 * ## Design
 * - Stores at most [maxEntries] bitmaps (default: 3).
 * - Uses an LRU (Least Recently Used) eviction policy — the oldest
 *   frame is recycled when the store is full.
 * - The domain layer only ever sees an opaque [ImageData] ID; actual
 *   [Bitmap] objects never escape this module.
 * - All write operations are `@Synchronized` to be safe for concurrent
 *   calls from the `ImageReader` callback thread and the main thread.
 *
 * ## Downsampling
 * Incoming bitmaps taller than [maxHeightPx] (720 px) are downsampled
 * to preserve memory and improve OCR throughput.
 */
@Singleton
class ImageStore @Inject constructor() {

    private val maxEntries: Int = 3
    private val maxHeightPx: Int = 720

    /** LRU map: insertion order maintained, eldest evicted on overflow. */
    private val cache: LinkedHashMap<String, Bitmap> =
        object : LinkedHashMap<String, Bitmap>(maxEntries + 1, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, Bitmap>?): Boolean {
                val shouldEvict = size > maxEntries
                if (shouldEvict && eldest != null) {
                    Timber.v("ImageStore: evicting frame ${eldest.key}")
                    eldest.value.recycle()
                }
                return shouldEvict
            }
        }

    /**
     * Registers a [Bitmap], downsampling it if necessary.
     *
     * @param bitmap The raw bitmap from [android.media.Image].
     * @return An [ImageData] handle for the stored frame.
     */
    @Synchronized
    fun register(bitmap: Bitmap): ImageData {
        val downsampled = downsampleIfNeeded(bitmap)
        val id = UUID.randomUUID().toString()
        cache[id] = downsampled
        Timber.v("ImageStore: registered $id (${downsampled.width}x${downsampled.height})")
        return ImageData(id)
    }

    /**
     * Retrieves the [Bitmap] associated with [imageData].
     * Returns `null` if the frame was already evicted.
     */
    @Synchronized
    fun getBitmap(imageData: ImageData): Bitmap? = cache[imageData.id]

    /**
     * Explicitly removes and recycles a specific frame.
     * Callers should use this after the OCR pipeline is done with a frame.
     */
    @Synchronized
    fun release(imageData: ImageData) {
        cache.remove(imageData.id)?.also {
            if (!it.isRecycled) it.recycle()
            Timber.v("ImageStore: released ${imageData.id}")
        }
    }

    /** Recycles all stored bitmaps and clears the cache. Call on service stop. */
    @Synchronized
    fun clear() {
        cache.values.forEach { if (!it.isRecycled) it.recycle() }
        cache.clear()
        Timber.d("ImageStore: cleared")
    }

    /** Returns the number of frames currently stored (for testing). */
    @Synchronized
    fun size(): Int = cache.size

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * If [src] has more rows than [maxHeightPx], creates a scaled copy.
     * If the source is already within bounds, returns [src] unchanged.
     */
    private fun downsampleIfNeeded(src: Bitmap): Bitmap {
        if (src.height <= maxHeightPx) return src
        val scale = maxHeightPx.toFloat() / src.height
        val newWidth = (src.width * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(src, newWidth, maxHeightPx, /* filter= */ true)
        if (scaled !== src) src.recycle() // recycle original only when a new copy was created
        Timber.v("ImageStore: downsampled ${src.width}x${src.height} → ${scaled.width}x${scaled.height}")
        return scaled
    }
}
