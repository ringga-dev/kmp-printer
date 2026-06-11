---
title: Integration Tests
description: Validate real printers with the JVM hardware integration test runner.
prev: /docs/os-setup
---

## JVM Hardware Runner

The integration test is opt-in and skipped unless environment variables are set.

```bash
PRINTER_IT_TYPE=NETWORK
PRINTER_IT_ADDRESS=192.168.1.50
PRINTER_IT_PORT=9100
./gradlew :printer:jvmTest
```

## Bluetooth Classic Linux

```bash
PRINTER_IT_TYPE=BLUETOOTH
PRINTER_IT_ADDRESS=AA:BB:CC:DD:EE:FF
PRINTER_IT_RFCOMM=/dev/rfcomm0
./gradlew :printer:jvmTest
```

## Variables

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

The runner checks connection, raw print, and status query behavior where supported.
