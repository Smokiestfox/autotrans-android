package com.autotrans.android.domain.repository

import com.autotrans.android.domain.model.BoundingBox
import com.autotrans.android.domain.model.ImageData
import com.autotrans.android.domain.model.OcrResult

/**
 * Abstracts text recognition from a captured screen image.
 * Implementation lives in :feature:ocr.
 */
interface OcrRepository {
    suspend fun recognizeText(imageData: ImageData): Result<OcrResult>
    suspend fun recognizeInRegion(imageData: ImageData, region: BoundingBox): Result<OcrResult>
}
