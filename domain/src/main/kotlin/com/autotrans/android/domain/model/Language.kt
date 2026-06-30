package com.autotrans.android.domain.model

/**
 * Represents a language supported by OCR or translation engines.
 * Domain-level abstraction — no Android or library dependencies.
 */
sealed interface Language {
    val code: String
    val displayName: String

    /** Automatically detect the source language. Only valid as a source language. */
    data object Auto : Language {
        override val code: String = "auto"
        override val displayName: String = "Auto-detect"
    }

    /** A specific BCP-47 language code (e.g. "vi", "ja", "en"). */
    data class Specific(override val code: String) : Language {
        override val displayName: String
            get() = java.util.Locale(code).displayLanguage.ifBlank { code }
    }
}
