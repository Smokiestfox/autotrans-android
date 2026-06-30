package com.autotrans.android.domain.repository

import com.autotrans.android.domain.model.ImageData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstracts screen capture via MediaProjection.
 * Implementation lives in :feature:capture.
 */
interface CaptureRepository {
    /** True while continuous capture is active. */
    val isCapturing: StateFlow<Boolean>
    suspend fun captureScreen(): Result<ImageData>
    fun startContinuousCapture(intervalMs: Long): Flow<Result<ImageData>>
    fun stopCapture()
    suspend fun requestPermission(): Result<Unit>
    fun releaseResources()
}
