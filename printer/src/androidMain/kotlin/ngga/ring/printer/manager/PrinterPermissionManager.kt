package ngga.ring.printer.manager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import ngga.ring.printer.model.PrinterConnectionType

actual class PrinterPermissionManager {
    actual constructor()

    actual fun hasPermissions(connectionType: String): Boolean {
        val context = PrinterInitializer.getContext()
        return when (PrinterConnectionType.normalize(connectionType)) {
            PrinterConnectionType.BLUETOOTH,
            PrinterConnectionType.BLUETOOTH_LE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    check(context, Manifest.permission.BLUETOOTH_SCAN) &&
                    check(context, Manifest.permission.BLUETOOTH_CONNECT) &&
                    check(context, Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    check(context, Manifest.permission.BLUETOOTH) &&
                    check(context, Manifest.permission.BLUETOOTH_ADMIN) &&
                    check(context, Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            PrinterConnectionType.NETWORK -> true
            PrinterConnectionType.USB -> true
            else -> true
        }
    }

    companion object {
        private var permissionCallback: ((Boolean) -> Unit)? = null
    }

    actual fun requestPermissions(connectionType: String, onResult: (Boolean) -> Unit) {
        val activity = PrinterInitializer.getActivity() ?: run {
            onResult(false)
            return
        }

        if (hasPermissions(connectionType)) {
            onResult(true)
            return
        }

        permissionCallback = onResult

        val permissions = when (PrinterConnectionType.normalize(connectionType)) {
            PrinterConnectionType.BLUETOOTH,
            PrinterConnectionType.BLUETOOTH_LE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN, 
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } else {
                    arrayOf(
                        Manifest.permission.BLUETOOTH, 
                        Manifest.permission.BLUETOOTH_ADMIN, 
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                }
            }
            else -> emptyArray()
        }

        if (permissions.isEmpty()) {
            onResult(true)
            return
        }

        activity.requestPermissions(permissions, 1001)
    }

    fun onPermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 1001) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            android.util.Log.d("KmpPrinter", "Permission result: $allGranted")
            permissionCallback?.invoke(allGranted)
            permissionCallback = null
        }
    }

    private fun check(context: android.content.Context, perm: String): Boolean {
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}
