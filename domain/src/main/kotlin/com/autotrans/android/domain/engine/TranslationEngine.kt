package com.autotrans.android.domain.engine

import com.autotrans.android.domain.model.EngineConfig
import com.autotrans.android.domain.model.Language
import com.autotrans.android.domain.model.TranslationEngineType
import com.autotrans.android.domain.model.TranslationRequest
import com.autotrans.android.domain.model.TranslationResult

/**
 * Plugin interface for translation engines.
 * New engines are added by implementing this interface and registering via Hilt @IntoMap.
 * See CONTRIBUTOR_GUIDE.md §8 for the full walkthrough.
 */
interface TranslationEngine {
    val engineType: TranslationEngineType
    val requiresApiKey: Boolean
    val supportsOffline: Boolean
    suspend fun initialize(config: EngineConfig): Result<Unit>
    suspend fun translate(request: TranslationRequest): Result<TranslationResult>
    suspend fun detectLanguage(text: String): Result<Language>
    suspend fun getSupportedLanguages(): Result<List<Language>>
    fun release()
}
