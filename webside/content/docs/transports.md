---
title: Transports
description: Network, USB, Serial, Bluetooth Classic, BLE, and virtual transport support by platform.
prev: /docs/getting-started
next: /docs/diagnostics
---

## JVM Desktop

| Transport | JVM Support | Notes |
| --- | --- | --- |
| Network | Native TCP | Best stability for ESC/POS printers. |
| Raw USB | libusb | Falls back to serial devices and OS printer queues. |
| Serial | jSerialComm | Uses `COMx`, `/dev/tty*`, `/dev/rfcomm*`, or `/dev/cu.*`. |
| Bluetooth Classic | OS serial/queue | Windows COM, Linux rfcomm, macOS cu/tty or queue. |
| BLE | BlueZ/helper bridge | Linux uses `bluetoothctl`; Windows/macOS use helper binaries. |
| Virtual | Native | For preview and tests. |

## Android

Android supports Network, USB host, Bluetooth Classic, BLE, and Virtual. BLE now uses MTU negotiation, service discovery retry, write-result callbacks, and read/notify status where the printer exposes readable or notifiable characteristics.

## Web and Wasm

Web support depends on browser APIs, HTTPS, and user gesture requirements. Wasm is best treated as bridge-backed or virtual unless the host app supplies browser API integrations.
