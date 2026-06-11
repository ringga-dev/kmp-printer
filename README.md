# KmpPrinter

KmpPrinter is a Kotlin Multiplatform ESC/POS thermal printing library for Android, iOS, JVM/Desktop, JavaScript, and WASM targets.

Current source version: `2.2.0`.

## Connection Support

| Platform | Bluetooth Classic | BLE | USB | Network TCP |
| --- | ---: | ---: | ---: | ---: |
| Android | Yes | Yes | Yes | Yes |
| iOS | No | Yes | No | Yes |
| JVM/Desktop | Serial/SPP only | No native BLE | Yes | Yes |
| Web/WASM | Browser dependent | Browser dependent | Browser dependent | Browser dependent |

Supported `connectionType` values:

- `BLUETOOTH`
- `BLUETOOTH_LE`
- `USB`
- `NETWORK`
- `SERIAL`
- `VIRTUAL`

## Installation

```kotlin
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

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.ringga-dev:kmp_printer:2.2.0")
        }
    }
}
```

The dependency version is synchronized from `LIB_VERSION` in `gradle.properties` by running:

```bash
./gradlew syncDocumentationVersion
```

## Quick Start

```kotlin
val printer = KmpPrinter()

val config = PrinterConfig(
    name = "Receipt Printer",
    connectionType = "NETWORK",
    address = "192.168.1.50",
    port = 9100,
    characterPerLine = 32,
    paperWidthDots = 384
)

printer.print(config) {
    initialize()
    alignCenter()
    bold(true)
    line("STORE RECEIPT")
    bold(false)
    divider()
    tableRow(listOf("Coffee", "1", "$3.00"), listOf(2, 1, 1))
    qrCodeNative("https://example.com/order/123", center = true)
    feed(3)
    cut()
}.collect { status ->
    println(status)
}
```

## JVM Bluetooth Classic

JVM/Desktop does not have a reliable cross-platform native Bluetooth API. KmpPrinter supports Bluetooth Classic on JVM through the operating system's Serial Port Profile.

Recommended setup:

1. Pair the printer in the operating system Bluetooth settings.
2. Make sure the OS exposes the printer as a serial port.
3. Use that serial port as `PrinterConfig.address`.

Windows example:

```kotlin
val config = PrinterConfig(
    name = "Bluetooth Printer",
    connectionType = "BLUETOOTH",
    address = "COM5",
    baudRate = 9600
)
```

Linux example:

```kotlin
val config = PrinterConfig(
    name = "Bluetooth Printer",
    connectionType = "BLUETOOTH",
    address = "/dev/rfcomm0",
    baudRate = 9600
)
```

If the printer uses a different baud rate, set `baudRate` to the value required by the device, such as `19200`, `38400`, or `115200`.

Native JVM BLE is not implemented. BLE requires separate OS-specific implementations for Windows, Linux, and macOS.

## PrinterConfig

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
    val readTimeoutMs: Int = 2000,
    val baudRate: Int = 9600
)
```

## Platform Notes

Android needs runtime Bluetooth permissions on Android 12 and newer:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

iOS BLE requires:

```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>This app uses Bluetooth to connect to receipt printers.</string>
```

## License

MIT License. See [LICENSE](./LICENSE).
