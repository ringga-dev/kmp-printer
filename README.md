# 🖨️ KmpPrinter

**Kotlin Multiplatform ESC/POS Thermal Printing Library**

[![GitHub Release](https://img.shields.io/github/v/release/ringga-dev/kmp-printer?style=flat&logo=github&color=blue)](https://github.com/ringga-dev/kmp-printer/releases)
[![License](https://img.shields.io/github/license/ringga-dev/kmp-printer?style=flat&color=green)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/ringga-dev/kmp-printer?style=flat&logo=github)](https://github.com/ringga-dev/kmp-printer/stargazers)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.x-blueviolet?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Platforms](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20Web%20%7C%20Wasm-blue?style=flat)](#platform-support)
[![CI](https://img.shields.io/github/actions/workflow/status/ringga-dev/kmp-printer/ci.yml?style=flat&logo=githubactions&label=CI)](https://github.com/ringga-dev/kmp-printer/actions/workflows/ci.yml)

A single unified API for thermal printer discovery, connection, ESC/POS receipt building, and status monitoring — across **Android, iOS, JVM/Desktop, JS, and Wasm**.

---

## ✨ Features

- ✅ **Multiplatform** — Android, iOS, JVM/Desktop, JS, Wasm
- ✅ **Multiple Transports** — Bluetooth Classic, BLE, USB, Network TCP, Serial, Virtual
- ✅ **ESC/POS Receipt DSL** — text styling, alignment, tables, dividers, images, barcodes, QR codes
- ✅ **Flow-based Discovery** — reactive printer scanning via Kotlin coroutines
- ✅ **Status Monitoring** — real-time paper out, cover open, error detection
- ✅ **Chunked Sending** — prevents printer buffer overflow
- ✅ **Concurrency Protection** — built-in mutex for safe multi-thread printing
- ✅ **Preview Rendering** — render receipts virtually for UI preview
- ✅ **MIT Licensed** — free for commercial and personal use

---

## 📱 Platform Support

| Platform | Bluetooth Classic | BLE | USB | Network TCP | Status Query |
|---|---|---|---|---|---|
| **Android** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **iOS** | ❌ | ✅ | ❌ | ✅ | ✅ |
| **JVM/Desktop** | ✅ OS serial/queue | ✅ BlueZ helper | ✅ raw USB/serial | ✅ | ⚠️ Transport dependent |
| **Web (JS)** | ✅ | ✅ | ✅ | ✅ | ⚠️ Browser dependent |
| **Wasm** | ✅ | ✅ | ✅ | ✅ | ⚠️ Browser dependent |

> Support depends on printer firmware, OS APIs, browser capabilities, and hardware transport. See [Transport Support](docs/TRANSPORT_SUPPORT.md) for details.

---

## 🚀 Quick Start

### 1. Add Repository

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

### 2. Add Dependency

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.ringga-dev:kmp_printer:2.3.3")
        }
    }
}
```

> **Maven users:**
> ```xml
> <!-- pom.xml -->
> <dependency>
>     <groupId>io.github.ringga-dev</groupId>
>     <artifactId>kmp_printer</artifactId>
>     <version>2.3.3</version> <!-- sync-version -->
> </dependency>
> ```
> *Available on Maven Central — no extra repository needed.*

### 3. Print Your First Receipt

```kotlin
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.model.PrinterConfig

val printer = KmpPrinter()
val config = PrinterConfig(
    name = "Receipt Printer",
    connectionType = "NETWORK",
    address = "192.168.1.50",
    port = 9100,
    characterPerLine = 32,
    paperWidth = 58
)

printer.print(config) {
    alignCenter()
    bold(true)
    line("☕ COFFEE SHOP")
    bold(false)
    divider()
    tableRow(listOf("Americano", "2", "6.00"), listOf(2, 1, 1))
    tableRow(listOf("Latte", "1", "4.50"), listOf(2, 1, 1))
    divider()
    line("Total: $10.50", align = "RIGHT")
    feed(1)
    qrCodeNative("https://example.com", center = true)
    feed(3)
    cut()
}.collect { status -> println(status) }
```

---

## 🧩 Usage Examples

### 📋 Receipt Builder DSL

```kotlin
printer.print(config) {
    setCharset(PrinterCharset.USA_EUROPE)
    alignCenter()
    image(myBitmap, center = true)
    feed(2)
    bold(true)
    fontSize(2)
    line("HEADER")
    fontSize(1)
    bold(false)
    divider('=')
    table(listOf("Item", "Qty", "Price"), listOf(2, 1, 1))
    tableRow(listOf("Product 1", "3", "9.99"), listOf(2, 1, 1))
    barcode("123456789012", type = BarcodeType.EAN13)
    feed(2)
    cut()
}
```

### 🔍 Printer Discovery (Network)

```kotlin
printer.discovery("NETWORK") { log ->
    println("Scanning: $log")
}.collect { devices ->
    devices.forEach { device ->
        println("Found: ${device.name} at ${device.address}:${device.port}")
    }
}
```

### 📊 Status Monitoring

```kotlin
printer.monitorStatus(config, intervalMs = 2000).collect { status ->
    when {
        status.isPaperOut -> sendAlert("📢 Printer kehabisan kertas!")
        status.isCoverOpen -> sendAlert("🔓 Cover printer terbuka!")
        !status.isOnline -> sendAlert("⚠️ Printer offline!")
    }
}
```

### 🎛 Typed Transport Configs (New API)

```kotlin
// Recommended for new code — avoids hardcoded strings
val networkConfig = PrinterConfig(
    name = "Kitchen Printer",
    connection = PrinterConnection.NETWORK,
    profile = PrinterProfile.MM58,
    address = "192.168.1.50"
)

val bleConfig = PrinterConfig(
    name = "Portable Printer",
    connection = PrinterConnection.BLE,
    address = "00:11:22:33:44:55",
    profile = PrinterProfile.MM80
)
```

### 🎨 Print Quality

```kotlin
printer.print(config) {
    printQuality(PrintQuality.Dark)
    line("High density thermal output")
    cut()
}
```

---

## 📚 Documentation

| Document | Description |
|---|---|
| [Transport Support](docs/TRANSPORT_SUPPORT.md) | Platform-by-platform transport compatibility |
| [Printer OS Setup](docs/PRINTER_OS_SETUP.md) | OS-level driver & permission setup (Linux udev, Windows, macOS) |
| [API & Migration](docs/API_MIGRATION.md) | Typed configs, profiles, migration from v1 |
| [Releasing](RELEASING.md) | How to publish to GitHub Maven & Maven Central |
| [GitHub Pages](https://ringga-dev.github.io/kmp-printer/) | Online documentation site |

---

## 🛠 Development

```bash
# Build all platforms
./gradlew build

# Run JVM tests
./gradlew :printer:jvmTest

# Compile specific targets
./gradlew :printer:compileKotlinMetadata
./gradlew :printer:compileKotlinJs
./gradlew :printer:compileKotlinWasmJs
./gradlew :printer:compileDebugKotlinAndroid

# Update version in docs after changing LIB_VERSION in gradle.properties
./gradlew syncDocumentationVersion
```

---

## 🏗 Architecture

```
printer/
├── src/
│   ├── commonMain/        # Shared platform-independent logic
│   │   ├── model/         # Printer models, configs, enums
│   │   ├── manager/       # Connection management, discovery
│   │   ├── usecase/       # Print, discover, diagnose
│   │   └── util/          # ESC/POS commands, rendering, preview
│   ├── androidMain/       # Android BLE, USB, Network, Bluetooth
│   ├── iosMain/           # iOS BLE, Network
│   ├── jvmMain/           # Desktop USB, Serial, Network, BlueZ
│   ├── jsMain/            # Web Bluetooth, USB, Serial, Network
│   └── wasmJsMain/        # Wasm hardware bridge
├── androidApp/            # Android sample app
├── desktopApp/            # JVM Desktop sample app
└── iosApp/                # iOS sample app
```

---

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on:

- Code style and PR process
- Platform-specific testing
- How to add new transport support

---

## 📄 License

**MIT License** — see [LICENSE](LICENSE).

Copyright (c) 2026 Ringga. Free to use, modify, and distribute in commercial and non-commercial applications.

---

Built with ❤️ and Kotlin Multiplatform — [⭐ Star on GitHub](https://github.com/ringga-dev/kmp-printer)
