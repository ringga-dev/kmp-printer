import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
    id("signing")
    // Dokka for professional Javadoc required by Maven Central
    id("org.jetbrains.dokka") version "1.9.20"
}

group = project.property("LIB_GROUP").toString()
version = project.property("LIB_VERSION").toString()

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
        binaries.executable()
    }

    js(IR) {
        browser()
        binaries.executable()
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

// Requirements for Maven Central: Sources Jar
val kmpSourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets.getByName("commonMain").kotlin)
}

// Requirements for Maven Central: Javadoc Jar (using Dokka)
tasks.named("dokkaHtml") {
    // Temporarily disabled due to internal bug: "not array: KClass<out Annotation>"
    // This is a known incompatibility in current Kotlin/Dokka versions.
    enabled = false 
}

val kmpJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    // If dokkaHtml is disabled or fails, this JAR will just be empty
    if (tasks.findByName("dokkaHtml")?.enabled == true) {
        from(tasks.named("dokkaHtml"))
    }
}

publishing {
    publications {
        // The Multiplatform plugin automatically creates publications for each target.
        // We just need to configure them to include standard pom metadata.
        withType<MavenPublication> {
            // artifactId is handled by KMP automatically.
            // groupId and version are already set at the top level
            
            // Note: KMP automatically handles sources JAR for most targets.
            // If you need Javadoc, it should be configured specifically.

            pom {
                name.set("KmpPrinter")
                description.set("KmpPrinter: Professional Kotlin Multiplatform Thermal Printing Library for ESC/POS.")
                url.set("https://github.com/ringga-dev/Printer-ESC-POS")
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
                        email.set("ringga@dev.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/ringga-dev/Printer-ESC-POS.git")
                    developerConnection.set("scm:git:ssh://github.com/ringga-dev/Printer-ESC-POS.git")
                    url.set("https://github.com/ringga-dev/Printer-ESC-POS")
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
    val signingKey = System.getenv("ORG_GRADLE_PROJECT_signingKey")
    val signingPassword = System.getenv("ORG_GRADLE_PROJECT_signingPassword")
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}
