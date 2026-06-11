# Transport Support

KmpPrinter exposes one API across platforms, but transport support depends on OS APIs, permissions, printer firmware, drivers, browser capabilities, and whether the device supports readback status bytes.

| Transport | Android | iOS | JVM/Desktop | Web/JS | Notes |
| --- | --- | --- | --- | --- | --- |
| Network TCP `9100` | Supported | Supported | Supported | Best-effort | Most reliable ESC/POS transport when the printer exposes raw TCP. Status readback depends on firmware. |
| USB raw printer | Supported | Not supported | Supported / best-effort | Best-effort | Android requires USB host permission. JVM may need libusb, WinUSB, udev rules, or fallback to serial/print queue. Browser USB requires secure context and user gesture. |
| Bluetooth Classic SPP | Supported | Not supported | Best-effort | Browser dependent | Android can connect to paired SPP devices. JVM uses OS serial ports, rfcomm, or print queues, not always direct MAC access. |
| BLE | Supported / best-effort | Supported / best-effort | Best-effort with helper/backends | Browser dependent | BLE printers vary heavily by service UUID, characteristic properties, MTU, and write-with-response support. |
| Serial | JVM/Desktop | Not supported | Supported | Browser dependent | Useful for USB-serial adapters, Linux rfcomm bindings, and OS-created Bluetooth serial ports. |
| OS print queue | Not primary | Not primary | Best-effort | Not supported | Print queues can send bytes, but often cannot return ESC/POS status. |
| Virtual | Supported | Supported | Supported | Supported | Intended for tests, previews, and demos. |

## Status Readback

`queryStatus()` and `monitorStatus()` use ESC/POS status requests where possible. They should be treated as optional capabilities:

- Network, serial, and raw USB are the best candidates for status readback.
- BLE status requires readable or notifiable characteristics.
- OS print queues and many browser transports are usually write-only.
- Some printers accept print bytes but ignore `DLE EOT` status commands.

Always handle `PrinterStatus.isStatusSupported == false` and continue with a print-only flow when status is unavailable.

## Recommended Defaults

- Prefer Network TCP for fixed-location printers.
- Prefer USB on Android when the printer is physically attached to the device.
- Prefer typed configs or `PrinterConnection` instead of raw string connection types in new code.
- Reduce `sendChunkSize` and increase `sendChunkDelayMs` for cheap BLE or Bluetooth Classic printers that drop bytes.
- Use `diagnoseUsb`, `diagnoseBle`, and `troubleshootingHint` before asking users to change OS drivers or permissions.
