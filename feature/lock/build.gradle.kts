plugins {
    alias(libs.plugins.otpforwarder.android.feature)
}

android {
    namespace = "com.samsung.android.otpforwarder.feature.lock"
}

dependencies {
    implementation(project(":core:security"))
}
