package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.model.Language
import com.autotrans.android.domain.repository.TranslationRepository

/** Detects the language of the provided text using the active translation engine. */
class DetectLanguageUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(text: String): Result<Language> = repository.detectLanguage(text)
}
