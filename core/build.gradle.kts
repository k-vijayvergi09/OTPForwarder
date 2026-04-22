plugins {
    id("base")
}

// Optional: Make :core:clean also trigger clean in all subprojects of core
tasks.named("clean") {
    subprojects.forEach { subproject ->
        dependsOn(subproject.tasks.matching { it.name == "clean" })
    }
}
