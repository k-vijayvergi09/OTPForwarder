plugins {
    alias(libs.plugins.otpforwarder.android.feature)
}

android {
    namespace = "com.samsung.android.otpforwarder.feature.rules"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:model"))
}
