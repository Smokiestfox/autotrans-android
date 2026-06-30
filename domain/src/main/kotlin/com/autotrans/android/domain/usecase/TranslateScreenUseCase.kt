package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.model.LanguagePair
import com.autotrans.android.domain.model.TranslationResult
import com.autotrans.android.domain.repository.CaptureRepository
import com.autotrans.android.domain.repository.OcrRepository
import com.autotrans.android.domain.repository.TranslationRepository

/**
 * Performs a single screen capture → OCR → translation cycle.
 * Used for one-shot "Translate Now" button.
 */
class TranslateScreenUseCase(
    private val captureRepository: CaptureRepository,
    private val ocrRepository: OcrRepository,
    private val translationRepository: TranslationRepository
) {
    suspend operator fun invoke(languagePair: LanguagePair): Result<TranslationResult> {
        val imageData = captureRepository.captureScreen()
            .getOrElse { return Result.failure(it) }
        val ocrResult = ocrRepository.recognizeText(imageData)
            .getOrElse { return Result.failure(it) }
        if (ocrResult.fullText.isBlank()) {
            return Result.failure(IllegalStateException("No text detected on screen"))
        }
        return translationRepository.translate(
            text = ocrResult.fullText,
            from = languagePair.source,
            to   = languagePair.target
        )
    }
}
