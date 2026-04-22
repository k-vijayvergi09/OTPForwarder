package com.samsung.android.otpforwarder.core.data.di

import com.samsung.android.otpforwarder.core.common.coroutines.ApplicationScope
import com.samsung.android.otpforwarder.core.data.InMemoryForwardingRepository
import com.samsung.android.otpforwarder.core.domain.ForwardingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindModule {

    @Binds
    @Singleton
    abstract fun bindForwardingRepository(
        impl: InMemoryForwardingRepository,
    ): ForwardingRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvideModule {

    /**
     * Application-scoped [CoroutineScope] for work that must outlive ViewModels
     * (e.g. collecting from [com.samsung.android.otpforwarder.core.sms.SmsEventBus]).
     * Uses [SupervisorJob] so a failing child doesn't cancel siblings.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob())
}
