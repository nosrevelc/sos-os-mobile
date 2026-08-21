package br.com.sos.osmobile.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import br.com.sos.osmobile.data.repository.CalendarAccount
import br.com.sos.osmobile.data.print.BluetoothPrinterDevice
import br.com.sos.osmobile.data.print.BluetoothThermalPrinter
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.data.print.ThermalPrintStyle
import br.com.sos.osmobile.ui.input.InputMasks
import kotlinx.coroutines.launch

internal fun driveFolderDisplayName(context: Context, uriValue: String): String {
    if (uriValue.isBlank()) return "nenhuma pasta selecionada"
    return runCatching {
        DocumentFile.fromTreeUri(context, Uri.parse(uriValue))
            ?.name
            ?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: "pasta configurada"
}

internal fun DocumentFile.isDriveRootLike(): Boolean {
    val normalized = name.orEmpty().trim().lowercase()
    return normalized in setOf("meu drive", "my drive", "drive", "arquivos do drive")
}

@Composable
internal fun SettingsSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    OutlinedButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (expanded) "v $title" else "> $title",
            fontWeight = FontWeight.SemiBold,
        )
    }
    if (expanded) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun ContactAccountSelector(
    selectedAccount: String,
    accounts: List<br.com.sos.osmobile.data.repository.ContactAccount>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = accounts.firstOrNull { it.storageKey == selectedAccount || it.name == selectedAccount }?.label ?: "Agenda local do aparelho"

    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(selectedLabel)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(if (selectedAccount.isBlank()) "Agenda local do aparelho *" else "Agenda local do aparelho") },
            onClick = {
                onSelected("")
                expanded = false
            },
        )
        accounts.forEach { account ->
            DropdownMenuItem(
                text = { Text(if (account.storageKey == selectedAccount || account.name == selectedAccount) "${account.label} *" else account.label) },
                onClick = {
                onSelected(account.storageKey)
                    expanded = false
                },
            )
        }
    }
}

@Composable
internal fun CalendarSelector(
    selectedCalendarId: String,
    selectedCalendarLabel: String,
    calendars: List<CalendarAccount>,
    onSelected: (CalendarAccount) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = calendars.firstOrNull { it.id.toString() == selectedCalendarId }?.label
        ?: selectedCalendarLabel.takeIf { it.isNotBlank() }
        ?: "Selecione a agenda padrao"

    OutlinedButton(
        onClick = { expanded = calendars.isNotEmpty() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(selectedLabel)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        calendars.forEach { calendar ->
            DropdownMenuItem(
                text = { Text(if (calendar.id.toString() == selectedCalendarId) "${calendar.label} *" else calendar.label) },
                onClick = {
                    onSelected(calendar)
                    expanded = false
                },
            )
        }
    }
}

@Composable
internal fun BluetoothPrinterSelector(
    selectedAddress: String,
    printers: List<BluetoothPrinterDevice>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = printers.firstOrNull { it.address == selectedAddress }?.label
        ?: selectedAddress.takeIf { it.isNotBlank() }
        ?: "Nenhuma impressora selecionada"

    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(selectedLabel)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(if (selectedAddress.isBlank()) "Nenhuma impressora *" else "Nenhuma impressora") },
            onClick = {
                onSelected("")
                expanded = false
            },
        )
        printers.forEach { printer ->
            DropdownMenuItem(
                text = { Text(if (printer.address == selectedAddress) "${printer.label} *" else printer.label) },
                onClick = {
                    onSelected(printer.address)
                    expanded = false
                },
            )
        }
    }
}

@Composable
internal fun FiscalEnvironmentSelector(
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Homologacao", "Producao")
    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Ambiente: $selected")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(if (option == selected) "$option *" else option) },
                onClick = {
                    onSelected(option)
                    expanded = false
                },
            )
        }
    }
}

@Composable
internal fun PrintOptionSelector(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedValue }?.second ?: label

    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text("$label: $selectedLabel")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { (value, text) ->
            DropdownMenuItem(
                text = { Text(if (value == selectedValue) "$text *" else text) },
                onClick = {
                    onSelected(value)
                    expanded = false
                },
            )
        }
    }
}

@Composable
internal fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
internal fun PolicyButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(if (selected) "$label *" else label)
    }
}

