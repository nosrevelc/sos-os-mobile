package br.com.sos.osmobile.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.sos.osmobile.data.model.CpfCnpjPolicy

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val readContactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.loadContactAccounts()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Modulos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        SettingSwitch("Orcamento", checked = settings.orcamento, onCheckedChange = { viewModel.setModule("modulo_orcamento", it) })
        SettingSwitch("Fotos", checked = settings.fotos, onCheckedChange = { viewModel.setModule("modulo_fotos", it) })
        SettingSwitch("Assinatura", checked = settings.assinatura, onCheckedChange = { viewModel.setModule("modulo_assinatura", it) })
        SettingSwitch("Checklist", checked = settings.checklist, onCheckedChange = { viewModel.setModule("modulo_checklist", it) })
        SettingSwitch("Garantia", checked = settings.garantia, onCheckedChange = { viewModel.setModule("modulo_garantia", it) })
        SettingSwitch("Financeiro", checked = settings.financeiro, onCheckedChange = { viewModel.setModule("modulo_financeiro", it) })

        Text("CPF/CNPJ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PolicyButton("Nao usar", settings.cpfCnpjPolicy == CpfCnpjPolicy.NotUsed) {
                viewModel.setCpfCnpjPolicy(CpfCnpjPolicy.NotUsed)
            }
            PolicyButton("Opcional", settings.cpfCnpjPolicy == CpfCnpjPolicy.Optional) {
                viewModel.setCpfCnpjPolicy(CpfCnpjPolicy.Optional)
            }
            PolicyButton("Obrigatorio", settings.cpfCnpjPolicy == CpfCnpjPolicy.Required) {
                viewModel.setCpfCnpjPolicy(CpfCnpjPolicy.Required)
            }
        }

        Text("Agenda Google", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.loadContactAccounts()
                } else {
                    readContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Buscar agendas do aparelho")
        }
        ContactAccountSelector(
            selectedAccount = settings.contactsGoogleAccount,
            accounts = settings.contactAccounts,
            onSelected = viewModel::setContactsGoogleAccount,
        )
        settings.contactsMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = "Agenda local salva apenas no aparelho. Agenda Google sincroniza conforme a configuracao do Android.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContactAccountSelector(
    selectedAccount: String,
    accounts: List<br.com.sos.osmobile.data.repository.ContactAccount>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = accounts.firstOrNull { it.name == selectedAccount }?.label ?: "Agenda local do aparelho"

    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(selectedLabel)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Agenda local do aparelho") },
            onClick = {
                onSelected("")
                expanded = false
            },
        )
        accounts.forEach { account ->
            DropdownMenuItem(
                text = { Text(account.label) },
                onClick = {
                    onSelected(account.name)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun SettingSwitch(
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
private fun PolicyButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick) {
        Text(if (selected) "$label *" else label)
    }
}
