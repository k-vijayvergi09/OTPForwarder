package com.samsung.android.otpforwarder

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom [AndroidJUnitRunner] that replaces the real [Application] with
 * [HiltTestApplication] during instrumentation tests.
 *
 * [HiltTestApplication] exposes a Hilt component that honours
 * [@TestInstallIn] overrides and [@UninstallModules] on each test class,
 * while still providing all real production modules as defaults.
 *
 * Registered as the test runner in app/build.gradle.kts via:
 *   testInstrumentationRunner = "com.samsung.android.otpforwarder.HiltTestRunner"
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader,
        className: String,
        context: Context,
    ): Application = super.newApplication(
        classLoader,
        HiltTestApplication::class.java.name,
        context,
    )
}
