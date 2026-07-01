package com.autotrans.android.core.testing.fake

import com.autotrans.android.domain.model.ImageData
import com.autotrans.android.domain.repository.CaptureRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Test double for [CaptureRepository].
 *
 * Emits pre-defined [ImageData] frames at a fixed interval without needing
 * a real device screen or MediaProjection permission.
 *
 * ## Usage
 * ```kotlin
 * val fakeCapture = FakeCaptureRepository()
 * fakeCapture.emitFrames = listOf(ImageData("frame-1"), ImageData("frame-2"))
 * ```
 */
class FakeCaptureRepository : CaptureRepository {

    private val _isCapturing = MutableStateFlow(false)
    override val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    /** Frames to emit when [startContinuousCapture] is called. */
    var emitFrames: List<ImageData> = emptyList()

    /** Delay between emitted frames (ms). */
    var emitIntervalMs: Long = 100L

    /** If set, [startContinuousCapture] will emit this failure for each frame. */
    var failWith: Throwable? = null

    /** Tracks how many times [stopCapture] was called. */
    var stopCallCount: Int = 0
        private set

    /** Tracks how many times [releaseResources] was called. */
    var releaseCallCount: Int = 0
        private set

    override suspend fun captureScreen(): Result<ImageData> {
        return failWith?.let { Result.failure(it) }
            ?: emitFrames.firstOrNull()?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No frames configured"))
    }

    override fun startContinuousCapture(intervalMs: Long): Flow<Result<ImageData>> = flow {
        _isCapturing.value = true
        try {
            for (frame in emitFrames) {
                delay(emitIntervalMs)
                emit(
                    failWith?.let { Result.failure(it) }
                        ?: Result.success(frame)
                )
            }
        } finally {
            _isCapturing.value = false
        }
    }

    override fun stopCapture() {
        stopCallCount++
        _isCapturing.value = false
    }

    override suspend fun requestPermission(): Result<Unit> = Result.success(Unit)

    override fun releaseResources() {
        releaseCallCount++
        _isCapturing.value = false
    }

    fun reset() {
        emitFrames = emptyList()
        failWith = null
        stopCallCount = 0
        releaseCallCount = 0
        _isCapturing.value = false
    }
}
