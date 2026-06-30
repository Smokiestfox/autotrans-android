package com.autotrans.android.domain.model

/**
 * User-configurable application settings persisted via DataStore.
 * Observed as a [kotlinx.coroutines.flow.Flow] by the pipeline and ViewModels.
 */
data class AppSettings(
    val sourceLanguage: Language = Language.Auto,
    val targetLanguage: Language = Language.Specific("vi"),
    val captureIntervalMs: Long = 1_000L,
    val autoTranslate: Boolean = false,
    val overlayOpacity: Float = 0.9f,
    val ocrEngineType: OcrEngineType = OcrEngineType.ML_KIT,
    val translationEngineType: TranslationEngineType = TranslationEngineType.ML_KIT
)
