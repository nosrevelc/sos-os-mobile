package br.com.sos.osmobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity

@Composable
fun ServiceProductSearchSelector(
    services: List<ServiceProductEntity>,
    selectedServiceProductId: Long?,
    onServiceSelected: (ServiceProductEntity) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    if (services.isEmpty()) {
        Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    var query by remember { mutableStateOf("") }
    var showResults by remember { mutableStateOf(false) }
    val selected = services.firstOrNull { it.id == selectedServiceProductId }

    LaunchedEffect(selectedServiceProductId, services) {
        query = selected?.let { "${it.codigo} - ${it.nome}" }.orEmpty()
    }

    val filteredServices = remember(services, query) {
        val normalized = query.trim()
        if (normalized.isBlank()) {
            services.take(12)
        } else {
            services
                .filter {
                    it.codigo.contains(normalized, ignoreCase = true) ||
                        it.nome.contains(normalized, ignoreCase = true) ||
                        it.categoria.orEmpty().contains(normalized, ignoreCase = true) ||
                        it.descricao.orEmpty().contains(normalized, ignoreCase = true)
                }
                .take(12)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                showResults = true
            },
            label = { Text("Buscar servico/produto") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        showResults = true
                    }
                },
        )
        if (showResults) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                if (filteredServices.isEmpty()) {
                    Text(
                        text = "Nenhum servico/produto encontrado.",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    filteredServices.forEach { service ->
                        TextButton(
                            onClick = {
                                onServiceSelected(service)
                                query = "${service.codigo} - ${service.nome}"
                                showResults = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${service.codigo} - ${service.nome}")
                        }
                    }
                }
            }
        }
    }
}
