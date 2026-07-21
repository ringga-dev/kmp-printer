package ngga.ring.printer_esc_pos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PresetOption(
    val label: String,
    val value: String
)

@Composable
fun PresetSelector(
    label: String,
    presets: List<PresetOption>,
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustom by remember { mutableStateOf(presets.none { it.value == selectedValue }) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        presets.forEach { preset ->
            val isSelected = selectedValue == preset.value
            Card(
                onClick = {
                    onValueSelected(preset.value)
                    showCustom = false
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle
                        else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            preset.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            preset.value,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Custom option
        val isCustom = showCustom || presets.none { it.value == selectedValue }
        Card(
            onClick = { showCustom = true },
            colors = CardDefaults.cardColors(
                containerColor = if (isCustom) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isCustom) Icons.Filled.CheckCircle
                    else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isCustom) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Custom...", modifier = Modifier.weight(1f))
            }
        }

        if (isCustom) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = onValueSelected,
                label = { Text("Enter $label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Paste the full UUID or path") }
            )
        }
    }
}
