package com.autotrans.android.domain.model

/**
 * Configuration passed to [TranslationEngine.initialize].
 *
 * @param apiKey Required for cloud-based engines. Store via EncryptedSharedPreferences.
 * @param endpointUrl Custom server URL for self-hosted engines (e.g. LibreTranslate).
 * @param timeoutMs Network timeout for remote engine requests.
 */
data class EngineConfig(
    val apiKey: String? = null,
    val endpointUrl: String? = null,
    val timeoutMs: Long = 10_000L
)
