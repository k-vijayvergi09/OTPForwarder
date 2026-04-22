package com.otpforwarder.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Shared Android + Kotlin configuration applied to every Android module
 * (application or library) via the convention plugins.
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.compileSdk = libs.versionInt("androidCompileSdk")
    commonExtension.defaultConfig.minSdk = libs.versionInt("androidMinSdk")
    commonExtension.compileOptions.sourceCompatibility = JavaVersion.VERSION_21
    commonExtension.compileOptions.targetCompatibility = JavaVersion.VERSION_21

    configureKotlin<KotlinAndroidProjectExtension>()
}
