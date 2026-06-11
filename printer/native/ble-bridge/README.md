# JVM BLE Native Bridges

The Kotlin/JVM library uses small external helpers for native BLE on OSes where JVM has no stable built-in BLE API.

## Protocol

The JVM starts a helper with:

```text
--connect <address> --service <uuid> --characteristic <uuid>
```

Then it writes one hex-encoded ESC/POS chunk per stdin line. The helper writes the bytes to the selected BLE characteristic. `QUIT` closes the session.

## Windows

Bootstrap and build:

```powershell
powershell -ExecutionPolicy Bypass -File printer\native\ble-bridge\windows\bootstrap.ps1
```

The bootstrap script installs .NET SDK locally under `printer\native\ble-bridge\windows\.dotnet` if `dotnet` is not already available.

Manual build:

```powershell
dotnet publish printer\native\ble-bridge\windows\KmpPrinterBleWindows.csproj -c Release -r win-x64 --self-contained false
```

Output binary name: `kmp-printer-ble-windows.exe`.

Set it on `PATH`, or configure:

```kotlin
PrinterConfig(
    name = "BLE",
    connectionType = PrinterConnectionType.BLUETOOTH_LE,
    address = "AA:BB:CC:DD:EE:FF",
    bleBridgeCommand = "C:\\path\\kmp-printer-ble-windows.exe"
)
```

## macOS

Bootstrap and build on macOS:

```bash
bash printer/native/ble-bridge/macos/bootstrap.sh
```

The bootstrap script checks for Swift/Xcode Command Line Tools and starts `xcode-select --install` if needed.

Manual build:

```bash
cd printer/native/ble-bridge/macos
swift build -c release
```

Output binary name: `kmp-printer-ble-macos`.

Set it on `PATH`, or configure `bleBridgeCommand` to the built executable.
