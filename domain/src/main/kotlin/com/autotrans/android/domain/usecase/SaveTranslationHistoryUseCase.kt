package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.model.TranslationHistoryItem
import com.autotrans.android.domain.repository.TranslationHistoryRepository

/** Saves a completed translation to Room history. */
class SaveTranslationHistoryUseCase(
    private val repository: TranslationHistoryRepository
) {
    suspend operator fun invoke(item: TranslationHistoryItem): Result<Unit> =
        repository.save(item)
}
