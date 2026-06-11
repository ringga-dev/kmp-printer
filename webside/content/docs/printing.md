---
title: Printing
description: Build receipts, send raw ESC/POS bytes, handle errors, and monitor status.
prev: /docs/diagnostics
next: /docs/api-migration
---

## Receipt DSL

```kotlin
printer.print(config) {
    initialize()
    alignCenter()
    bold(true)
    line("STORE RECEIPT")
    bold(false)
    divider()
    alignLeft()
    tableRow(listOf("Coffee", "1", "25.000"), listOf(2, 1, 1))
    feed(3)
    cut()
}
```

## Raw Bytes

```kotlin
val bytes = printer.newCommandBuilder(config)
    .initialize()
    .line("Raw ESC/POS")
    .cut()
    .build()

printer.printRaw(config, bytes).collect { status ->
    when (status) {
        is PrintStatus.Error -> println("${status.code}: ${status.message}")
        else -> println(status)
    }
}
```

## Status

```kotlin
printer.monitorStatus(config).collect { status ->
    if (!status.isStatusSupported) {
        println(status.message)
    }
}
```

Transport queues and write-only BLE characteristics usually cannot return printer status.
