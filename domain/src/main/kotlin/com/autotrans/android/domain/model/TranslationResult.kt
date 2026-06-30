package com.autotrans.android.domain.model

/**
 * The result of a successful translation operation.
 *
 * @param originalText The input text that was translated.
 * @param translatedText The translated output text.
 * @param sourceLanguage Detected or specified source language BCP-47 code.
 * @param targetLanguage Target language BCP-47 code.
 */
data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String
)
