package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.model.AppSettings
import com.autotrans.android.domain.model.PipelineState
import com.autotrans.android.domain.repository.CaptureRepository
import com.autotrans.android.domain.repository.OcrRepository
import com.autotrans.android.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.mapLatest

/**
 * Starts the continuous Capture → OCR → Translate pipeline.
 * Uses [Flow.conflate] + [Flow.mapLatest] to prevent frame backlog.
 * See PIPELINE.md §4 for concurrency design.
 */
class StartContinuousTranslationUseCase(
    private val captureRepository: CaptureRepository,
    private val ocrRepository: OcrRepository,
    private val translationRepository: TranslationRepository
) {
    operator fun invoke(settings: AppSettings): Flow<PipelineState> =
        captureRepository.startContinuousCapture(settings.captureIntervalMs)
            .conflate()
            .mapLatest { frameResult ->
                val imageData = frameResult
                    .getOrElse { return@mapLatest PipelineState.Error(it) }
                val ocrResult = ocrRepository.recognizeText(imageData)
                    .getOrElse { return@mapLatest PipelineState.Error(it) }
                if (ocrResult.fullText.isBlank()) return@mapLatest PipelineState.Capturing
                val translationResult = translationRepository.translate(
                    text = ocrResult.fullText,
                    from = settings.sourceLanguage,
                    to   = settings.targetLanguage
                ).getOrElse { return@mapLatest PipelineState.Error(it) }
                PipelineState.Success(translationResult)
            }
}
