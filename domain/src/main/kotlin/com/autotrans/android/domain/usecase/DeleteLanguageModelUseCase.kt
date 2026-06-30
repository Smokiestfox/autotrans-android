package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.model.Language
import com.autotrans.android.domain.repository.LanguageRepository

/** Removes a downloaded language model to free storage. */
class DeleteLanguageModelUseCase(
    private val repository: LanguageRepository
) {
    suspend operator fun invoke(language: Language): Result<Unit> =
        repository.deleteModel(language)
}
