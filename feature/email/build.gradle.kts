plugins {
    alias(libs.plugins.otpforwarder.android.feature)
}

android {
    namespace = "com.samsung.android.otpforwarder.feature.email"
}

dependencies {
    implementation(project(":core:data"))
}
