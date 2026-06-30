package com.autotrans.android.domain.engine

import com.autotrans.android.domain.model.BoundingBox
import com.autotrans.android.domain.model.ImageData
import com.autotrans.android.domain.model.OcrEngineType
import com.autotrans.android.domain.model.OcrResult

/**
 * Plugin interface for OCR engines.
 * New engines are added by implementing this interface and registering via Hilt @IntoMap.
 * See CONTRIBUTOR_GUIDE.md §7 for the full walkthrough.
 */
interface OcrEngine {
    val engineType: OcrEngineType
    val supportedLanguages: List<String>
    suspend fun initialize(): Result<Unit>
    suspend fun recognize(imageData: ImageData): Result<OcrResult>
    suspend fun recognizeInRegion(imageData: ImageData, region: BoundingBox): Result<OcrResult>
    fun release()
}
