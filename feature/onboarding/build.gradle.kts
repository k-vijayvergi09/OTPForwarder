plugins {
    alias(libs.plugins.otpforwarder.android.feature)
}

android {
    namespace = "com.samsung.android.otpforwarder.feature.onboarding"
}

dependencies {
    implementation(project(":core:model"))
}
