package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.repository.CaptureRepository

/** Stops the continuous screen capture pipeline. */
class StopContinuousTranslationUseCase(
    private val captureRepository: CaptureRepository
) {
    operator fun invoke() = captureRepository.stopCapture()
}
