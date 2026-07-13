package br.com.sos.osmobile.data.print

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.UUID

data class ThermalPrintContent(
    val header: String = "",
    val body: String,
    val footer: String = "",
) {
    fun asText(): String = listOf(header, body, footer)
        .filter { it.isNotBlank() }
        .joinToString(separator = "\n\n")
}

data class ThermalPrintStyle(
    val font: String = "A",
    val textSize: String = "normal",
    val headerBold: Boolean = true,
    val headerAlignment: String = "center",
)

data class ThermalTextBlock(
    val text: String,
    val alignment: String = "left",
    val bold: Boolean = false,
    val size: String = "normal",
    val font: String = "A",
)

data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
) {
    val label: String = "$name - $address"
}

object BluetoothThermalPrinter {
    private val serialPortUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val charsPerLine58mm = 32

    fun hasBluetoothPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun listBondedPrinters(context: Context): List<BluetoothPrinterDevice> {
        if (!hasBluetoothPermission(context)) return emptyList()
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices
            .orEmpty()
            .sortedBy { it.name.orEmpty() }
            .map { BluetoothPrinterDevice(name = it.name ?: "Bluetooth", address = it.address) }
    }

    suspend fun print58mm(
        context: Context,
        deviceAddress: String,
        text: String,
        copies: Int,
        style: ThermalPrintStyle = ThermalPrintStyle(),
    ): Result<Unit> = withContext(Dispatchers.IO) {
        print58mm(
            context = context,
            deviceAddress = deviceAddress,
            content = ThermalPrintContent(body = text),
            copies = copies,
            style = style,
        )
    }

    suspend fun print58mm(
        context: Context,
        deviceAddress: String,
        content: ThermalPrintContent,
        copies: Int,
        style: ThermalPrintStyle = ThermalPrintStyle(),
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(deviceAddress.isNotBlank()) { "Selecione a impressora nas configuracoes." }
            require(hasBluetoothPermission(context)) { "Permissao Bluetooth nao concedida." }
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: error("Bluetooth indisponivel.")
            val device = adapter.getRemoteDevice(deviceAddress)
            val payload = buildPayload(content, style)
            repeat(copies.coerceAtLeast(1)) {
                writeToDevice(device, payload)
            }
        }
    }

    suspend fun print58mmBlocks(
        context: Context,
        deviceAddress: String,
        blocks: List<ThermalTextBlock>,
        copies: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(deviceAddress.isNotBlank()) { "Selecione a impressora nas configuracoes." }
            require(hasBluetoothPermission(context)) { "Permissao Bluetooth nao concedida." }
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: error("Bluetooth indisponivel.")
            val device = adapter.getRemoteDevice(deviceAddress)
            val payload = buildPayload(blocks)
            repeat(copies.coerceAtLeast(1)) {
                writeToDevice(device, payload)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeToDevice(device: BluetoothDevice, payload: ByteArray) {
        device.createRfcommSocketToServiceRecord(serialPortUuid).use { socket ->
            socket.connect()
            socket.outputStream.use { output ->
                output.write(payload)
                output.flush()
            }
        }
    }

    private fun buildPayload(content: ThermalPrintContent, style: ThermalPrintStyle): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        output.write(byteArrayOf(0x1B, 0x40))
        setFont(output, style)
        if (content.header.isNotBlank()) {
            setAlignment(output, style.headerAlignment)
            setBold(output, style.headerBold)
            setSize(output, style.textSize)
            writeText(output, content.header, lineWidth(style))
            resetText(output, style)
            output.write('\n'.code)
        }
        setAlignment(output, "left")
        setBold(output, false)
        setSize(output, style.textSize)
        writeText(output, content.body, lineWidth(style))
        if (content.footer.isNotBlank()) {
            output.write('\n'.code)
            setAlignment(output, "center")
            writeText(output, content.footer, lineWidth(style))
        }
        output.write("\n\n\n".toByteArray(Charsets.ISO_8859_1))
        return output.toByteArray()
    }

    private fun buildPayload(blocks: List<ThermalTextBlock>): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        output.write(byteArrayOf(0x1B, 0x40))
        blocks.forEach { block ->
            val style = ThermalPrintStyle(font = block.font, textSize = block.size)
            setFont(output, style)
            setAlignment(output, block.alignment)
            setBold(output, block.bold)
            setSize(output, block.size)
            writeText(output, block.text, lineWidth(style))
        }
        output.write("\n\n\n".toByteArray(Charsets.ISO_8859_1))
        return output.toByteArray()
    }

    private fun resetText(output: java.io.ByteArrayOutputStream, style: ThermalPrintStyle) {
        setAlignment(output, "left")
        setBold(output, false)
        setSize(output, style.textSize)
    }

    private fun setFont(output: java.io.ByteArrayOutputStream, style: ThermalPrintStyle) {
        output.write(byteArrayOf(0x1B, 0x4D, if (style.font == "B") 1 else 0))
    }

    private fun setAlignment(output: java.io.ByteArrayOutputStream, alignment: String) {
        val value = when (alignment) {
            "center" -> 1
            "right" -> 2
            else -> 0
        }
        output.write(byteArrayOf(0x1B, 0x61, value.toByte()))
    }

    private fun setBold(output: java.io.ByteArrayOutputStream, enabled: Boolean) {
        output.write(byteArrayOf(0x1B, 0x45, if (enabled) 1 else 0))
    }

    private fun setSize(output: java.io.ByteArrayOutputStream, size: String) {
        output.write(byteArrayOf(0x1D, 0x21, if (size == "large") 0x11 else 0x00))
    }

    private fun writeText(output: java.io.ByteArrayOutputStream, text: String, width: Int) {
        val printable = normalize(text)
            .lineSequence()
            .flatMap { wrapLine(it, width) }
            .joinToString(separator = "\n", postfix = "\n")
        output.write(printable.toByteArray(Charsets.ISO_8859_1))
    }

    private fun lineWidth(style: ThermalPrintStyle): Int {
        val base = if (style.font == "B") 42 else charsPerLine58mm
        return if (style.textSize == "large") base / 2 else base
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("R$", "R$")

    private fun wrapLine(line: String, width: Int): List<String> {
        if (line.length <= width) return listOf(line)
        val result = mutableListOf<String>()
        var current = line
        while (current.length > width) {
            val split = current.substring(0, width).lastIndexOf(' ').takeIf { it > 0 } ?: width
            result += current.substring(0, split).trimEnd()
            current = current.substring(split).trimStart()
        }
        if (current.isNotBlank()) result += current
        return result
    }
}
