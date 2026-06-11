---
title: OS Setup
description: Windows, Linux, macOS, Android, WebUSB, BLE helper, and raw USB setup notes.
prev: /docs/api-migration
next: /docs/integration-tests
---

## Windows

- Raw USB may require a WinUSB/libusb driver such as Zadig.
- Bluetooth Classic needs the outgoing `COMx` port created by pairing.
- BLE JVM uses the WinRT helper bridge.

## Linux

- Raw USB may require udev rules.
- Bluetooth Classic can auto-bind MAC addresses to `/dev/rfcomm0` when permissions allow.
- BLE JVM uses BlueZ `bluetoothctl` GATT write support.

```bash
sudo rfcomm bind /dev/rfcomm0 AA:BB:CC:DD:EE:FF
```

## macOS

- Raw USB can fail if macOS owns the printer interface.
- Bluetooth Classic needs an outgoing `/dev/cu.*` device or printer queue.
- BLE JVM uses the CoreBluetooth helper bridge.

## Android

Android apps need Bluetooth, location, network, and USB host permission flow depending on transport.
