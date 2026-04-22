plugins {
    alias(libs.plugins.otpforwarder.android.library)
    alias(libs.plugins.otpforwarder.android.libraryCompose)
}

android {
    namespace = "com.samsung.android.otpforwarder.core.designsystem"
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
