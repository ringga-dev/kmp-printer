import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.dokka)
}

group = project.findProperty("LIB_GROUP")?.toString() ?: "io.github.ringga-dev"
version = project.findProperty("LIB_VERSION")?.toString() ?: "1.0.0"

base {
    archivesName = "kmp_printer-ui"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

    val xcf = XCFramework("KmpPrinterUi")
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KmpPrinterUi"
            isStatic = true
            freeCompilerArgs += listOf("-Xbinary=bundleId=io.github.ringga_dev.kmp_printer_ui")
            xcf.add(this)
        }
        iosTarget.mavenPublication {
            artifactId = "kmp_printer_ui-" + iosTarget.name.lowercase()
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    js(IR) {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":printer"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.materialIconsExtended)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

android {
    namespace = "ngga.ring.printer.ui"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

publishing {
    publications {
        all {
            when (this) {
                is MavenPublication -> {
                    artifactId = when (name) {
                        "kotlinMultiplatform" -> "kmp_printer-ui"
                        "androidRelease" -> "kmp_printer_ui-android"
                        else -> "kmp_printer_ui-${name.replaceFirstChar { it.lowercase() }}"
                    }

                    val javadocJar = tasks.register<Jar>("${name}JavadocJar") {
                        archiveClassifier.set("javadoc")
                        destinationDirectory.set(layout.buildDirectory.dir("libs/${name}"))
                        from(project.rootProject.layout.projectDirectory.file("README.md")) {
                            rename { "README.md" }
                        }
                    }
                    artifact(javadocJar)
                    pom {
                        name.set("KmpPrinterUi")
                        description.set("KmpPrinterUi: Compose Multiplatform UI components for ESC/POS and TSPL thermal printing.")
                        url.set("https://github.com/ringga-dev/kmp-printer")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }
                        developers {
                            developer {
                                id.set("ringga")
                                name.set("Ringga")
                                email.set("ringgadev@gmail.com")
                            }
                        }
                        scm {
                            connection.set("scm:git:git://github.com/ringga-dev/kmp-printer.git")
                            developerConnection.set("scm:git:ssh://github.com/ringga-dev/kmp-printer.git")
                            url.set("https://github.com/ringga-dev/kmp-printer")
                        }
                    }
                }
            }
        }
    }

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
            "kotlinMultiplatform" -> "kmp_printer-ui"
            "androidRelease" -> "kmp_printer_ui-android"
            else -> "kmp_printer_ui-${pub.name.replaceFirstChar { it.lowercase() }}"
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
        println("WARNING: GPG Signing is SKIPPED because keys are missing!")
    }
}
