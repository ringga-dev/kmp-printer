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

## Typed Devices & Factory

```kotlin
// Inisialisasi device secara strongly-typed
val kitchenDevice = PrinterDevices.network("Kitchen", host = "192.168.1.200", port = 9100)
val cashierUsb = PrinterDevices.usb("Cashier", "USB_RAW:04B8:0202")
val portableBt = PrinterDevices.bluetooth("RPP02N", "00:11:22:33:44:55")

// Cetak langsung dengan Device & Profile
printer.print(kitchenDevice, profile = PrinterProfile.MM80) {
    line("KITCHEN ORDER #42")
    cut()
}
```

## Multi-Printer Sessions

Gunakan `PrinterSession` untuk aplikasi kasir yang berkomunikasi ke banyak printer secara paralel:

```kotlin
val cashierSession = printer.openSession(cashierUsb)
val kitchenSession = printer.openSession(kitchenDevice)

cashierSession.print { line("Kasir Struk") }
kitchenSession.print { line("Dapur Order") }
```

## TSPL Label Printing (Stiker & Barcode)

Cetak label stiker menggunakan TSPL DSL:

```kotlin
printer.printLabel(cashierUsb, widthMm = 40.0, heightMm = 30.0) {
    direction(0)
    text(x = 10, y = 10, content = "PRODUK A", font = "3")
    barcode(x = 10, y = 45, data = "12345678", type = "128", height = 40)
    qrcode(x = 200, y = 45, data = "https://example.com")
    print(sets = 1, copies = 1)
}
```

## Arabic & RTL Shaping

```kotlin
printer.print(portableBt) {
    lineArabic("شكرا لزيارتكم")
    cut()
}
```

## Print Quality

Thermal output darkness depends on density, heat, power supply, paper quality, and printer firmware. KmpPrinter includes best-effort helpers for common ESC/POS-compatible density and heat commands:

```kotlin
printer.print(config) {
    printQuality(PrintQuality.Dark)
    line("Darkness test")
    cut()
}
```

Some printers ignore these commands or use vendor-specific alternatives. If output stays gray, check the adapter rating, thermal paper, printer head, and hardware density settings.
