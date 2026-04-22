plugins {
    alias(libs.plugins.otpforwarder.android.feature)
}

android {
    namespace = "com.samsung.android.otpforwarder.feature.settings"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:security"))
}
