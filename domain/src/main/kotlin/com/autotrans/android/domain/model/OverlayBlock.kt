package com.autotrans.android.domain.model

/**
 * A single translation block rendered in the overlay.
 *
 * @param id Stable unique identifier for Compose key() usage.
 * @param position Normalized [BoundingBox] in [0..1] coordinates.
 * @param fontSize Suggested font size in sp.
 */
data class OverlayBlock(
    val id: String,
    val originalText: String,
    val translatedText: String,
    val position: BoundingBox,
    val fontSize: Float
)
