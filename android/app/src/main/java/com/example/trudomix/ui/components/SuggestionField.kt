package com.example.trudomix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

fun matchingSuggestions(value: String, options: List<String>): List<String> = options
    .filter { it.contains(value.trim(), ignoreCase = true) }
    .distinctBy { it.lowercase() }.sortedBy { it.lowercase() }.take(8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionField(value: String, onChange: (String) -> Unit, label: String, options: List<String>,
                    modifier: Modifier = Modifier, allowCustom: Boolean = false, onSelect: (String) -> Unit = onChange) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = remember(value, options) { matchingSuggestions(value, options) }
    val custom = allowCustom && value.isNotBlank() && options.none { it.equals(value.trim(), true) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(value, { onChange(it); expanded = true }, label = { Text(label) }, singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable))
        ExposedDropdownMenu(expanded = expanded && (suggestions.isNotEmpty() || custom), onDismissRequest = { expanded = false }) {
            suggestions.forEach { suggestion -> DropdownMenuItem(text = { Text(suggestion) }, onClick = { onSelect(suggestion); expanded = false }) }
            if (custom) DropdownMenuItem(text = { Text("Использовать «${value.trim()}»") }, onClick = { onSelect(value.trim()); expanded = false })
        }
    }
}
