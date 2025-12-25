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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import com.famy.tree.domain.model.CareerStatus
import com.famy.tree.domain.model.EducationLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationCareerSection(
    educationLevel: EducationLevel,
    education: String,
    almaMater: String,
    careerStatus: CareerStatus,
    occupation: String,
    employer: String,
    militaryService: String,
    skills: List<String>,
    achievements: List<String>,
    onEducationLevelChange: (EducationLevel) -> Unit,
    onEducationChange: (String) -> Unit,
    onAlmaMaterChange: (String) -> Unit,
    onCareerStatusChange: (CareerStatus) -> Unit,
    onOccupationChange: (String) -> Unit,
    onEmployerChange: (String) -> Unit,
    onMilitaryServiceChange: (String) -> Unit,
    onAddSkill: (String) -> Unit,
    onRemoveSkill: (String) -> Unit,
    onAddAchievement: (String) -> Unit,
    onRemoveAchievement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EnumDropdown(
                label = "Education Level",
                selectedValue = educationLevel.displayName,
                options = EducationLevel.entries.map { it.displayName },
                onOptionSelected = { displayName ->
                    val level = EducationLevel.entries.find { it.displayName == displayName }
                        ?: EducationLevel.UNKNOWN
                    onEducationLevelChange(level)
                },
                modifier = Modifier.weight(1f)
            )
            EnumDropdown(
                label = "Career Status",
                selectedValue = careerStatus.displayName,
                options = CareerStatus.entries.map { it.displayName },
                onOptionSelected = { displayName ->
                    val status = CareerStatus.entries.find { it.displayName == displayName }
                        ?: CareerStatus.UNKNOWN
                    onCareerStatusChange(status)
                },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = education,
            onValueChange = onEducationChange,
            label = { Text("Field of Study / Degree") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = almaMater,
            onValueChange = onAlmaMaterChange,
            label = { Text("School / University") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = occupation,
                onValueChange = onOccupationChange,
                label = { Text("Occupation / Job Title") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )
            OutlinedTextField(
                value = employer,
                onValueChange = onEmployerChange,
                label = { Text("Employer / Company") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )
        }

        OutlinedTextField(
            value = militaryService,
            onValueChange = onMilitaryServiceChange,
            label = { Text("Military Service") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("e.g., US Army, 2010-2014, Sergeant") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )

        ChipInputField(
            label = "Skills & Expertise",
            items = skills,
            onAddItem = onAddSkill,
            onRemoveItem = onRemoveSkill,
            presets = listOf(
                "Leadership", "Communication", "Problem Solving", "Teaching",
                "Programming", "Design", "Marketing", "Finance", "Engineering",
                "Medical", "Legal", "Sales", "Management", "Woodworking"
            )
        )

        ChipInputField(
            label = "Achievements & Awards",
            items = achievements,
            onAddItem = onAddAchievement,
            onRemoveItem = onRemoveAchievement,
            presets = listOf(
                "Dean's List", "Valedictorian", "Employee of the Year",
                "Published Author", "Patent Holder", "Olympic Athlete",
                "Military Honors", "Volunteer Recognition", "Industry Award"
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnumDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipInputField(
    label: String,
    items: List<String>,
    onAddItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    presets: List<String>,
    modifier: Modifier = Modifier
) {
    var newItem by remember { mutableStateOf("") }
    var showPresets by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (items.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item ->
                    InputChip(
                        selected = false,
                        onClick = { onRemoveItem(item) },
                        label = { Text(item) },
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
                value = newItem,
                onValueChange = { newItem = it },
                label = { Text("Add ${label.lowercase().removeSuffix("s")}") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                )
            )
            IconButton(
                onClick = {
                    if (newItem.isNotBlank()) {
                        onAddItem(newItem.trim())
                        newItem = ""
                    }
                },
                enabled = newItem.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(onClick = { showPresets = !showPresets }) {
            Text(if (showPresets) "Hide suggestions" else "Show suggestions")
        }

        if (showPresets) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presets
                    .filter { it !in items }
                    .forEach { preset ->
                        AssistChip(
                            onClick = { onAddItem(preset) },
                            label = { Text(preset) }
                        )
                    }
            }
        }
    }
}
