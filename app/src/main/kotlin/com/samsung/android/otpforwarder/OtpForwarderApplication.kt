package com.samsung.android.otpforwarder

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.samsung.android.otpforwarder.core.sms.ForwardingDispatcher
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class OtpForwarderApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Eagerly instantiated singleton that subscribes to [SmsEventBus] and
     * enqueues [ForwardingWorker] for each detected OTP.
     *
     * Field-injecting it here guarantees it is created and [started][ForwardingDispatcher.start]
     * before any Activity or BroadcastReceiver runs, so no OTP event is ever missed.
     */
    @Inject
    lateinit var forwardingDispatcher: ForwardingDispatcher

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // super.onCreate() triggers Hilt injection — both lateinit fields are
        // populated by the time we reach this line.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        forwardingDispatcher.start()
    }
}
