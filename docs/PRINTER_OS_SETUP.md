# Printer OS Setup Guide

This guide covers the OS-level setup required by the printer module for real hardware validation.

## JVM Network

- Use the printer IPv4 address and raw TCP port, usually `9100`.
- Make sure the printer and host are on the same network.
- Status query support depends on whether the printer responds to ESC/POS `DLE EOT`.

## JVM Raw USB

Raw USB uses libusb through usb4java. The library tries raw USB first, then serial-like devices, then OS printer queues.

### Windows

- Install a WinUSB/libusb-compatible driver for the printer interface.
- Zadig can be used to switch the printer interface to WinUSB.
- If the printer is also installed as a Windows printer, the OS queue fallback can still be used.
- If raw USB reports busy or claim failure, close spooler jobs or use the printer queue transport.

### Linux

- Add a udev rule when diagnostics returns one.
- Example rule:

```text
SUBSYSTEM=="usb", ATTR{idVendor}=="xxxx", ATTR{idProduct}=="yyyy", MODE="0666", GROUP="plugdev"
```

- Reload rules:

```bash
sudo udevadm control --reload-rules
sudo udevadm trigger
```

- Reconnect the printer after applying rules.
- Add the user to `plugdev`, `dialout`, or the distribution-specific device group when needed.

### macOS

- Raw USB can fail when macOS owns the USB printer interface.
- Prefer the OS printer queue, USB-serial mode, or network printing when claim fails.

## JVM Bluetooth Classic

Bluetooth Classic desktop printing is OS-mediated. The JVM prints through the serial device or printer queue created by the OS.

### Windows

- Pair the printer in Windows Bluetooth settings.
- Use the outgoing `COMx` port, not the incoming COM port.
- If no COM port appears, the printer may not expose Classic SPP or Windows did not create the outgoing port.

### Linux

- Pair the printer with BlueZ.
- The library can auto-bind a MAC address to `PrinterConfig.bluetoothClassicRfcommDevice`, default `/dev/rfcomm0`.
- Manual bind:

```bash
sudo rfcomm bind /dev/rfcomm0 AA:BB:CC:DD:EE:FF
```

- Ensure the app user can read/write `/dev/rfcomm*`.

### macOS

- Pair the printer in Bluetooth settings.
- Use the outgoing `/dev/cu.*` device when macOS exposes one.
- If macOS does not expose a serial device, use an installed printer queue, USB, or network transport.

## JVM BLE

- Linux uses BlueZ `bluetoothctl` GATT write support.
- Windows uses the WinRT helper source in `printer/native/ble-bridge/windows`.
- macOS uses the CoreBluetooth helper source in `printer/native/ble-bridge/macos`.
- Build and place helper binaries on `PATH`, or set `PrinterConfig.bleBridgeCommand`.

## Status Query

`queryStatus()` sends ESC/POS `DLE EOT` commands. It is reliable only when the transport supports readback.

- More likely to work: network TCP, serial, Bluetooth Classic serial, USB devices with bulk IN endpoint.
- Often unsupported: OS printer queues, BLE write-only characteristics, raw USB devices without bulk IN endpoint.
- When unsupported, `PrinterStatus.isStatusSupported` is `false` and `message` explains the reason.

## JVM Hardware Integration Test

The JVM integration test is opt-in through environment variables:

```bash
PRINTER_IT_TYPE=NETWORK
PRINTER_IT_ADDRESS=192.168.1.50
PRINTER_IT_PORT=9100
./gradlew :printer:jvmTest
```

Bluetooth Classic Linux with MAC auto-bind:

```bash
PRINTER_IT_TYPE=BLUETOOTH
PRINTER_IT_ADDRESS=AA:BB:CC:DD:EE:FF
PRINTER_IT_RFCOMM=/dev/rfcomm0
./gradlew :printer:jvmTest
```

Useful variables:

- `PRINTER_IT_NAME`
- `PRINTER_IT_TYPE`
- `PRINTER_IT_ADDRESS`
- `PRINTER_IT_PORT`
- `PRINTER_IT_BAUD`
- `PRINTER_IT_BT_AUTOBIND`
- `PRINTER_IT_RFCOMM`
- `PRINTER_IT_BLE_SERVICE`
- `PRINTER_IT_BLE_CHARACTERISTIC`
- `PRINTER_IT_BLE_BRIDGE`
