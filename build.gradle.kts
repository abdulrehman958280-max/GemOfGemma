plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

// CI-only source preparation keeps user-visible branding consistent across modules
// while the permanent theme/resources remain source-controlled.
val prepareOmniCodeReleaseSources = tasks.register("prepareOmniCodeReleaseSources") {
    onlyIf { System.getenv("CI") == "true" }
    doLast {
        val sourceRoots = listOf(
            file("app/src/main"),
            file("ui/src/main")
        )
        sourceRoots.forEach { root ->
            if (!root.exists()) return@forEach
            root.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }
                .forEach { source ->
                    val original = source.readText()
                    val updated = original
                        .replace("GemOfGemma", "OmniCode")
                        .replace("Gem of Gemma", "OmniCode")
                    if (updated != original) source.writeText(updated)
                }
        }
    }
}

gradle.projectsEvaluated {
    subprojects.forEach { project ->
        project.tasks.matching { it.name == "preBuild" }.configureEach {
            dependsOn(prepareOmniCodeReleaseSources)
        }
    }
}
