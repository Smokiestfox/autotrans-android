package com.autotrans.android.domain.model

/**
 * Progress update for a language model download.
 *
 * @param percent Download progress from 0 to 100.
 * @param isDone True when download is complete.
 * @param error Non-null when download failed.
 */
data class DownloadProgress(
    val language: Language,
    val percent: Int,
    val isDone: Boolean = false,
    val error: Throwable? = null
)
