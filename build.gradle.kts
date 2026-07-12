plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.dokka) apply false
}

tasks.register("syncDocumentationVersion") {
    group = "documentation"
    description = "Updates documentation dependency snippets from LIB_VERSION in gradle.properties."

    val libraryVersion = providers.gradleProperty("LIB_VERSION")
    val documentationFiles = listOf(
        "README.md",
        "webside/content/docs/getting-started.md"
    ).map { layout.projectDirectory.file(it) }

    inputs.property("libraryVersion", libraryVersion)
    inputs.files(documentationFiles)
    outputs.files(documentationFiles)

    doLast {
        val version = libraryVersion.get()

        documentationFiles.forEach { regularFile ->
            val file = regularFile.asFile
            val original = file.readText()
            val updated = original
                .replace(
                    Regex("""io\.github\.ringga-dev:kmp_printer:[^")`\s]+"""),
                    "io.github.ringga-dev:kmp_printer:$version"
                )
                .replace(
                    Regex("""source version configured in this repository is `[^`]+`"""),
                    "source version configured in this repository is `$version`"
                )
                .replace(
                    Regex("""source tree is configured as `[^`]+`"""),
                    "source tree is configured as `$version`"
                )
                .replace(
                    Regex("""Current source version: `[^`]+`"""),
                    "Current source version: `$version`"
                )
                .replace(
                    Regex("""<version>[^<]+</version> <!-- sync-version -->"""),
                    "<version>$version</version> <!-- sync-version -->"
                )
                .replace(
                    Regex("""img\.shields\.io/badge/v[0-9]+\.[0-9]+\.[0-9]+"""),
                    "img.shields.io/badge/v$version"
                )
                .replace(
                    Regex("""releases/tag/v[0-9]+\.[0-9]+\.[0-9]+"""),
                    "releases/tag/v$version"
                )

            if (updated != original) {
                file.writeText(updated)
            }
        }
    }
}
