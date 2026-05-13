package com.samsung.android.otpforwarder.core.network.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for network-layer dependencies.
 *
 * [EmailSender] is provided automatically by Hilt via its @Inject constructor.
 * This module is a placeholder for future additions (e.g. a Ktor HttpClient
 * for any backend API calls).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule
