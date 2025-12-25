package com.famy.tree.ui.screen.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.famy.tree.R
import com.famy.tree.domain.model.BloodType
import com.famy.tree.domain.model.CareerStatus
import com.famy.tree.domain.model.EducationLevel
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.GeocodedLocation
import com.famy.tree.domain.model.RelationshipStatus
import com.famy.tree.ui.component.BackButton
import com.famy.tree.ui.component.LoadingOverlay
import com.famy.tree.ui.component.LoadingScreen
import com.famy.tree.ui.component.UnsavedChangesDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMemberScreen(
    treeId: Long,
    memberId: Long?,
    onNavigateBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: EditMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showBirthDatePicker by remember { mutableStateOf(false) }
    var showDeathDatePicker by remember { mutableStateOf(false) }
    var showAddCustomFieldDialog by remember { mutableStateOf(false) }
    var showAddSocialLinkDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.updatePhoto(context, it) }
    }

    BackHandler(enabled = uiState.hasChanges) {
        showUnsavedChangesDialog = true
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onDiscard = onNavigateBack,
            onDismiss = { showUnsavedChangesDialog = false }
        )
    }

    if (showBirthDatePicker) {
        DatePickerDialogComposable(
            initialDate = uiState.form.birthDate,
            onDateSelected = { viewModel.updateBirthDate(it) },
            onDismiss = { showBirthDatePicker = false }
        )
    }

    if (showDeathDatePicker) {
        DatePickerDialogComposable(
            initialDate = uiState.form.deathDate,
            onDateSelected = { viewModel.updateDeathDate(it) },
            onDismiss = { showDeathDatePicker = false }
        )
    }

    if (showAddCustomFieldDialog) {
        AddFieldDialog(
            title = "Add Custom Field",
            keyLabel = "Field Name",
            valueLabel = "Value",
            onAdd = { key, value ->
                viewModel.addCustomField(key, value)
                showAddCustomFieldDialog = false
            },
            onDismiss = { showAddCustomFieldDialog = false }
        )
    }

    if (showAddSocialLinkDialog) {
        AddFieldDialog(
            title = "Add Social Link",
            keyLabel = "Platform",
            valueLabel = "URL / Username",
            onAdd = { platform, url ->
                viewModel.addSocialLink(platform, url)
                showAddSocialLinkDialog = false
            },
            onDismiss = { showAddSocialLinkDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditing) stringResource(R.string.action_edit)
                        else stringResource(R.string.cd_add_member)
                    )
                },
                navigationIcon = {
                    BackButton(onClick = {
                        if (uiState.hasChanges) {
                            showUnsavedChangesDialog = true
                        } else {
                            onNavigateBack()
                        }
                    })
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save(onSaved) },
                        enabled = !uiState.isSaving && uiState.validationErrors.isEmpty()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_save))
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Photo Section
                    item {
                        PhotoSectionCompact(
                            photoPath = uiState.form.photoPath,
                            firstName = uiState.form.firstName,
                            lastName = uiState.form.lastName,
                            onPhotoClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }

                    // Personal Information Section
                    item {
                        CollapsibleSection(
                            title = "Personal Information",
                            icon = Icons.Default.Person,
                            isExpanded = uiState.expandedSections.contains("personal"),
                            onToggle = { viewModel.toggleSection("personal") },
                            isRequired = true
                        ) {
                            PersonalInfoContent(
                                form = uiState.form,
                                validationErrors = uiState.validationErrors,
                                onFirstNameChange = viewModel::updateFirstName,
                                onMiddleNameChange = viewModel::updateMiddleName,
                                onLastNameChange = viewModel::updateLastName,
                                onMaidenNameChange = viewModel::updateMaidenName,
                                onNicknameChange = viewModel::updateNickname,
                                onGenderChange = viewModel::updateGender
                            )
                        }
                    }

                    // Life Events Section
                    item {
                        CollapsibleSection(
                            title = "Life Events",
                            icon = Icons.Default.DateRange,
                            isExpanded = uiState.expandedSections.contains("lifeEvents"),
                            onToggle = { viewModel.toggleSection("lifeEvents") }
                        ) {
                            LifeEventsContent(
                                form = uiState.form,
                                validationErrors = uiState.validationErrors,
                                birthPlaceSearchResults = uiState.birthPlaceSearchResults,
                                deathPlaceSearchResults = uiState.deathPlaceSearchResults,
                                burialPlaceSearchResults = uiState.burialPlaceSearchResults,
                                isSearchingBirthPlace = uiState.isSearchingBirthPlace,
                                isSearchingDeathPlace = uiState.isSearchingDeathPlace,
                                isSearchingBurialPlace = uiState.isSearchingBurialPlace,
                                onBirthDateClick = { showBirthDatePicker = true },
                                onBirthDateClear = { viewModel.updateBirthDate(null) },
                                onBirthPlaceChange = viewModel::updateBirthPlace,
                                onBirthPlaceSearch = viewModel::searchBirthPlace,
                                onBirthPlaceSelect = viewModel::selectBirthPlace,
                                onBirthPlaceSearchClear = viewModel::clearBirthPlaceSearch,
                                onIsLivingChange = viewModel::updateIsLiving,
                                onDeathDateClick = { showDeathDatePicker = true },
                                onDeathDateClear = { viewModel.updateDeathDate(null) },
                                onDeathPlaceChange = viewModel::updateDeathPlace,
                                onDeathPlaceSearch = viewModel::searchDeathPlace,
                                onDeathPlaceSelect = viewModel::selectDeathPlace,
                                onDeathPlaceSearchClear = viewModel::clearDeathPlaceSearch,
                                onCauseOfDeathChange = viewModel::updateCauseOfDeath,
                                onBurialPlaceChange = viewModel::updateBurialPlace,
                                onBurialPlaceSearch = viewModel::searchBurialPlaceDebounced,
                                onBurialPlaceSelect = viewModel::selectBurialPlace,
                                onBurialPlaceSearchClear = viewModel::clearBurialPlaceSearch
                            )
                        }
                    }

                    // Education & Career Section
                    item {
                        CollapsibleSection(
                            title = "Education & Career",
                            icon = Icons.Default.School,
                            isExpanded = uiState.expandedSections.contains("education"),
                            onToggle = { viewModel.toggleSection("education") }
                        ) {
                            EducationCareerContent(
                                form = uiState.form,
                                onEducationChange = viewModel::updateEducation,
                                onEducationLevelChange = viewModel::updateEducationLevel,
                                onAlmaMaterChange = viewModel::updateAlmaMater,
                                onOccupationChange = viewModel::updateOccupation,
                                onEmployerChange = viewModel::updateEmployer,
                                onCareerStatusChange = viewModel::updateCareerStatus,
                                onAddSkill = viewModel::addSkill,
                                onRemoveSkill = viewModel::removeSkill,
                                onAddAchievement = viewModel::addAchievement,
                                onRemoveAchievement = viewModel::removeAchievement,
                                onMilitaryServiceChange = viewModel::updateMilitaryService
                            )
                        }
                    }

                    // Contact Information Section
                    item {
                        CollapsibleSection(
                            title = "Contact Information",
                            icon = Icons.Default.Phone,
                            isExpanded = uiState.expandedSections.contains("contact"),
                            onToggle = { viewModel.toggleSection("contact") }
                        ) {
                            ContactInfoContent(
                                form = uiState.form,
                                validationErrors = uiState.validationErrors,
                                addressSearchResults = uiState.addressSearchResults,
                                isSearchingAddress = uiState.isSearchingAddress,
                                onPhoneChange = viewModel::updatePhone,
                                onEmailChange = viewModel::updateEmail,
                                onAddressChange = viewModel::updateAddress,
                                onAddressSearch = viewModel::searchAddressDebounced,
                                onAddressSelect = viewModel::selectAddress,
                                onAddressSearchClear = viewModel::clearAddressSearch
                            )
                        }
                    }

                    // Cultural Information Section
                    item {
                        CollapsibleSection(
                            title = "Cultural & Personal",
                            icon = Icons.Outlined.Flag,
                            isExpanded = uiState.expandedSections.contains("cultural"),
                            onToggle = { viewModel.toggleSection("cultural") }
                        ) {
                            CulturalInfoContent(
                                form = uiState.form,
                                onRelationshipStatusChange = viewModel::updateRelationshipStatus,
                                onReligionChange = viewModel::updateReligion,
                                onNationalityChange = viewModel::updateNationality,
                                onEthnicityChange = viewModel::updateEthnicity,
                                onAddLanguage = viewModel::addLanguage,
                                onRemoveLanguage = viewModel::removeLanguage,
                                onAddInterest = viewModel::addInterest,
                                onRemoveInterest = viewModel::removeInterest,
                                onAddSocialLink = { showAddSocialLinkDialog = true },
                                onRemoveSocialLink = viewModel::removeSocialLink
                            )
                        }
                    }

                    // Medical Information Section
                    item {
                        CollapsibleSection(
                            title = "Medical Information",
                            icon = Icons.Outlined.MedicalServices,
                            isExpanded = uiState.expandedSections.contains("medical"),
                            onToggle = { viewModel.toggleSection("medical") }
                        ) {
                            MedicalInfoContent(
                                form = uiState.form,
                                onBloodTypeChange = viewModel::updateBloodType,
                                onMedicalInfoChange = viewModel::updateMedicalInfo
                            )
                        }
                    }

                    // Additional Information Section
                    item {
                        CollapsibleSection(
                            title = "Additional Information",
                            icon = Icons.Outlined.Info,
                            isExpanded = uiState.expandedSections.contains("additional"),
                            onToggle = { viewModel.toggleSection("additional") }
                        ) {
                            AdditionalInfoContent(
                                form = uiState.form,
                                onBiographyChange = viewModel::updateBiography,
                                onNotesChange = viewModel::updateNotes,
                                onAddCustomField = { showAddCustomFieldDialog = true },
                                onRemoveCustomField = viewModel::removeCustomField
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            LoadingOverlay(isLoading = uiState.isSaving)
        }
    }
}

@Composable
private fun PhotoSectionCompact(
    photoPath: String?,
    firstName: String,
    lastName: String,
    onPhotoClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onPhotoClick),
                contentAlignment = Alignment.Center
            ) {
                if (photoPath != null && File(photoPath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(photoPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = stringResource(R.string.profile_add_photo),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = if (firstName.isNotBlank() || lastName.isNotBlank()) {
                        "$firstName $lastName".trim()
                    } else {
                        "New Family Member"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Tap photo to change",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isRequired: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                color = if (isExpanded) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (isRequired) {
                        Text(
                            text = "*",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalInfoContent(
    form: EditMemberFormState,
    validationErrors: Map<String, String>,
    onFirstNameChange: (String) -> Unit,
    onMiddleNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onMaidenNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = form.firstName,
            onValueChange = onFirstNameChange,
            label = { Text(stringResource(R.string.profile_first_name) + " *") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            isError = validationErrors.containsKey("firstName"),
            supportingText = validationErrors["firstName"]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = form.middleName,
            onValueChange = onMiddleNameChange,
            label = { Text("Middle") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = form.lastName,
            onValueChange = onLastNameChange,
            label = { Text(stringResource(R.string.profile_last_name)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = form.maidenName,
            onValueChange = onMaidenNameChange,
            label = { Text(stringResource(R.string.profile_maiden_name)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
    }

    OutlinedTextField(
        value = form.nickname,
        onValueChange = onNicknameChange,
        label = { Text(stringResource(R.string.profile_nickname)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        )
    )

    Column {
        Text(
            text = stringResource(R.string.profile_gender),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Gender.entries.forEach { gender ->
                FilterChip(
                    selected = form.gender == gender,
                    onClick = { onGenderChange(gender) },
                    label = {
                        Text(
                            when (gender) {
                                Gender.MALE -> stringResource(R.string.gender_male)
                                Gender.FEMALE -> stringResource(R.string.gender_female)
                                Gender.OTHER -> stringResource(R.string.gender_other)
                                Gender.UNKNOWN -> stringResource(R.string.gender_unknown)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LifeEventsContent(
    form: EditMemberFormState,
    validationErrors: Map<String, String>,
    birthPlaceSearchResults: List<GeocodedLocation>,
    deathPlaceSearchResults: List<GeocodedLocation>,
    burialPlaceSearchResults: List<GeocodedLocation>,
    isSearchingBirthPlace: Boolean,
    isSearchingDeathPlace: Boolean,
    isSearchingBurialPlace: Boolean,
    onBirthDateClick: () -> Unit,
    onBirthDateClear: () -> Unit,
    onBirthPlaceChange: (String) -> Unit,
    onBirthPlaceSearch: (String) -> Unit,
    onBirthPlaceSelect: (GeocodedLocation) -> Unit,
    onBirthPlaceSearchClear: () -> Unit,
    onIsLivingChange: (Boolean) -> Unit,
    onDeathDateClick: () -> Unit,
    onDeathDateClear: () -> Unit,
    onDeathPlaceChange: (String) -> Unit,
    onDeathPlaceSearch: (String) -> Unit,
    onDeathPlaceSelect: (GeocodedLocation) -> Unit,
    onDeathPlaceSearchClear: () -> Unit,
    onCauseOfDeathChange: (String) -> Unit,
    onBurialPlaceChange: (String) -> Unit,
    onBurialPlaceSearch: (String) -> Unit,
    onBurialPlaceSelect: (GeocodedLocation) -> Unit,
    onBurialPlaceSearchClear: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    // Birth Date
    DatePickerField(
        label = stringResource(R.string.profile_birth_date),
        date = form.birthDate,
        error = validationErrors["birthDate"],
        onClick = onBirthDateClick,
        onClear = onBirthDateClear
    )

    // Birth Place with location search
    LocationSearchField(
        value = form.birthPlace,
        onValueChange = { value ->
            onBirthPlaceChange(value)
            onBirthPlaceSearch(value)
        },
        label = stringResource(R.string.profile_birth_place),
        searchResults = birthPlaceSearchResults,
        isSearching = isSearchingBirthPlace,
        onLocationSelected = onBirthPlaceSelect,
        onDismissResults = onBirthPlaceSearchClear,
        hasCoordinates = form.birthPlaceLatitude != null
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // Is Living toggle
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
            checked = form.isLiving,
            onCheckedChange = onIsLivingChange
        )
    }

    // Death-related fields (only show if not living)
    if (!form.isLiving) {
        DatePickerField(
            label = stringResource(R.string.profile_death_date),
            date = form.deathDate,
            error = validationErrors["deathDate"],
            onClick = onDeathDateClick,
            onClear = onDeathDateClear
        )

        LocationSearchField(
            value = form.deathPlace,
            onValueChange = { value ->
                onDeathPlaceChange(value)
                onDeathPlaceSearch(value)
            },
            label = stringResource(R.string.profile_death_place),
            searchResults = deathPlaceSearchResults,
            isSearching = isSearchingDeathPlace,
            onLocationSelected = onDeathPlaceSelect,
            onDismissResults = onDeathPlaceSearchClear,
            hasCoordinates = form.deathPlaceLatitude != null
        )

        OutlinedTextField(
            value = form.causeOfDeath,
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
            value = form.burialPlace,
            onValueChange = { value ->
                onBurialPlaceChange(value)
                onBurialPlaceSearch(value)
            },
            label = "Burial Place",
            searchResults = burialPlaceSearchResults,
            isSearching = isSearchingBurialPlace,
            onLocationSelected = onBurialPlaceSelect,
            onDismissResults = onBurialPlaceSearchClear,
            hasCoordinates = form.burialLatitude != null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EducationCareerContent(
    form: EditMemberFormState,
    onEducationChange: (String) -> Unit,
    onEducationLevelChange: (EducationLevel) -> Unit,
    onAlmaMaterChange: (String) -> Unit,
    onOccupationChange: (String) -> Unit,
    onEmployerChange: (String) -> Unit,
    onCareerStatusChange: (CareerStatus) -> Unit,
    onAddSkill: (String) -> Unit,
    onRemoveSkill: (String) -> Unit,
    onAddAchievement: (String) -> Unit,
    onRemoveAchievement: (String) -> Unit,
    onMilitaryServiceChange: (String) -> Unit
) {
    // Education Level Dropdown
    StatusDropdown(
        label = "Education Level",
        selectedValue = form.educationLevel.displayName,
        options = EducationLevel.entries.map { it.displayName },
        onOptionSelected = { displayName ->
            val level = EducationLevel.entries.find { it.displayName == displayName }
                ?: EducationLevel.UNKNOWN
            onEducationLevelChange(level)
        },
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = form.education,
            onValueChange = onEducationChange,
            label = { Text("Field of Study") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = form.almaMater,
            onValueChange = onAlmaMaterChange,
            label = { Text("School/University") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // Career Status Dropdown
    StatusDropdown(
        label = "Career Status",
        selectedValue = form.careerStatus.displayName,
        options = CareerStatus.entries.map { it.displayName },
        onOptionSelected = { displayName ->
            val status = CareerStatus.entries.find { it.displayName == displayName }
                ?: CareerStatus.UNKNOWN
            onCareerStatusChange(status)
        },
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = form.occupation,
            onValueChange = onOccupationChange,
            label = { Text(stringResource(R.string.profile_occupation)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = form.employer,
            onValueChange = onEmployerChange,
            label = { Text("Employer") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
    }

    // Skills
    ChipInputField(
        label = "Skills",
        items = form.skills,
        onAddItem = onAddSkill,
        onRemoveItem = onRemoveSkill,
        placeholder = "Add skill"
    )

    // Achievements
    ChipInputField(
        label = "Achievements",
        items = form.achievements,
        onAddItem = onAddAchievement,
        onRemoveItem = onRemoveAchievement,
        placeholder = "Add achievement"
    )

    // Military Service
    OutlinedTextField(
        value = form.militaryService,
        onValueChange = onMilitaryServiceChange,
        label = { Text("Military Service") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        )
    )
}

@Composable
private fun ContactInfoContent(
    form: EditMemberFormState,
    validationErrors: Map<String, String>,
    addressSearchResults: List<GeocodedLocation>,
    isSearchingAddress: Boolean,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onAddressSearch: (String) -> Unit,
    onAddressSelect: (GeocodedLocation) -> Unit,
    onAddressSearchClear: () -> Unit
) {
    OutlinedTextField(
        value = form.phone,
        onValueChange = onPhoneChange,
        label = { Text("Phone") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        )
    )

    OutlinedTextField(
        value = form.email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        isError = validationErrors.containsKey("email"),
        supportingText = validationErrors["email"]?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )
    )

    LocationSearchField(
        value = form.address,
        onValueChange = { value ->
            onAddressChange(value)
            onAddressSearch(value)
        },
        label = "Address",
        searchResults = addressSearchResults,
        isSearching = isSearchingAddress,
        onLocationSelected = onAddressSelect,
        onDismissResults = onAddressSearchClear,
        hasCoordinates = form.addressLatitude != null
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CulturalInfoContent(
    form: EditMemberFormState,
    onRelationshipStatusChange: (RelationshipStatus) -> Unit,
    onReligionChange: (String) -> Unit,
    onNationalityChange: (String) -> Unit,
    onEthnicityChange: (String) -> Unit,
    onAddLanguage: (String) -> Unit,
    onRemoveLanguage: (String) -> Unit,
    onAddInterest: (String) -> Unit,
    onRemoveInterest: (String) -> Unit,
    onAddSocialLink: () -> Unit,
    onRemoveSocialLink: (String) -> Unit
) {
    // Relationship Status
    StatusDropdown(
        label = "Relationship Status",
        selectedValue = form.relationshipStatus.displayName,
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = form.religion,
            onValueChange = onReligionChange,
            label = { Text("Religion") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = form.nationality,
            onValueChange = onNationalityChange,
            label = { Text("Nationality") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
    }

    OutlinedTextField(
        value = form.ethnicity,
        onValueChange = onEthnicityChange,
        label = { Text("Ethnicity") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        )
    )

    // Languages
    ChipInputField(
        label = "Languages",
        items = form.languages,
        onAddItem = onAddLanguage,
        onRemoveItem = onRemoveLanguage,
        placeholder = "Add language"
    )

    // Interests
    ChipInputFieldWithPresets(
        label = "Interests & Hobbies",
        items = form.interests,
        onAddItem = onAddInterest,
        onRemoveItem = onRemoveInterest,
        placeholder = "Add interest",
        presets = listOf(
            "Reading", "Music", "Sports", "Cooking", "Travel",
            "Photography", "Gardening", "Art", "Movies", "Gaming"
        )
    )

    // Social Links
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Social Links",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onAddSocialLink) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }
        if (form.socialLinks.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                form.socialLinks.forEach { (platform, url) ->
                    InputChip(
                        selected = false,
                        onClick = { onRemoveSocialLink(platform) },
                        label = { Text("$platform: $url", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MedicalInfoContent(
    form: EditMemberFormState,
    onBloodTypeChange: (BloodType?) -> Unit,
    onMedicalInfoChange: (String) -> Unit
) {
    // Blood Type
    Column {
        Text(
            text = "Blood Type",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = form.bloodType == null,
                onClick = { onBloodTypeChange(null) },
                label = { Text("Unknown") }
            )
            BloodType.entries.forEach { bloodType ->
                FilterChip(
                    selected = form.bloodType == bloodType,
                    onClick = { onBloodTypeChange(bloodType) },
                    label = { Text(bloodType.displayName) }
                )
            }
        }
    }

    OutlinedTextField(
        value = form.medicalInfo,
        onValueChange = onMedicalInfoChange,
        label = { Text("Medical Notes") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdditionalInfoContent(
    form: EditMemberFormState,
    onBiographyChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddCustomField: () -> Unit,
    onRemoveCustomField: (String) -> Unit
) {
    OutlinedTextField(
        value = form.biography,
        onValueChange = onBiographyChange,
        label = { Text(stringResource(R.string.profile_biography)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 6,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        )
    )

    OutlinedTextField(
        value = form.notes,
        onValueChange = onNotesChange,
        label = { Text(stringResource(R.string.profile_notes)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        )
    )

    // Custom Fields
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.profile_custom_fields),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onAddCustomField) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }
        if (form.customFields.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                form.customFields.forEach { (key, value) ->
                    CustomFieldItem(
                        key = key,
                        value = value,
                        onRemove = { onRemoveCustomField(key) }
                    )
                }
            }
        }
    }
}

// Reusable Components

@Composable
private fun DatePickerField(
    label: String,
    date: Long?,
    error: String?,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    OutlinedTextField(
        value = date?.let { dateFormat.format(Date(it)) } ?: "",
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier
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
private fun DatePickerDialogComposable(
    initialDate: Long?,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    searchResults: List<GeocodedLocation>,
    isSearching: Boolean,
    onLocationSelected: (GeocodedLocation) -> Unit,
    onDismissResults: () -> Unit,
    hasCoordinates: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth()) {
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
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                supportingText = if (hasCoordinates) {
                    { Text("Location coordinates saved", color = MaterialTheme.colorScheme.primary) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
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
private fun ChipInputField(
    label: String,
    items: List<String>,
    onAddItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    placeholder: String
) {
    var newItem by remember { mutableStateOf("") }

    Column {
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
                label = { Text(placeholder) },
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
                        onAddItem(newItem)
                        newItem = ""
                    }
                },
                enabled = newItem.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipInputFieldWithPresets(
    label: String,
    items: List<String>,
    onAddItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    placeholder: String,
    presets: List<String>
) {
    var newItem by remember { mutableStateOf("") }
    var showPresets by remember { mutableStateOf(false) }

    Column {
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
                label = { Text(placeholder) },
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
                        onAddItem(newItem)
                        newItem = ""
                    }
                },
                enabled = newItem.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        TextButton(onClick = { showPresets = !showPresets }) {
            Text(if (showPresets) "Hide presets" else "Show preset options")
        }

        AnimatedVisibility(visible = showPresets) {
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
                .padding(12.dp),
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
private fun AddFieldDialog(
    title: String,
    keyLabel: String,
    valueLabel: String,
    onAdd: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(keyLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(valueLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
