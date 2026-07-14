plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

group = project.findProperty("LIB_GROUP")?.toString() ?: "io.github.ringga-dev"
version = project.findProperty("LIB_VERSION")?.toString() ?: "1.0.0"

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":printer"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// ─── Publishing ────────────────────────────────────

publishing {
    repositories {
        maven {
            name = "LocalRepo"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

afterEvaluate {
    publishing.publications.withType<MavenPublication>().forEach { pub ->
        pub.artifactId = when (pub.name) {
            "kotlinMultiplatform" -> "kmp_printer:label"
            "jvm" -> "kmp_printer:label-jvm"
            else -> "kmp_printer:label-${pub.name.replaceFirstChar { it.lowercase() }}"
        }
    }
}

signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY") ?: (project.findProperty("signingKey") as? String)
    val signingPassword = System.getenv("GPG_PASSWORD") ?: (project.findProperty("signingPassword") as? String)

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    } else {
        println("WARNING: Label — GPG Signing is SKIPPED because keys are missing!")
    }
}
