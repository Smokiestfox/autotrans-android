package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.model.Language
import com.autotrans.android.domain.repository.LanguageRepository

/** Returns the list of language models currently installed on device. */
class GetInstalledLanguagesUseCase(
    private val repository: LanguageRepository
) {
    suspend operator fun invoke(): Result<List<Language>> = repository.getInstalledLanguages()
}
