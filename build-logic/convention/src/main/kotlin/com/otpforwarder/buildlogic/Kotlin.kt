package com.otpforwarder.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * Centralised Kotlin compiler-options config used by both Android and pure-JVM modules.
 * Forces JVM 21 so all modules emit class files at the same target as the JDK toolchain.
 */
internal inline fun <reified T : KotlinProjectExtension> Project.configureKotlin() {
    extensions.configure<T> {
        when (this) {
            is KotlinAndroidProjectExtension -> compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
            is KotlinJvmProjectExtension -> compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }
}

/**
 * Configuration for pure-JVM (non-Android) Kotlin modules: domain, model, etc.
 */
internal fun Project.configureKotlinJvm() {
    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        sourceCompatibility = org.gradle.api.JavaVersion.VERSION_21
        targetCompatibility = org.gradle.api.JavaVersion.VERSION_21
    }
    configureKotlin<KotlinJvmProjectExtension>()
}
