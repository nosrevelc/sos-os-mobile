package br.com.sos.osmobile.feature.audit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.sos.osmobile.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow
import java.text.DateFormat
import java.util.Date

@Composable
fun AuditScreen(auditFlow: Flow<List<AuditLogEntity>>) {
    val logs by auditFlow.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (logs.isEmpty()) {
            item {
                Text(
                    text = "O historico sera preenchido automaticamente pelas acoes relevantes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(logs, key = { it.id }) { log ->
                AuditRow(log)
            }
        }
    }
}

@Composable
private fun AuditRow(log: AuditLogEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(log.acao, fontWeight = FontWeight.SemiBold)
            Text("${log.modulo} - ${formatDate(log.timestamp)}", style = MaterialTheme.typography.bodySmall)
            log.detalhes?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
