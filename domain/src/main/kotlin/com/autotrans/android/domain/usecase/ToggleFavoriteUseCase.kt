package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.repository.TranslationHistoryRepository

/** Toggles the isFavorite flag on a history item. */
class ToggleFavoriteUseCase(
    private val repository: TranslationHistoryRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> = repository.toggleFavorite(id)
}
