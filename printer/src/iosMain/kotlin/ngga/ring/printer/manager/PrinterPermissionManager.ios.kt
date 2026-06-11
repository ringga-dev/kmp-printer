package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConnectionType
import platform.CoreBluetooth.*

actual class PrinterPermissionManager {
    actual constructor()

    actual fun hasPermissions(connectionType: String): Boolean {
        val normalizedType = PrinterConnectionType.normalize(connectionType)
        return if (normalizedType == PrinterConnectionType.BLUETOOTH || normalizedType == PrinterConnectionType.BLUETOOTH_LE) {
            CBCentralManager.authorization == CBManagerAuthorizationAllowedAlways
        } else {
            true
        }
    }

    actual fun requestPermissions(connectionType: String, onResult: (Boolean) -> Unit) {
        // iOS requests permission automatically when CBCentralManager is instantiated
        // or when a scan is started.
        onResult(true)
    }
}
