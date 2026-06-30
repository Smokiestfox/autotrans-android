package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.repository.TranslationHistoryRepository
import kotlinx.coroutines.flow.first

/**
 * Exports all translation history as a formatted plain-text string.
 * Caller is responsible for writing the result to a file or sharing it.
 */
class ExportTranslationHistoryUseCase(
    private val repository: TranslationHistoryRepository
) {
    suspend operator fun invoke(): Result<String> = runCatching {
        val items = repository.getHistory(limit = Int.MAX_VALUE).first()
        items.joinToString(separator = "\n\n") { item ->
            "[${item.sourceLang} \u2192 ${item.targetLang}]\n" +
            "Original: ${item.originalText}\n" +
            "Translation: ${item.translatedText}"
        }
    }
}
