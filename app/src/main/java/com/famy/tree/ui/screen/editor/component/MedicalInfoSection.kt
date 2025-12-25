package com.famy.tree.ui.screen.editor.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.famy.tree.domain.model.BloodType

@Composable
fun MedicalInfoSection(
    bloodType: BloodType?,
    medicalInfo: String,
    onBloodTypeChange: (BloodType?) -> Unit,
    onMedicalInfoChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BloodTypeDropdown(
            selectedBloodType = bloodType,
            onBloodTypeChange = onBloodTypeChange,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = medicalInfo,
            onValueChange = onMedicalInfoChange,
            label = { Text("Medical Notes (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            placeholder = { Text("Allergies, conditions, medical history...") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            )
        )
    }
}

@Composable
private fun BloodTypeDropdown(
    selectedBloodType: BloodType?,
    onBloodTypeChange: (BloodType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Unknown") + BloodType.entries.map { it.displayName }
    val selectedValue = selectedBloodType?.displayName ?: "Unknown"

    EnumDropdown(
        label = "Blood Type",
        selectedValue = selectedValue,
        options = options,
        onOptionSelected = { displayName ->
            val bloodType = if (displayName == "Unknown") {
                null
            } else {
                BloodType.entries.find { it.displayName == displayName }
            }
            onBloodTypeChange(bloodType)
        },
        modifier = modifier
    )
}
