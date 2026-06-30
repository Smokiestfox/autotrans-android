package com.autotrans.android.domain.model

/**
 * Normalized bounding box with coordinates in [0.0, 1.0] relative to screen dimensions.
 * All implementations must ensure left < right and top < bottom.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}
