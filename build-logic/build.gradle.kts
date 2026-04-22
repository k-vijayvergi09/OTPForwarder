plugins {
    id("base")
}

// Make :build-logic:clean also trigger clean in all subprojects of build-logic
tasks.named("clean") {
    subprojects.forEach { subproject ->
        dependsOn(subproject.tasks.matching { it.name == "clean" })
    }
}
