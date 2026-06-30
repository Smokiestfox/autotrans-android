package com.autotrans.android.domain.model

/**
 * A single detected text block from the OCR engine.
 *
 * @param text Raw detected text for this block.
 * @param boundingBox Normalized position on screen in [0..1] coordinates.
 * @param confidence Recognition confidence in [0.0, 1.0]. Blocks below 0.6 are filtered.
 */
data class OcrBlock(
    val text: String,
    val boundingBox: BoundingBox,
    val confidence: Float
)
