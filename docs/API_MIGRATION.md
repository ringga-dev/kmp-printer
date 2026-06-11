# API Migration and Transport Quickstart

The old `PrinterConfig` API remains supported. New code can use typed transport configs and convert them through `toPrinterConfig()`.

`PrinterConfig.connectionType` remains a `String` for compatibility. New code that still uses `PrinterConfig` directly can avoid hardcoded strings with `PrinterConnection`:

```kotlin
val config = PrinterConfig(
    name = "Kitchen Printer",
    connection = PrinterConnection.NETWORK,
    address = "192.168.1.50"
)
```

## Network

```kotlin
val config = NetworkPrinterConfig(
    name = "Kitchen Printer",
    host = "192.168.1.50",
    port = 9100
)

printer.print(config) {
    initialize()
    line("Network test")
    cut()
}
```

## USB

```kotlin
val config = UsbPrinterConfig(
    name = "USB Printer",
    address = "USB_RAW:04B8:0202"
)
```

Use `printer.diagnoseUsb(config.toPrinterConfig())` to get driver, permission, and udev guidance.

## Bluetooth Classic

Windows and macOS use the OS-created serial port or printer queue. Linux can use an `rfcomm` binding.

```kotlin
val windows = BluetoothClassicPrinterConfig(
    name = "Bluetooth Printer",
    osPortQueueOrMac = "COM7"
)

val linux = BluetoothClassicPrinterConfig(
    name = "Bluetooth Printer",
    osPortQueueOrMac = "AA:BB:CC:DD:EE:FF",
    linuxAutoBind = true,
    linuxRfcommDevice = "/dev/rfcomm0"
)
```

## BLE

```kotlin
val config = BlePrinterConfig(
    name = "BLE Printer",
    address = "AA:BB:CC:DD:EE:FF",
    autoDiscover = true
)
```

Android BLE supports MTU negotiation, service discovery retry, write result handling, and read/notify status where the printer exposes readable or notifiable characteristics.

## Status Query

```kotlin
val status = printer.queryStatus()
if (!status.isStatusSupported) {
    println(status.message)
}
```

Status support depends on the transport and printer firmware. Write-only BLE characteristics and OS print queues often cannot return status bytes.

## Error Handling

`PrintStatus.Error` now includes a structured `PrinterErrorCode` while keeping the old `message` field.

```kotlin
when (val status = result) {
    is PrintStatus.Error -> println("${status.code}: ${status.message}")
    else -> Unit
}
```

## Logging

```kotlin
printer.setLogger { event ->
    println("${event.level}/${event.tag}: ${event.message}")
}
```

## Hardware Integration Test

```bash
PRINTER_IT_TYPE=NETWORK
PRINTER_IT_ADDRESS=192.168.1.50
PRINTER_IT_PORT=9100
./gradlew :printer:jvmTest
```

The test is skipped unless `PRINTER_IT_TYPE` and `PRINTER_IT_ADDRESS` are set.

## Troubleshooting

| Symptom | Likely Cause | Action |
| --- | --- | --- |
| USB access denied | OS permission/driver missing | Run USB diagnostics and apply udev/WinUSB guidance |
| Bluetooth Classic MAC fails on JVM | JVM needs OS serial/queue, not raw MAC | Use discovery or Linux rfcomm auto-bind |
| `queryStatus()` unsupported | Transport has no readback | Check `isStatusSupported` and continue with print-only flow |
| BLE write fails | Characteristic or MTU issue | Use auto discovery, reduce chunk size, verify write characteristic |
| Printer queue prints but no status | OS queue is write-only | Use serial/network/raw USB for status where possible |
