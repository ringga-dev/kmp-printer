---
title: Getting Started
description: Install KmpPrinter, choose a transport, and print the first ESC/POS receipt.
next: /docs/transports
---

## Install

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.ringga-dev:kmp_printer:2.2.1")
        }
    }
}
```

## First Print

```kotlin
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.model.NetworkPrinterConfig

val printer = KmpPrinter()
val config = NetworkPrinterConfig(
    name = "Receipt Printer",
    host = "192.168.1.50",
    port = 9100
)

printer.print(config) {
    initialize()
    alignCenter()
    bold(true)
    line("KMP PRINTER")
    bold(false)
    divider()
    line("Network print ready")
    feed(3)
    cut()
}.collect { status ->
    println(status)
}
```

## Logging

```kotlin
printer.setLogger { event ->
    println("${event.level}/${event.tag}: ${event.message}")
}
```

## Status Query

```kotlin
val status = printer.queryStatus()
if (!status.isStatusSupported) {
    println(status.message)
}
```

Status depends on printer firmware and transport readback support.
