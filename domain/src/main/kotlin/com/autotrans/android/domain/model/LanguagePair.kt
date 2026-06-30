package com.autotrans.android.domain.model

/**
 * A source and target language pair used to configure the translation pipeline.
 */
data class LanguagePair(
    val source: Language,
    val target: Language
)
