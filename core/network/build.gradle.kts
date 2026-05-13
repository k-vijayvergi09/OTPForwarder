plugins {
    alias(libs.plugins.otpforwarder.android.library)
    alias(libs.plugins.otpforwarder.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.samsung.android.otpforwarder.core.network"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))

    // SMTP (Android-safe JavaMail — no java.desktop/AWT dependencies)
    implementation(libs.android.mail)
    implementation(libs.android.activation)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.timber)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk.core)
}
