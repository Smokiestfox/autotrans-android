package com.autotrans.android.domain.model

/**
 * Opaque handle to a captured screen frame stored in ImageStore.
 * Bitmap objects never cross module boundaries — only this ID is passed around.
 */
@JvmInline
value class ImageData(val id: String)
