package com.samsung.android.otpforwarder.core.common.coroutines

import javax.inject.Qualifier

/**
 * Qualifier for the application-scoped [kotlinx.coroutines.CoroutineScope].
 * Provided by the DI graph; lives as long as the process and should be used
 * for work that must outlive individual ViewModels (e.g. collecting from
 * [SmsEventBus] into an in-memory repository).
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
