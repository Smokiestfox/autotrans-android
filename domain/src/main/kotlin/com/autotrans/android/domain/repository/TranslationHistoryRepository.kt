package com.autotrans.android.domain.repository

import com.autotrans.android.domain.model.TranslationHistoryItem
import kotlinx.coroutines.flow.Flow

/**
 * Persists and queries translation history via Room.
 * Implementation lives in :data.
 */
interface TranslationHistoryRepository {
    fun getHistory(limit: Int = 50): Flow<List<TranslationHistoryItem>>
    suspend fun save(item: TranslationHistoryItem): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
    suspend fun clearAll(): Result<Unit>
    suspend fun toggleFavorite(id: Long): Result<Unit>
}
