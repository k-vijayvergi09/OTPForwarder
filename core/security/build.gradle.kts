plugins {
    alias(libs.plugins.otpforwarder.android.library)
    alias(libs.plugins.otpforwarder.android.hilt)
}

android {
    namespace = "com.samsung.android.otpforwarder.core.security"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))

    implementation(libs.androidx.security.crypto)
    implementation(libs.google.tink.android)
    implementation(libs.argon2.jvm)
    implementation(libs.androidx.biometric)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk.core)
}
