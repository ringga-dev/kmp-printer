---
title: API Migration
description: Move from one large PrinterConfig to typed transport configs without breaking old code.
prev: /docs/printing
next: /docs/os-setup
---

## Legacy API

`PrinterConfig` remains supported and source-compatible.

```kotlin
val config = PrinterConfig(
    name = "Receipt Printer",
    connectionType = "NETWORK",
    address = "192.168.1.50"
)
```

New code can avoid hardcoded strings and paper values:

```kotlin
val config = PrinterConfig(
    name = "Receipt Printer",
    connection = PrinterConnection.NETWORK,
    profile = PrinterProfile.MM58,
    address = "192.168.1.50"
)
```

`PrinterConnection.BLE` is available as a short alias for `PrinterConnection.BLUETOOTH_LE`.

## Typed Configs

```kotlin
val network = NetworkPrinterConfig("Kitchen", "192.168.1.50")
val usb = UsbPrinterConfig("USB", "USB_RAW:04B8:0202")
val classic = BluetoothClassicPrinterConfig("BT", "COM7")
val ble = BlePrinterConfig("BLE", "AA:BB:CC:DD:EE:FF")
```

Typed configs can be passed directly to `KmpPrinter.print` and `KmpPrinter.printRaw`.

```kotlin
printer.print(network) {
    line("Typed config")
    cut()
}
```

## Factory Helpers

`PrinterConfigs` provides non-breaking helpers for apps that still prefer `PrinterConfig`.

```kotlin
val config = PrinterConfigs.bluetoothClassicLinuxRfcomm(
    name = "BT Printer",
    macAddress = "AA:BB:CC:DD:EE:FF"
)
```
