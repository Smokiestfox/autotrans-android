package com.autotrans.android.domain.model

/**
 * A single translation request sent to a [TranslationEngine].
 */
data class TranslationRequest(
    val text: String,
    val from: Language,
    val to: Language
)
