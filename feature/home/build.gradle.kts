plugins {
    alias(libs.plugins.otpforwarder.android.feature)
}

android {
    namespace = "com.samsung.android.otpforwarder.feature.home"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:sms"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
}
