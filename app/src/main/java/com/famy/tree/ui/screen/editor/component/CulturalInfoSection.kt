package com.famy.tree.ui.screen.editor.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.famy.tree.domain.model.RelationshipStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CulturalInfoSection(
    relationshipStatus: RelationshipStatus,
    religion: String,
    nationality: String,
    ethnicity: String,
    languages: List<String>,
    interests: List<String>,
    onRelationshipStatusChange: (RelationshipStatus) -> Unit,
    onReligionChange: (String) -> Unit,
    onNationalityChange: (String) -> Unit,
    onEthnicityChange: (String) -> Unit,
    onAddLanguage: (String) -> Unit,
    onRemoveLanguage: (String) -> Unit,
    onAddInterest: (String) -> Unit,
    onRemoveInterest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EnumDropdown(
            label = "Relationship Status",
            selectedValue = relationshipStatus.displayName,
            options = RelationshipStatus.entries.map { it.displayName },
            onOptionSelected = { displayName ->
                val status = RelationshipStatus.entries.find { it.displayName == displayName }
                    ?: RelationshipStatus.UNKNOWN
                onRelationshipStatusChange(status)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = nationality,
                onValueChange = onNationalityChange,
                label = { Text("Nationality") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )
            OutlinedTextField(
                value = ethnicity,
                onValueChange = onEthnicityChange,
                label = { Text("Ethnicity") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )
        }

        OutlinedTextField(
            value = religion,
            onValueChange = onReligionChange,
            label = { Text("Religion / Beliefs") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )

        LanguagesField(
            languages = languages,
            onAddLanguage = onAddLanguage,
            onRemoveLanguage = onRemoveLanguage
        )

        InterestsField(
            interests = interests,
            onAddInterest = onAddInterest,
            onRemoveInterest = onRemoveInterest
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguagesField(
    languages: List<String>,
    onAddLanguage: (String) -> Unit,
    onRemoveLanguage: (String) -> Unit
) {
    var newLanguage by remember { mutableStateOf("") }
    var showPresets by remember { mutableStateOf(false) }

    val commonLanguages = listOf(
        "English", "Spanish", "Mandarin", "Hindi", "Arabic", "Portuguese",
        "Bengali", "Russian", "Japanese", "German", "French", "Korean",
        "Vietnamese", "Italian", "Turkish", "Polish", "Dutch", "Thai"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Languages Spoken",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (languages.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                languages.forEach { language ->
                    InputChip(
                        selected = false,
                        onClick = { onRemoveLanguage(language) },
                        label = { Text(language) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newLanguage,
                onValueChange = { newLanguage = it },
                label = { Text("Add language") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                )
            )
            IconButton(
                onClick = {
                    if (newLanguage.isNotBlank()) {
                        onAddLanguage(newLanguage.trim())
                        newLanguage = ""
                    }
                },
                enabled = newLanguage.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        TextButton(onClick = { showPresets = !showPresets }) {
            Text(if (showPresets) "Hide common languages" else "Show common languages")
        }

        if (showPresets) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                commonLanguages
                    .filter { it !in languages }
                    .forEach { lang ->
                        AssistChip(
                            onClick = { onAddLanguage(lang) },
                            label = { Text(lang) }
                        )
                    }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterestsField(
    interests: List<String>,
    onAddInterest: (String) -> Unit,
    onRemoveInterest: (String) -> Unit
) {
    var newInterest by remember { mutableStateOf("") }
    var showPresets by remember { mutableStateOf(false) }

    val presetInterests = listOf(
        "Reading", "Music", "Sports", "Cooking", "Travel",
        "Photography", "Gardening", "Art", "Movies", "Gaming",
        "Hiking", "Fishing", "Dancing", "Writing", "Crafts",
        "Yoga", "Chess", "History", "Science", "Nature",
        "Technology", "Fashion", "Cars", "Volunteering", "Animals"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Interests & Hobbies",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (interests.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                interests.forEach { interest ->
                    InputChip(
                        selected = false,
                        onClick = { onRemoveInterest(interest) },
                        label = { Text(interest) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newInterest,
                onValueChange = { newInterest = it },
                label = { Text("Add interest") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                )
            )
            IconButton(
                onClick = {
                    if (newInterest.isNotBlank()) {
                        onAddInterest(newInterest.trim())
                        newInterest = ""
                    }
                },
                enabled = newInterest.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        TextButton(onClick = { showPresets = !showPresets }) {
            Text(if (showPresets) "Hide preset interests" else "Show preset interests")
        }

        if (showPresets) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presetInterests
                    .filter { it !in interests }
                    .forEach { preset ->
                        AssistChip(
                            onClick = { onAddInterest(preset) },
                            label = { Text(preset) }
                        )
                    }
            }
        }
    }
}
