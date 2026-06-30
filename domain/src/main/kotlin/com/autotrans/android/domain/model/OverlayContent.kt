package com.autotrans.android.domain.model

/**
 * Complete content to render in the overlay window.
 *
 * @param screenWidth Physical screen width in pixels (for BoundingBox → pixel conversion).
 * @param screenHeight Physical screen height in pixels.
 */
data class OverlayContent(
    val blocks: List<OverlayBlock>,
    val screenWidth: Int,
    val screenHeight: Int
)
