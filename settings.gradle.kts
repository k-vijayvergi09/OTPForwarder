pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OTPForwarder"

include(":app")

// Core modules
include(":core:common")
include(":core:designsystem")
include(":core:model")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:network")
include(":core:domain")
include(":core:security")
include(":core:sms")

// Feature modules
include(":feature:onboarding")
include(":feature:home")
include(":feature:rules")
include(":feature:logs")
include(":feature:settings")
include(":feature:lock")
include(":feature:email")
