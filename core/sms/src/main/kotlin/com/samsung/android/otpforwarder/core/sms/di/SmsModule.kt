package com.samsung.android.otpforwarder.core.sms.di

import com.samsung.android.otpforwarder.core.domain.DetectOtpUseCase
import com.samsung.android.otpforwarder.core.domain.OtpDetector
import com.samsung.android.otpforwarder.core.sms.OtpDetectorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SmsBindModule {

    /** Bind the regex implementation to the [OtpDetector] interface. */
    @Binds
    @Singleton
    abstract fun bindOtpDetector(impl: OtpDetectorImpl): OtpDetector
}

@Module
@InstallIn(SingletonComponent::class)
object SmsProvideModule {

    /**
     * Provide [DetectOtpUseCase] — constructed manually because it lives in
     * `:core:domain` (a plain JVM module) and cannot carry Hilt annotations itself.
     */
    @Provides
    @Singleton
    fun provideDetectOtpUseCase(detector: OtpDetector): DetectOtpUseCase =
        DetectOtpUseCase(detector)
}
