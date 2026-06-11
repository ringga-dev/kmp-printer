# KmpPrinter

KmpPrinter is a Kotlin Multiplatform ESC/POS thermal printing library for Android, iOS, JVM/Desktop, and Web targets. It provides a single API for printer discovery, connection management, receipt building, raw ESC/POS output, image printing, barcodes, QR codes, and printer status checks.

The source version configured in this repository is `2.2.0`.

## Features

- Kotlin Multiplatform support for Android, iOS, JVM/Desktop, JS, and WASM.
- ESC/POS receipt builder with text styling, alignment, tables, dividers, images, barcodes, QR codes, cash drawer commands, and paper cutting.
- Bluetooth, BLE, USB, TCP/network, and virtual printer connectors depending on platform support.
- Flow-based printer discovery and print status updates.
- Real-time status querying for compatible printers.
- Built-in concurrency protection and chunked sending to reduce data corruption and printer buffer overflow.
- Preview block generation for UI receipt previews.

## Platform Support

| Platform | Bluetooth Classic | BLE | USB | Network TCP | Status Query |
| --- | ---: | ---: | ---: | ---: | ---: |
| Android | Yes | Yes | Yes | Yes | Yes |
| iOS | No | Yes | No | Yes | Yes |
| JVM/Desktop | Yes, OS serial/queue | Yes, helper/BlueZ | Yes, raw USB/serial/queue | Yes | Transport dependent |
| Web | Yes | Yes | Yes | Yes | Browser dependent |

Support still depends on the printer firmware, browser APIs, OS permissions, drivers, and hardware transport availability. See [Printer OS Setup Guide](./docs/PRINTER_OS_SETUP.md) and [API Migration Guide](./docs/API_MIGRATION.md) for setup, troubleshooting, typed config APIs, and hardware integration tests.

## Installation

Add the Maven repository:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://raw.githubusercontent.com/ringga-dev/kmp-printer/maven-repo")
        }
    }
}
```

Add the dependency:

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.ringga-dev:kmp_printer:2.2.0")
        }
    }
}
```

This dependency version is synchronized from `LIB_VERSION` in `gradle.properties` by running `./gradlew syncDocumentationVersion`.

## Quick Start

```kotlin
import kotlinx.coroutines.flow.collect
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.model.PrinterConfig

val printer = KmpPrinter()

val config = PrinterConfig(
    name = "Receipt Printer",
    connectionType = "NETWORK",
    address = "192.168.1.50",
    port = 9100,
    characterPerLine = 32,
    paperWidth = 58,
    paperWidthDots = 384
)

printer.print(config) {
    alignCenter()
    bold(true)
    line("STORE RECEIPT")
    bold(false)
    divider()
    tableRow(listOf("Coffee", "1", "$3.00"), listOf(2, 1, 1))
    tableRow(listOf("Tax", "", "$0.30"), listOf(2, 1, 1))
    divider()
    qrCodeNative("https://example.com/order/123", center = true)
    feed(3)
    cut()
}.collect { status ->
    println(status)
}
```

## Printer Configuration

```kotlin
data class PrinterConfig(
    val name: String,
    val connectionType: String,
    val address: String? = null,
    val port: Int = 9100,
    val characterPerLine: Int = 31,
    val paperWidth: Int = 58,
    val paperWidthDots: Int = 0,
    val leftMargin: Int = 0,
    val autoCenter: Boolean = false,
    val charsetName: String = "UTF-8",
    val escPosCodePage: Byte = 0x00,
    val connectionTimeoutMs: Int = 5000,
    val readTimeoutMs: Int = 2000
)
```

Common `connectionType` values are `BLUETOOTH`, `BLUETOOTH_LE`, `USB`, `NETWORK`, `SERIAL`, and `VIRTUAL`. `PrinterConfigs` provides transport-specific factory helpers when direct `PrinterConfig` construction becomes too verbose.

## Discovery

```kotlin
printer.discovery("NETWORK") { log ->
    println(log)
}.collect { devices ->
    devices.forEach { device ->
        println("${device.name} ${device.address}:${device.port}")
    }
}
```

## Status Monitoring

```kotlin
printer.monitorStatus(config, intervalMs = 2000).collect { status ->
    if (status.isPaperOut) {
        println("Printer is out of paper")
    }
}
```

Status monitoring uses ESC/POS `DLE EOT` queries and only works when the connector and printer firmware support reading responses.

## Platform Setup

Android applications usually need:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

USB printing uses Android's USB host permission flow for the selected device. It is not declared as a normal manifest permission.

iOS applications that use BLE need:

```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>This app uses Bluetooth to connect to receipt printers.</string>
```

Web printing requires a secure context and a user gesture before the browser can show Bluetooth, USB, or serial device pickers.

## Development

Run the version sync task after changing `LIB_VERSION` in `gradle.properties`:

```bash
./gradlew syncDocumentationVersion
```

Use the Gradle wrapper for builds:

```bash
./gradlew build
```

## Changelog

Current source version: `2.2.0`.

Recent highlights:

- Multiplatform ESC/POS support for Android, iOS, JVM/Desktop, JS, and WASM.
- Hardened connector sending with mutex protection and chunked transfer.
- Receipt DSL for text, tables, images, barcodes, QR codes, cash drawer, and cutter commands.
- Printer discovery, preview blocks, virtual printer support, and status monitoring for compatible hardware.

## Contributing

- Keep public API changes intentional and documented here.
- Prefer shared `commonMain` code when behavior is platform independent.
- Keep platform-specific transport code inside the matching source set.
- Test affected targets before publishing.

## License

MIT License. See [LICENSE](./LICENSE).
