import com.otpforwarder.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for `:feature:*` modules. Applies the Android library +
 * Compose conventions, wires Hilt, and adds the standard set of dependencies
 * every feature needs (lifecycle, navigation, orbit, hilt-nav-compose).
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("otpforwarder.android.library")
            pluginManager.apply("otpforwarder.android.library.compose")
            pluginManager.apply("otpforwarder.android.hilt")

            dependencies {
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:common"))
                "implementation"(project(":core:domain"))

                "implementation"(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-navigation-compose").get())
                "implementation"(libs.findLibrary("hilt-navigation-compose").get())
                "implementation"(libs.findLibrary("orbit-core").get())
                "implementation"(libs.findLibrary("orbit-viewmodel").get())
                "implementation"(libs.findLibrary("orbit-compose").get())
                "implementation"(libs.findLibrary("kotlinx-coroutines-android").get())

                "testImplementation"(libs.findLibrary("junit4").get())
                "testImplementation"(libs.findLibrary("kotlinx-coroutines-test").get())
                "testImplementation"(libs.findLibrary("mockk-core").get())
                "testImplementation"(libs.findLibrary("turbine").get())
                "testImplementation"(libs.findLibrary("orbit-test").get())
            }
        }
    }
}
