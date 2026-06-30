package com.autotrans.android.domain.model

/**
 * Represents the current state of the [TranslationPipeline].
 * Observed by [OverlayWindowManager] to update the overlay UI.
 */
sealed interface PipelineState {
    data object Idle : PipelineState
    data object Capturing : PipelineState
    data class Processing(val stage: PipelineStage) : PipelineState
    data class Success(val result: TranslationResult) : PipelineState
    data class Error(val cause: Throwable, val fatal: Boolean = false) : PipelineState
}

/** Identifies which stage of the pipeline is currently executing. */
enum class PipelineStage {
    CAPTURE, OCR, POST_PROCESS, TRANSLATE, LAYOUT
}
