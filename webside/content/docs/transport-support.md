# Transport Support

KmpPrinter keeps the print API consistent across platforms, but hardware support depends on OS APIs, permissions, drivers, firmware, and whether the printer exposes status readback.

| Transport | Android | iOS | JVM/Desktop | Web/JS | Notes |
| --- | --- | --- | --- | --- | --- |
| Network TCP | Supported | Supported | Supported | Best-effort | Usually the most reliable raw ESC/POS transport. |
| USB | Supported | Not supported | Supported / best-effort | Best-effort | Requires Android USB host permission, JVM driver setup, or browser user gesture. |
| Bluetooth Classic | Supported | Not supported | Best-effort | Browser dependent | JVM normally uses OS serial ports, rfcomm, or print queues. |
| BLE | Supported / best-effort | Supported / best-effort | Best-effort | Browser dependent | UUIDs, MTU, and write mode differ across printers. |
| Serial | JVM/Desktop | Not supported | Supported | Browser dependent | Useful for USB-serial adapters and rfcomm bindings. |
| Virtual | Supported | Supported | Supported | Supported | Intended for tests, previews, and demos. |

Status querying is optional. Handle `PrinterStatus.isStatusSupported == false` for write-only transports such as OS print queues, some BLE characteristics, and browser-backed devices.
