package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.model.TranslationHistoryItem
import com.autotrans.android.domain.repository.TranslationHistoryRepository
import kotlinx.coroutines.flow.Flow

/** Observes the translation history list from Room. */
class GetTranslationHistoryUseCase(
    private val repository: TranslationHistoryRepository
) {
    operator fun invoke(limit: Int = 50): Flow<List<TranslationHistoryItem>> =
        repository.getHistory(limit)
}
