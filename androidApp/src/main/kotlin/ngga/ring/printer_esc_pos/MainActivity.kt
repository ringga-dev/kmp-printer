package ngga.ring.printer_esc_pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import ngga.ring.printer.manager.PrinterInitializer
import ngga.ring.printer.manager.PrinterPermissionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Initialize the printer library with the application context
        PrinterInitializer.initialize(this)

        setContent {
            App()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        // Relay result to the library
        PrinterPermissionManager().onPermissionResult(requestCode, permissions, grantResults)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}