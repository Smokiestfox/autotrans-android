package com.autotrans.android.domain.model

/** Identifies a registered translation engine implementation. */
enum class TranslationEngineType {
    ML_KIT,
    GOOGLE_CLOUD,
    LIBRE_TRANSLATE,
    DEEPL
}
