package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.repository.TranslationHistoryRepository

/** Deletes all translation history items from Room. */
class ClearTranslationHistoryUseCase(
    private val repository: TranslationHistoryRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.clearAll()
}
