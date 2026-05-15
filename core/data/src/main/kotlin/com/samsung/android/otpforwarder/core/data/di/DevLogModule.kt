package com.samsung.android.otpforwarder.core.data.di

import com.samsung.android.otpforwarder.core.data.InMemoryDevLogRepository
import com.samsung.android.otpforwarder.core.domain.DevLogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DevLogModule {

    @Binds
    @Singleton
    abstract fun bindDevLogRepository(
        impl: InMemoryDevLogRepository,
    ): DevLogRepository
}
