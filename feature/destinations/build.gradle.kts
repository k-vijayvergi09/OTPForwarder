plugins {
    alias(libs.plugins.otpforwarder.android.feature)
}

android {
    namespace = "com.samsung.android.otpforwarder.feature.destinations"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
}
