package com.famy.tree.ui.screen.editor.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.famy.tree.R
import com.famy.tree.domain.model.GeocodedLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LifeEventsSection(
    birthDate: Long?,
    birthPlace: String,
    birthPlaceLatitude: Double?,
    birthPlaceSearchResults: List<GeocodedLocation>,
    isSearchingBirthPlace: Boolean,
    deathDate: Long?,
    deathPlace: String,
    deathPlaceLatitude: Double?,
    deathPlaceSearchResults: List<GeocodedLocation>,
    isSearchingDeathPlace: Boolean,
    causeOfDeath: String,
    burialPlace: String,
    burialLatitude: Double?,
    burialPlaceSearchResults: List<GeocodedLocation>,
    isSearchingBurialPlace: Boolean,
    isLiving: Boolean,
    birthDateError: String?,
    deathDateError: String?,
    onBirthDateClick: () -> Unit,
    onBirthDateClear: () -> Unit,
    onBirthPlaceChange: (String) -> Unit,
    onBirthPlaceSelected: (GeocodedLocation) -> Unit,
    onBirthPlaceSearchDismiss: () -> Unit,
    onDeathDateClick: () -> Unit,
    onDeathDateClear: () -> Unit,
    onDeathPlaceChange: (String) -> Unit,
    onDeathPlaceSelected: (GeocodedLocation) -> Unit,
    onDeathPlaceSearchDismiss: () -> Unit,
    onCauseOfDeathChange: (String) -> Unit,
    onBurialPlaceChange: (String) -> Unit,
    onBurialPlaceSelected: (GeocodedLocation) -> Unit,
    onBurialPlaceSearchDismiss: () -> Unit,
    onIsLivingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DatePickerField(
            label = stringResource(R.string.profile_birth_date),
            date = birthDate,
            error = birthDateError,
            onClick = onBirthDateClick,
            onClear = onBirthDateClear
        )

        LocationSearchField(
            value = birthPlace,
            onValueChange = onBirthPlaceChange,
            label = stringResource(R.string.profile_birth_place),
            searchResults = birthPlaceSearchResults,
            isSearching = isSearchingBirthPlace,
            onLocationSelected = onBirthPlaceSelected,
            onDismissResults = onBirthPlaceSearchDismiss,
            hasCoordinates = birthPlaceLatitude != null
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.profile_is_living),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = isLiving,
                onCheckedChange = onIsLivingChange
            )
        }

        if (!isLiving) {
            DatePickerField(
                label = stringResource(R.string.profile_death_date),
                date = deathDate,
                error = deathDateError,
                onClick = onDeathDateClick,
                onClear = onDeathDateClear
            )

            LocationSearchField(
                value = deathPlace,
                onValueChange = onDeathPlaceChange,
                label = stringResource(R.string.profile_death_place),
                searchResults = deathPlaceSearchResults,
                isSearching = isSearchingDeathPlace,
                onLocationSelected = onDeathPlaceSelected,
                onDismissResults = onDeathPlaceSearchDismiss,
                hasCoordinates = deathPlaceLatitude != null
            )

            OutlinedTextField(
                value = causeOfDeath,
                onValueChange = onCauseOfDeathChange,
                label = { Text("Cause of Death") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )

            LocationSearchField(
                value = burialPlace,
                onValueChange = onBurialPlaceChange,
                label = "Burial / Memorial Location",
                searchResults = burialPlaceSearchResults,
                isSearching = isSearchingBurialPlace,
                onLocationSelected = onBurialPlaceSelected,
                onDismissResults = onBurialPlaceSearchDismiss,
                hasCoordinates = burialLatitude != null
            )
        }
    }
}

@Composable
fun DatePickerField(
    label: String,
    date: Long?,
    error: String?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    OutlinedTextField(
        value = date?.let { dateFormat.format(Date(it)) } ?: "",
        onValueChange = {},
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        readOnly = true,
        enabled = false,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        trailingIcon = {
            Row {
                if (date != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.filter_clear))
                    }
                }
                IconButton(onClick = onClick) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    searchResults: List<GeocodedLocation>,
    isSearching: Boolean,
    onLocationSelected: (GeocodedLocation) -> Unit,
    onDismissResults: () -> Unit,
    hasCoordinates: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded && searchResults.isNotEmpty(),
            onExpandedChange = { }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    onValueChange(newValue)
                    expanded = true
                },
                label = { Text(label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (hasCoordinates) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier,
                            strokeWidth = 2.dp
                        )
                    }
                },
                supportingText = if (hasCoordinates) {
                    { Text("Location coordinates saved", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                )
            )

            ExposedDropdownMenu(
                expanded = expanded && searchResults.isNotEmpty(),
                onDismissRequest = {
                    expanded = false
                    onDismissResults()
                }
            ) {
                searchResults.forEach { location ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = location.displayName,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            onLocationSelected(location)
                            expanded = false
                            focusManager.clearFocus()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}
