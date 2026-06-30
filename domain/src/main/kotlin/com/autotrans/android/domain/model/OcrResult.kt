package com.autotrans.android.domain.model

/**
 * Complete OCR result from a single screen frame.
 *
 * @param blocks Individual text blocks with positions.
 * @param fullText Concatenated text from all blocks (for cache key computation).
 * @param timestamp System time when recognition was performed.
 */
data class OcrResult(
    val blocks: List<OcrBlock>,
    val fullText: String,
    val timestamp: Long
)
