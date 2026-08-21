package br.com.sos.osmobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class StatusOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> StatusSelectorCompact(
    options: List<StatusOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        options.forEach { option ->
            val isSelected = option.value == selected
            if (isSelected) {
                Button(onClick = { onSelected(option.value) }) {
                    StatusOptionContent(option)
                }
            } else {
                OutlinedButton(onClick = { onSelected(option.value) }) {
                    StatusOptionContent(option)
                }
            }
        }
    }
}

@Composable
private fun <T> StatusOptionContent(option: StatusOption<T>) {
    option.icon?.let { Icon(it, contentDescription = null) }
    Text(option.label)
}
