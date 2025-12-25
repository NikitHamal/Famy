package com.famy.tree.ui.screen.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.BloodType
import com.famy.tree.domain.model.CareerStatus
import com.famy.tree.domain.model.EducationLevel
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.GeocodedLocation
import com.famy.tree.domain.model.LocationResult
import com.famy.tree.domain.model.RelationshipStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.service.LocationService
import com.famy.tree.domain.repository.FamilyTreeRepository
import com.famy.tree.domain.usecase.CreateMemberUseCase
import com.famy.tree.domain.usecase.UpdateMemberUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

data class EditMemberFormState(
    // Personal Info
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val maidenName: String = "",
    val nickname: String = "",
    val gender: Gender = Gender.UNKNOWN,
    val photoPath: String? = null,

    // Life Events
    val birthDate: Long? = null,
    val birthPlace: String = "",
    val birthPlaceLatitude: Double? = null,
    val birthPlaceLongitude: Double? = null,
    val isLiving: Boolean = true,
    val deathDate: Long? = null,
    val deathPlace: String = "",
    val deathPlaceLatitude: Double? = null,
    val deathPlaceLongitude: Double? = null,
    val causeOfDeath: String = "",
    val burialPlace: String = "",
    val burialLatitude: Double? = null,
    val burialLongitude: Double? = null,

    // Education & Career
    val education: String = "",
    val educationLevel: EducationLevel = EducationLevel.UNKNOWN,
    val almaMater: String = "",
    val occupation: String = "",
    val employer: String = "",
    val careerStatus: CareerStatus = CareerStatus.UNKNOWN,
    val skills: List<String> = emptyList(),
    val achievements: List<String> = emptyList(),
    val militaryService: String = "",

    // Contact Info
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val addressLatitude: Double? = null,
    val addressLongitude: Double? = null,

    // Cultural Info
    val relationshipStatus: RelationshipStatus = RelationshipStatus.UNKNOWN,
    val religion: String = "",
    val nationality: String = "",
    val ethnicity: String = "",
    val languages: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val socialLinks: MutableMap<String, String> = mutableMapOf(),

    // Medical Info
    val bloodType: BloodType? = null,
    val medicalInfo: String = "",

    // Additional
    val biography: String = "",
    val notes: String = "",
    val customFields: MutableMap<String, String> = mutableMapOf()
)

data class EditMemberUiState(
    val isEditing: Boolean = false,
    val form: EditMemberFormState = EditMemberFormState(),
    val originalMember: FamilyMember? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasChanges: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    // Location search states
    val birthPlaceSearchResults: List<GeocodedLocation> = emptyList(),
    val deathPlaceSearchResults: List<GeocodedLocation> = emptyList(),
    val addressSearchResults: List<GeocodedLocation> = emptyList(),
    val burialPlaceSearchResults: List<GeocodedLocation> = emptyList(),
    val isSearchingBirthPlace: Boolean = false,
    val isSearchingDeathPlace: Boolean = false,
    val isSearchingAddress: Boolean = false,
    val isSearchingBurialPlace: Boolean = false,
    // Expanded sections for UI
    val expandedSections: Set<String> = setOf("personal", "lifeEvents")
)

@HiltViewModel
class EditMemberViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: FamilyMemberRepository,
    private val treeRepository: FamilyTreeRepository,
    private val createMemberUseCase: CreateMemberUseCase,
    private val updateMemberUseCase: UpdateMemberUseCase,
    private val locationService: LocationService
) : ViewModel() {

    companion object {
        private const val DEBOUNCE_DELAY = 400L
    }

    private val treeId: Long = savedStateHandle.get<Long>("treeId") ?: 0L
    private val memberId: Long? = savedStateHandle.get<Long>("memberId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(EditMemberUiState(isEditing = memberId != null))
    val uiState: StateFlow<EditMemberUiState> = _uiState.asStateFlow()

    init {
        loadMember()
    }

    private fun loadMember() {
        viewModelScope.launch {
            if (memberId != null) {
                val member = memberRepository.getMember(memberId)
                if (member != null) {
                    _uiState.update {
                        it.copy(
                            form = EditMemberFormState(
                                // Personal Info
                                firstName = member.firstName,
                                middleName = member.middleName ?: "",
                                lastName = member.lastName ?: "",
                                maidenName = member.maidenName ?: "",
                                nickname = member.nickname ?: "",
                                gender = member.gender,
                                photoPath = member.photoPath,
                                // Life Events
                                birthDate = member.birthDate,
                                birthPlace = member.birthPlace ?: "",
                                birthPlaceLatitude = member.birthPlaceLatitude,
                                birthPlaceLongitude = member.birthPlaceLongitude,
                                isLiving = member.isLiving,
                                deathDate = member.deathDate,
                                deathPlace = member.deathPlace ?: "",
                                deathPlaceLatitude = member.deathPlaceLatitude,
                                deathPlaceLongitude = member.deathPlaceLongitude,
                                causeOfDeath = member.causeOfDeath ?: "",
                                burialPlace = member.burialPlace ?: "",
                                burialLatitude = member.burialLatitude,
                                burialLongitude = member.burialLongitude,
                                // Education & Career
                                education = member.education ?: "",
                                educationLevel = member.educationLevel,
                                almaMater = member.almaMater ?: "",
                                occupation = member.occupation ?: "",
                                employer = member.employer ?: "",
                                careerStatus = member.careerStatus,
                                skills = member.skills,
                                achievements = member.achievements,
                                militaryService = member.militaryService ?: "",
                                // Contact Info
                                phone = member.phone ?: "",
                                email = member.email ?: "",
                                address = member.address ?: "",
                                addressLatitude = member.addressLatitude,
                                addressLongitude = member.addressLongitude,
                                // Cultural Info
                                relationshipStatus = member.relationshipStatus,
                                religion = member.religion ?: "",
                                nationality = member.nationality ?: "",
                                ethnicity = member.ethnicity ?: "",
                                languages = member.languages,
                                interests = member.interests,
                                socialLinks = member.socialLinks.toMutableMap(),
                                // Medical Info
                                bloodType = member.bloodType,
                                medicalInfo = member.medicalInfo ?: "",
                                // Additional
                                biography = member.biography ?: "",
                                notes = member.notes ?: "",
                                customFields = member.customFields.toMutableMap()
                            ),
                            originalMember = member,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Member not found", isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateFirstName(value: String) {
        _uiState.update {
            it.copy(
                form = it.form.copy(firstName = value),
                hasChanges = true,
                validationErrors = it.validationErrors - "firstName"
            )
        }
    }

    fun updateMiddleName(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(middleName = value), hasChanges = true)
        }
    }

    fun updateLastName(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(lastName = value), hasChanges = true)
        }
    }

    fun updateMaidenName(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(maidenName = value), hasChanges = true)
        }
    }

    fun updateNickname(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(nickname = value), hasChanges = true)
        }
    }

    fun updateGender(value: Gender) {
        _uiState.update {
            it.copy(form = it.form.copy(gender = value), hasChanges = true)
        }
    }

    fun updateBirthDate(value: Long?) {
        _uiState.update {
            val newForm = it.form.copy(birthDate = value)
            val errors = it.validationErrors.toMutableMap()

            if (value != null && value > System.currentTimeMillis()) {
                errors["birthDate"] = "Birth date cannot be in the future"
            } else {
                errors.remove("birthDate")
            }

            if (value != null && newForm.deathDate != null && value > newForm.deathDate) {
                errors["birthDate"] = "Birth date cannot be after death date"
            }

            it.copy(form = newForm, hasChanges = true, validationErrors = errors)
        }
    }

    fun updateBirthPlace(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(birthPlace = value), hasChanges = true)
        }
    }

    fun updateDeathDate(value: Long?) {
        _uiState.update {
            val newForm = it.form.copy(deathDate = value, isLiving = value == null)
            val errors = it.validationErrors.toMutableMap()

            if (value != null && newForm.birthDate != null && value < newForm.birthDate) {
                errors["deathDate"] = "Death date cannot be before birth date"
            } else {
                errors.remove("deathDate")
            }

            it.copy(form = newForm, hasChanges = true, validationErrors = errors)
        }
    }

    fun updateDeathPlace(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(deathPlace = value), hasChanges = true)
        }
    }

    fun updateIsLiving(value: Boolean) {
        _uiState.update {
            val newForm = if (value) {
                it.form.copy(isLiving = true, deathDate = null, deathPlace = "")
            } else {
                it.form.copy(isLiving = false)
            }
            it.copy(form = newForm, hasChanges = true)
        }
    }

    fun updateBiography(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(biography = value), hasChanges = true)
        }
    }

    fun updateOccupation(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(occupation = value), hasChanges = true)
        }
    }

    fun updateEducation(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(education = value), hasChanges = true)
        }
    }

    fun updateInterests(interests: List<String>) {
        _uiState.update {
            it.copy(form = it.form.copy(interests = interests), hasChanges = true)
        }
    }

    fun addInterest(interest: String) {
        val trimmed = interest.trim()
        if (trimmed.isNotBlank()) {
            _uiState.update {
                val newInterests = it.form.interests + trimmed
                it.copy(form = it.form.copy(interests = newInterests.distinct()), hasChanges = true)
            }
        }
    }

    fun removeInterest(interest: String) {
        _uiState.update {
            val newInterests = it.form.interests - interest
            it.copy(form = it.form.copy(interests = newInterests), hasChanges = true)
        }
    }

    fun updateCareerStatus(status: CareerStatus) {
        _uiState.update {
            it.copy(form = it.form.copy(careerStatus = status), hasChanges = true)
        }
    }

    fun updateRelationshipStatus(status: RelationshipStatus) {
        _uiState.update {
            it.copy(form = it.form.copy(relationshipStatus = status), hasChanges = true)
        }
    }

    fun searchBirthPlace(query: String) {
        if (query.length < 3) {
            _uiState.update { it.copy(birthPlaceSearchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingBirthPlace = true) }
            when (val result = locationService.searchLocations(query, 5)) {
                is LocationResult.Success -> {
                    _uiState.update {
                        it.copy(birthPlaceSearchResults = result.data, isSearchingBirthPlace = false)
                    }
                }
                is LocationResult.Error -> {
                    _uiState.update { it.copy(isSearchingBirthPlace = false) }
                }
                is LocationResult.Loading -> {
                    // Loading state is already handled by isSearchingBirthPlace = true above
                }
            }
        }
    }

    fun selectBirthPlace(location: GeocodedLocation) {
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    birthPlace = location.displayName,
                    birthPlaceLatitude = location.latitude,
                    birthPlaceLongitude = location.longitude
                ),
                birthPlaceSearchResults = emptyList(),
                hasChanges = true
            )
        }
    }

    fun clearBirthPlaceSearch() {
        _uiState.update { it.copy(birthPlaceSearchResults = emptyList()) }
    }

    fun searchDeathPlace(query: String) {
        if (query.length < 3) {
            _uiState.update { it.copy(deathPlaceSearchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingDeathPlace = true) }
            when (val result = locationService.searchLocations(query, 5)) {
                is LocationResult.Success -> {
                    _uiState.update {
                        it.copy(deathPlaceSearchResults = result.data, isSearchingDeathPlace = false)
                    }
                }
                is LocationResult.Error -> {
                    _uiState.update { it.copy(isSearchingDeathPlace = false) }
                }
                is LocationResult.Loading -> {
                    // Loading state is already handled by isSearchingDeathPlace = true above
                }
            }
        }
    }

    fun selectDeathPlace(location: GeocodedLocation) {
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    deathPlace = location.displayName,
                    deathPlaceLatitude = location.latitude,
                    deathPlaceLongitude = location.longitude
                ),
                deathPlaceSearchResults = emptyList(),
                hasChanges = true
            )
        }
    }

    fun clearDeathPlaceSearch() {
        _uiState.update { it.copy(deathPlaceSearchResults = emptyList()) }
    }

    // Cause of death and burial
    fun updateCauseOfDeath(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(causeOfDeath = value), hasChanges = true)
        }
    }

    fun updateBurialPlace(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(burialPlace = value), hasChanges = true)
        }
    }

    private var burialPlaceSearchJob: Job? = null

    fun searchBurialPlaceDebounced(query: String) {
        burialPlaceSearchJob?.cancel()
        if (query.length < 3) {
            _uiState.update { it.copy(burialPlaceSearchResults = emptyList()) }
            return
        }
        burialPlaceSearchJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY)
            _uiState.update { it.copy(isSearchingBurialPlace = true) }
            when (val result = locationService.searchLocations(query, 5)) {
                is LocationResult.Success -> {
                    _uiState.update {
                        it.copy(burialPlaceSearchResults = result.data, isSearchingBurialPlace = false)
                    }
                }
                is LocationResult.Error -> {
                    _uiState.update { it.copy(isSearchingBurialPlace = false) }
                }
                is LocationResult.Loading -> {}
            }
        }
    }

    fun selectBurialPlace(location: GeocodedLocation) {
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    burialPlace = location.displayName,
                    burialLatitude = location.latitude,
                    burialLongitude = location.longitude
                ),
                burialPlaceSearchResults = emptyList(),
                hasChanges = true
            )
        }
    }

    fun clearBurialPlaceSearch() {
        _uiState.update { it.copy(burialPlaceSearchResults = emptyList()) }
    }

    // Education & Career
    fun updateEducationLevel(level: EducationLevel) {
        _uiState.update {
            it.copy(form = it.form.copy(educationLevel = level), hasChanges = true)
        }
    }

    fun updateAlmaMater(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(almaMater = value), hasChanges = true)
        }
    }

    fun updateEmployer(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(employer = value), hasChanges = true)
        }
    }

    fun updateMilitaryService(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(militaryService = value), hasChanges = true)
        }
    }

    fun updateSkills(skills: List<String>) {
        _uiState.update {
            it.copy(form = it.form.copy(skills = skills), hasChanges = true)
        }
    }

    fun addSkill(skill: String) {
        val trimmed = skill.trim()
        if (trimmed.isNotBlank()) {
            _uiState.update {
                val newSkills = it.form.skills + trimmed
                it.copy(form = it.form.copy(skills = newSkills.distinct()), hasChanges = true)
            }
        }
    }

    fun removeSkill(skill: String) {
        _uiState.update {
            val newSkills = it.form.skills - skill
            it.copy(form = it.form.copy(skills = newSkills), hasChanges = true)
        }
    }

    fun updateAchievements(achievements: List<String>) {
        _uiState.update {
            it.copy(form = it.form.copy(achievements = achievements), hasChanges = true)
        }
    }

    fun addAchievement(achievement: String) {
        val trimmed = achievement.trim()
        if (trimmed.isNotBlank()) {
            _uiState.update {
                val newAchievements = it.form.achievements + trimmed
                it.copy(form = it.form.copy(achievements = newAchievements.distinct()), hasChanges = true)
            }
        }
    }

    fun removeAchievement(achievement: String) {
        _uiState.update {
            val newAchievements = it.form.achievements - achievement
            it.copy(form = it.form.copy(achievements = newAchievements), hasChanges = true)
        }
    }

    // Contact Info
    fun updatePhone(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(phone = value), hasChanges = true)
        }
    }

    fun updateEmail(value: String) {
        _uiState.update {
            val errors = it.validationErrors.toMutableMap()
            if (value.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                errors["email"] = "Invalid email format"
            } else {
                errors.remove("email")
            }
            it.copy(
                form = it.form.copy(email = value),
                hasChanges = true,
                validationErrors = errors
            )
        }
    }

    fun updateAddress(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(address = value), hasChanges = true)
        }
    }

    private var addressSearchJob: Job? = null

    fun searchAddressDebounced(query: String) {
        addressSearchJob?.cancel()
        if (query.length < 3) {
            _uiState.update { it.copy(addressSearchResults = emptyList()) }
            return
        }
        addressSearchJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY)
            _uiState.update { it.copy(isSearchingAddress = true) }
            when (val result = locationService.searchLocations(query, 5)) {
                is LocationResult.Success -> {
                    _uiState.update {
                        it.copy(addressSearchResults = result.data, isSearchingAddress = false)
                    }
                }
                is LocationResult.Error -> {
                    _uiState.update { it.copy(isSearchingAddress = false) }
                }
                is LocationResult.Loading -> {}
            }
        }
    }

    fun selectAddress(location: GeocodedLocation) {
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    address = location.displayName,
                    addressLatitude = location.latitude,
                    addressLongitude = location.longitude
                ),
                addressSearchResults = emptyList(),
                hasChanges = true
            )
        }
    }

    fun clearAddressSearch() {
        _uiState.update { it.copy(addressSearchResults = emptyList()) }
    }

    // Cultural Info
    fun updateReligion(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(religion = value), hasChanges = true)
        }
    }

    fun updateNationality(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(nationality = value), hasChanges = true)
        }
    }

    fun updateEthnicity(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(ethnicity = value), hasChanges = true)
        }
    }

    fun updateLanguages(languages: List<String>) {
        _uiState.update {
            it.copy(form = it.form.copy(languages = languages), hasChanges = true)
        }
    }

    fun addLanguage(language: String) {
        val trimmed = language.trim()
        if (trimmed.isNotBlank()) {
            _uiState.update {
                val newLanguages = it.form.languages + trimmed
                it.copy(form = it.form.copy(languages = newLanguages.distinct()), hasChanges = true)
            }
        }
    }

    fun removeLanguage(language: String) {
        _uiState.update {
            val newLanguages = it.form.languages - language
            it.copy(form = it.form.copy(languages = newLanguages), hasChanges = true)
        }
    }

    fun addSocialLink(platform: String, url: String) {
        _uiState.update {
            val links = it.form.socialLinks.toMutableMap()
            links[platform] = url
            it.copy(
                form = it.form.copy(socialLinks = links),
                hasChanges = true
            )
        }
    }

    fun removeSocialLink(platform: String) {
        _uiState.update {
            val links = it.form.socialLinks.toMutableMap()
            links.remove(platform)
            it.copy(
                form = it.form.copy(socialLinks = links),
                hasChanges = true
            )
        }
    }

    // Medical Info
    fun updateBloodType(bloodType: BloodType?) {
        _uiState.update {
            it.copy(form = it.form.copy(bloodType = bloodType), hasChanges = true)
        }
    }

    fun updateMedicalInfo(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(medicalInfo = value), hasChanges = true)
        }
    }

    // Notes
    fun updateNotes(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(notes = value), hasChanges = true)
        }
    }

    // Section management
    fun toggleSection(sectionId: String) {
        _uiState.update {
            val expanded = it.expandedSections.toMutableSet()
            if (expanded.contains(sectionId)) {
                expanded.remove(sectionId)
            } else {
                expanded.add(sectionId)
            }
            it.copy(expandedSections = expanded)
        }
    }

    fun updatePhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val photoDir = File(context.filesDir, "photos")
                if (!photoDir.exists()) photoDir.mkdirs()

                val tempId = memberId ?: System.currentTimeMillis()
                val photoFile = File(photoDir, "${tempId}_${UUID.randomUUID()}.jpg")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(photoFile).use { output ->
                        input.copyTo(output)
                    }
                }

                _uiState.update {
                    it.copy(
                        form = it.form.copy(photoPath = photoFile.absolutePath),
                        hasChanges = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save photo: ${e.message}") }
            }
        }
    }

    fun addCustomField(key: String, value: String) {
        _uiState.update {
            val fields = it.form.customFields.toMutableMap()
            fields[key] = value
            it.copy(
                form = it.form.copy(customFields = fields),
                hasChanges = true
            )
        }
    }

    fun removeCustomField(key: String) {
        _uiState.update {
            val fields = it.form.customFields.toMutableMap()
            fields.remove(key)
            it.copy(
                form = it.form.copy(customFields = fields),
                hasChanges = true
            )
        }
    }

    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()
        val form = _uiState.value.form

        if (form.firstName.isBlank()) {
            errors["firstName"] = "First name is required"
        }

        if (form.birthDate != null && form.birthDate > System.currentTimeMillis()) {
            errors["birthDate"] = "Birth date cannot be in the future"
        }

        if (form.birthDate != null && form.deathDate != null && form.deathDate < form.birthDate) {
            errors["deathDate"] = "Death date cannot be before birth date"
        }

        _uiState.update { it.copy(validationErrors = errors) }
        return errors.isEmpty()
    }

    fun save(onSaved: (Long) -> Unit) {
        if (!validate()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val form = _uiState.value.form

                if (memberId != null) {
                    val originalMember = _uiState.value.originalMember!!
                    val updatedMember = originalMember.copy(
                        // Personal Info
                        firstName = form.firstName.trim(),
                        middleName = form.middleName.trim().takeIf { it.isNotEmpty() },
                        lastName = form.lastName.trim().takeIf { it.isNotEmpty() },
                        maidenName = form.maidenName.trim().takeIf { it.isNotEmpty() },
                        nickname = form.nickname.trim().takeIf { it.isNotEmpty() },
                        gender = form.gender,
                        photoPath = form.photoPath,
                        // Life Events
                        birthDate = form.birthDate,
                        birthPlace = form.birthPlace.trim().takeIf { it.isNotEmpty() },
                        birthPlaceLatitude = form.birthPlaceLatitude,
                        birthPlaceLongitude = form.birthPlaceLongitude,
                        isLiving = form.isLiving,
                        deathDate = form.deathDate,
                        deathPlace = form.deathPlace.trim().takeIf { it.isNotEmpty() },
                        deathPlaceLatitude = form.deathPlaceLatitude,
                        deathPlaceLongitude = form.deathPlaceLongitude,
                        causeOfDeath = form.causeOfDeath.trim().takeIf { it.isNotEmpty() },
                        burialPlace = form.burialPlace.trim().takeIf { it.isNotEmpty() },
                        burialLatitude = form.burialLatitude,
                        burialLongitude = form.burialLongitude,
                        // Education & Career
                        education = form.education.trim().takeIf { it.isNotEmpty() },
                        educationLevel = form.educationLevel,
                        almaMater = form.almaMater.trim().takeIf { it.isNotEmpty() },
                        occupation = form.occupation.trim().takeIf { it.isNotEmpty() },
                        employer = form.employer.trim().takeIf { it.isNotEmpty() },
                        careerStatus = form.careerStatus,
                        skills = form.skills,
                        achievements = form.achievements,
                        militaryService = form.militaryService.trim().takeIf { it.isNotEmpty() },
                        // Contact Info
                        phone = form.phone.trim().takeIf { it.isNotEmpty() },
                        email = form.email.trim().takeIf { it.isNotEmpty() },
                        address = form.address.trim().takeIf { it.isNotEmpty() },
                        addressLatitude = form.addressLatitude,
                        addressLongitude = form.addressLongitude,
                        // Cultural Info
                        relationshipStatus = form.relationshipStatus,
                        religion = form.religion.trim().takeIf { it.isNotEmpty() },
                        nationality = form.nationality.trim().takeIf { it.isNotEmpty() },
                        ethnicity = form.ethnicity.trim().takeIf { it.isNotEmpty() },
                        languages = form.languages,
                        interests = form.interests,
                        socialLinks = form.socialLinks.filterValues { it.isNotBlank() },
                        // Medical Info
                        bloodType = form.bloodType,
                        medicalInfo = form.medicalInfo.trim().takeIf { it.isNotEmpty() },
                        // Additional
                        biography = form.biography.trim().takeIf { it.isNotEmpty() },
                        notes = form.notes.trim().takeIf { it.isNotEmpty() },
                        customFields = form.customFields.filterValues { it.isNotBlank() },
                        updatedAt = System.currentTimeMillis()
                    )
                    updateMemberUseCase(updatedMember)
                    onSaved(memberId)
                } else {
                    val memberCount = memberRepository.observeMemberCount(treeId).first()
                    val setAsRoot = memberCount == 0

                    val newMember = FamilyMember(
                        treeId = treeId,
                        // Personal Info
                        firstName = form.firstName.trim(),
                        middleName = form.middleName.trim().takeIf { it.isNotEmpty() },
                        lastName = form.lastName.trim().takeIf { it.isNotEmpty() },
                        maidenName = form.maidenName.trim().takeIf { it.isNotEmpty() },
                        nickname = form.nickname.trim().takeIf { it.isNotEmpty() },
                        gender = form.gender,
                        photoPath = form.photoPath,
                        // Life Events
                        birthDate = form.birthDate,
                        birthPlace = form.birthPlace.trim().takeIf { it.isNotEmpty() },
                        birthPlaceLatitude = form.birthPlaceLatitude,
                        birthPlaceLongitude = form.birthPlaceLongitude,
                        isLiving = form.isLiving,
                        deathDate = form.deathDate,
                        deathPlace = form.deathPlace.trim().takeIf { it.isNotEmpty() },
                        deathPlaceLatitude = form.deathPlaceLatitude,
                        deathPlaceLongitude = form.deathPlaceLongitude,
                        causeOfDeath = form.causeOfDeath.trim().takeIf { it.isNotEmpty() },
                        burialPlace = form.burialPlace.trim().takeIf { it.isNotEmpty() },
                        burialLatitude = form.burialLatitude,
                        burialLongitude = form.burialLongitude,
                        // Education & Career
                        education = form.education.trim().takeIf { it.isNotEmpty() },
                        educationLevel = form.educationLevel,
                        almaMater = form.almaMater.trim().takeIf { it.isNotEmpty() },
                        occupation = form.occupation.trim().takeIf { it.isNotEmpty() },
                        employer = form.employer.trim().takeIf { it.isNotEmpty() },
                        careerStatus = form.careerStatus,
                        skills = form.skills,
                        achievements = form.achievements,
                        militaryService = form.militaryService.trim().takeIf { it.isNotEmpty() },
                        // Contact Info
                        phone = form.phone.trim().takeIf { it.isNotEmpty() },
                        email = form.email.trim().takeIf { it.isNotEmpty() },
                        address = form.address.trim().takeIf { it.isNotEmpty() },
                        addressLatitude = form.addressLatitude,
                        addressLongitude = form.addressLongitude,
                        // Cultural Info
                        relationshipStatus = form.relationshipStatus,
                        religion = form.religion.trim().takeIf { it.isNotEmpty() },
                        nationality = form.nationality.trim().takeIf { it.isNotEmpty() },
                        ethnicity = form.ethnicity.trim().takeIf { it.isNotEmpty() },
                        languages = form.languages,
                        interests = form.interests,
                        socialLinks = form.socialLinks.filterValues { it.isNotBlank() },
                        // Medical Info
                        bloodType = form.bloodType,
                        medicalInfo = form.medicalInfo.trim().takeIf { it.isNotEmpty() },
                        // Additional
                        biography = form.biography.trim().takeIf { it.isNotEmpty() },
                        notes = form.notes.trim().takeIf { it.isNotEmpty() },
                        customFields = form.customFields.filterValues { it.isNotBlank() }
                    )

                    val createdMember = createMemberUseCase(newMember, setAsRoot)
                    onSaved(createdMember.id)
                }

                _uiState.update { it.copy(isSaving = false, hasChanges = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to save member", isSaving = false)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
