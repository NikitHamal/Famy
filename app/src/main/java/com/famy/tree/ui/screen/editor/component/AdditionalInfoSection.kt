package com.famy.tree.ui.screen.editor.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.famy.tree.R

@Composable
fun AdditionalInfoSection(
    biography: String,
    notes: String,
    customFields: Map<String, String>,
    onBiographyChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddCustomField: (String, String) -> Unit,
    onRemoveCustomField: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddCustomFieldDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = biography,
            onValueChange = onBiographyChange,
            label = { Text(stringResource(R.string.profile_biography)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            placeholder = { Text("A brief life story, memories, or description...") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            )
        )

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.profile_notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            placeholder = { Text("Private notes, reminders, or additional information...") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            )
        )

        if (customFields.isNotEmpty()) {
            Text(
                text = stringResource(R.string.profile_custom_fields),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            customFields.forEach { (key, value) ->
                CustomFieldItem(
                    key = key,
                    value = value,
                    onRemove = { onRemoveCustomField(key) }
                )
            }
        }

        TextButton(onClick = { showAddCustomFieldDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Custom Field")
        }
    }

    if (showAddCustomFieldDialog) {
        AddCustomFieldDialog(
            onAdd = { key, value ->
                onAddCustomField(key, value)
                showAddCustomFieldDialog = false
            },
            onDismiss = { showAddCustomFieldDialog = false }
        )
    }
}

@Composable
private fun CustomFieldItem(
    key: String,
    value: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(Modifier),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}

@Composable
private fun AddCustomFieldDialog(
    onAdd: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Field") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Field Name") },
                    singleLine = true,
                    placeholder = { Text("e.g., Social Security Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(key.trim(), value.trim()) },
                enabled = key.isNotBlank() && value.isNotBlank()
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
