plugins {
    id("base")
}

// Make :feature:clean also trigger clean in all subprojects of feature
tasks.named("clean") {
    subprojects.forEach { subproject ->
        dependsOn(subproject.tasks.matching { it.name == "clean" })
    }
}
