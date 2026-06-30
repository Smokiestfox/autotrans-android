package com.autotrans.android.domain.repository

import com.autotrans.android.domain.model.Language
import com.autotrans.android.domain.model.TranslationResult

/**
 * Abstracts translation and language detection.
 * Includes LRU caching — callers need not implement caching themselves.
 * Implementation lives in :feature:translator.
 */
interface TranslationRepository {
    suspend fun translate(text: String, from: Language, to: Language): Result<TranslationResult>
    suspend fun detectLanguage(text: String): Result<Language>
}
