package com.samsung.android.otpforwarder.core.sms.di

import android.content.Context
import androidx.work.WorkManager
import com.samsung.android.otpforwarder.core.domain.DetectOtpUseCase
import com.samsung.android.otpforwarder.core.domain.OtpDetector
import com.samsung.android.otpforwarder.core.sms.OtpDetectorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    /**
     * Provide the app-wide [WorkManager] instance.
     *
     * [WorkManager] is configured via [Configuration.Provider] on
     * [OtpForwarderApplication] (using [HiltWorkerFactory]), so
     * [WorkManager.getInstance] must be called *after* Application.onCreate —
     * which is guaranteed here because Hilt injects singletons lazily on first use.
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
