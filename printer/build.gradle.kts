import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
    id("signing")
    alias(libs.plugins.dokka)
}

group = project.findProperty("LIB_GROUP")?.toString() ?: "io.github.ringga-dev"
version = project.findProperty("LIB_VERSION")?.toString() ?: "1.0.0"

base {
    archivesName = "kmp_printer"
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    val xcf = XCFramework("KmpPrinter")
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KmpPrinter"
            isStatic = true
            freeCompilerArgs += listOf("-Xbinary=bundleId=io.github.ringga_dev.kmp_printer")
            xcf.add(this)
        }
    }

    jvm()

    wasmJs {
        browser()
    }

    js(IR) {
        browser()
    }
    
    targets.all {
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation("io.github.aakira:napier:2.7.1")
        }

        androidMain.dependencies {
            implementation("androidx.appcompat:appcompat:1.7.0")
            implementation("com.google.android.material:material:1.12.0")
        }

        jvmMain.dependencies {
            implementation("com.fazecast:jSerialComm:2.11.0")
            implementation("org.usb4java:usb4java:1.3.0")
            implementation("org.usb4java:libusb4java:1.3.0")
            implementation("net.java.dev.jna:jna:5.14.0")
            implementation("net.java.dev.jna:jna-platform:5.14.0")
            implementation("org.apache.pdfbox:pdfbox:3.0.1")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}

android {
    namespace = "ngga.ring.printer"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable.add("MissingPermission")
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            val javadocJar = tasks.register<Jar>("${name}JavadocJar") {
                archiveClassifier.set("javadoc")
                // Unique output dir per publication to prevent signing conflicts
                destinationDirectory.set(layout.buildDirectory.dir("libs/${name}"))
                // Include a simple placeholder
                from(project.rootProject.layout.projectDirectory.file("README.md")) {
                    rename { "README.md" }
                }
            }
            artifact(javadocJar)
            pom {
                name.set("KmpPrinter")
                description.set("KmpPrinter: Professional Kotlin Multiplatform Thermal Printing Library for ESC/POS.")
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

    repositories {
        maven {
            name = "LocalRepo"
            url = uri(layout.buildDirectory.dir("repo"))
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
