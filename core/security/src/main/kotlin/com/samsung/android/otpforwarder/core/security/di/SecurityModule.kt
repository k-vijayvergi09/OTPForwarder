package com.samsung.android.otpforwarder.core.security.di

import com.samsung.android.otpforwarder.core.domain.EmailCredentialRepository
import com.samsung.android.otpforwarder.core.security.GmailCredentialStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindEmailCredentialRepository(
        impl: GmailCredentialStore,
    ): EmailCredentialRepository
}
