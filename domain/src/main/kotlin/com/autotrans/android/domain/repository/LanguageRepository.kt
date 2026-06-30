package com.autotrans.android.domain.repository

import com.autotrans.android.domain.model.DownloadProgress
import com.autotrans.android.domain.model.Language
import kotlinx.coroutines.flow.Flow

/**
 * Manages language model downloads for offline engines (e.g. ML Kit Translation).
 * Implementation lives in :feature:translator.
 */
interface LanguageRepository {
    suspend fun getInstalledLanguages(): Result<List<Language>>
    suspend fun getSupportedLanguages(): Result<List<Language>>
    fun downloadModel(language: Language): Flow<DownloadProgress>
    suspend fun deleteModel(language: Language): Result<Unit>
}
