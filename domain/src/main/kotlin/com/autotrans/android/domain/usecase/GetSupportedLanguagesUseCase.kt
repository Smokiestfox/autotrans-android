package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.model.Language
import com.autotrans.android.domain.repository.LanguageRepository

/** Returns all languages supported by the current translation engine. */
class GetSupportedLanguagesUseCase(
    private val repository: LanguageRepository
) {
    suspend operator fun invoke(): Result<List<Language>> = repository.getSupportedLanguages()
}
