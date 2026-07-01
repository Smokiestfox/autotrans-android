package com.autotrans.android.feature.capture.di

import com.autotrans.android.domain.repository.CaptureRepository
import com.autotrans.android.feature.capture.repository.CaptureRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [CaptureRepositoryImpl] to the [CaptureRepository] interface.
 * Installed in [SingletonComponent] so the same instance is shared across the app.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureModule {

    @Binds
    @Singleton
    abstract fun bindCaptureRepository(
        impl: CaptureRepositoryImpl,
    ): CaptureRepository
}
