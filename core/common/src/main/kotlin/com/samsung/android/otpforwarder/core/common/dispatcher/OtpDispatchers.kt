package com.samsung.android.otpforwarder.core.common.dispatcher

import javax.inject.Qualifier

/**
 * Qualifiers for dispatchers so modules can inject the right CoroutineDispatcher
 * for their workload.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: OtpDispatchers)

enum class OtpDispatchers {
    Default,
    IO,
}
