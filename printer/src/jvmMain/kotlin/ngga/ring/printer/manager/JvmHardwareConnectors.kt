package ngga.ring.printer.manager

import com.fazecast.jSerialComm.SerialPort
import ngga.ring.printer.model.PrinterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/**
 * JVM Implementation for Serial/USB/Bluetooth Serial printers using jSerialComm.
 */
class JvmSerialConnector : BasePrinterConnector() {
    private var serialPort: SerialPort? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            configureFlowControl(config)
            val portDescriptor = config.address ?: return@withContext false
            val port = SerialPort.getCommPort(portDescriptor)
            
            // Optimized settings for thermal printers
            port.baudRate = config.baudRate
            port.numDataBits = 8
            port.numStopBits = SerialPort.ONE_STOP_BIT
            port.parity = SerialPort.NO_PARITY
            
            if (port.openPort()) {
                port.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING or SerialPort.TIMEOUT_WRITE_BLOCKING,
                    config.readTimeoutMs, 
                    config.connectionTimeoutMs
                )
                serialPort = port
                inputStream = port.inputStream
                outputStream = port.outputStream
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("PrinterJVM: Serial connection failed: ${e.message}")
            false
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val out = outputStream ?: return@withContext false
            out.write(data)
            out.flush()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val input = inputStream ?: return@withContext null
            val buffer = ByteArray(count)
            val read = input.read(buffer)
            if (read > 0) buffer.copyOf(read) else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            inputStream?.close()
            outputStream?.close()
            serialPort?.closePort()
            inputStream = null
            outputStream = null
            serialPort = null
        } catch (e: Exception) {}
    }

    override fun isConnected(): Boolean = serialPort?.isOpen ?: false
}
