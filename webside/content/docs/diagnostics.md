---
title: Diagnostics
description: Run platform reports, USB checks, BLE checks, serial checks, and status probes.
prev: /docs/transports
next: /docs/printing
---

## Platform Report

```kotlin
val report = printer.platformReport()
println(report.platformName)
println(report.capabilities)
```

## USB

```kotlin
val usb = printer.diagnoseUsb(config)
println(usb.failureReason)
println(usb.suggestedFix)
println(usb.udevRule)
```

Raw USB diagnostics explain WinUSB/libusb driver problems, Linux udev permission, macOS interface ownership, and endpoint availability.

## Serial and Bluetooth Classic

```kotlin
val serial = printer.diagnoseSerial(config)
serial.ports.forEach {
    println("${it.address}: ${it.confidence}%")
}
```

On JVM, Bluetooth Classic diagnostics inspect adapter state, pairing signals, serial candidates, and OS-specific hints.

## BLE

```kotlin
val ble = printer.diagnoseBle(config)
println(ble.message)
```

BLE diagnostics vary by platform. JVM Windows/macOS require the helper bridge on `PATH` or `PrinterConfig.bleBridgeCommand`.
