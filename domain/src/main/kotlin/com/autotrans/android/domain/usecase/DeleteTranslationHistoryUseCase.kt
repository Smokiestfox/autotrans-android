package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.repository.TranslationHistoryRepository

/** Deletes a single history item by its Room primary key. */
class DeleteTranslationHistoryUseCase(
    private val repository: TranslationHistoryRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> = repository.delete(id)
}
