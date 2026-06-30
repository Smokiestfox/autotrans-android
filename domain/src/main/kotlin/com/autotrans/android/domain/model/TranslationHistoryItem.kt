package com.autotrans.android.domain.model

/**
 * A single entry in the translation history.
 *
 * @param id Room-generated primary key. 0 means not yet persisted.
 * @param isFavorite Whether the user has starred this item.
 */
data class TranslationHistoryItem(
    val id: Long = 0,
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val timestamp: Long,
    val isFavorite: Boolean = false
)
