package com.famy.tree.domain.model

import java.util.Calendar

data class FamilyMember(
    val id: Long = 0,
    val treeId: Long,
    val firstName: String,
    val middleName: String? = null,
    val lastName: String? = null,
    val maidenName: String? = null,
    val nickname: String? = null,
    val gender: Gender = Gender.UNKNOWN,
    val photoPath: String? = null,
    val birthDate: Long? = null,
    val birthPlace: String? = null,
    val birthPlaceLatitude: Double? = null,
    val birthPlaceLongitude: Double? = null,
    val deathDate: Long? = null,
    val deathPlace: String? = null,
    val deathPlaceLatitude: Double? = null,
    val deathPlaceLongitude: Double? = null,
    val isLiving: Boolean = true,
    val biography: String? = null,
    val occupation: String? = null,
    val education: String? = null,
    val educationLevel: EducationLevel = EducationLevel.UNKNOWN,
    val almaMater: String? = null,
    val interests: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val achievements: List<String> = emptyList(),
    val careerStatus: CareerStatus = CareerStatus.UNKNOWN,
    val employer: String? = null,
    val relationshipStatus: RelationshipStatus = RelationshipStatus.UNKNOWN,
    val religion: String? = null,
    val nationality: String? = null,
    val ethnicity: String? = null,
    val languages: List<String> = emptyList(),
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val addressLatitude: Double? = null,
    val addressLongitude: Double? = null,
    val socialLinks: Map<String, String> = emptyMap(),
    val medicalInfo: String? = null,
    val bloodType: BloodType? = null,
    val causeOfDeath: String? = null,
    val burialPlace: String? = null,
    val burialLatitude: Double? = null,
    val burialLongitude: Double? = null,
    val militaryService: String? = null,
    val notes: String? = null,
    val customFields: Map<String, String> = emptyMap(),
    val generation: Int = 0,
    val paternalLine: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = buildString {
            append(firstName)
            lastName?.let { append(" $it") }
        }

    val displayName: String
        get() = nickname ?: fullName

    val age: Int?
        get() {
            val birth = birthDate ?: return null
            val end = if (isLiving) System.currentTimeMillis() else (deathDate ?: System.currentTimeMillis())
            return calculateYearsDifference(birth, end)
        }

    val lifespan: Int?
        get() {
            if (isLiving) return null
            val birth = birthDate ?: return null
            val death = deathDate ?: return null
            return calculateYearsDifference(birth, death)
        }

    private fun calculateYearsDifference(startMillis: Long, endMillis: Long): Int {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
        var years = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        if (endCal.get(Calendar.DAY_OF_YEAR) < startCal.get(Calendar.DAY_OF_YEAR)) {
            years--
        }
        return years.coerceAtLeast(0)
    }

    companion object {
        fun create(
            treeId: Long,
            firstName: String,
            lastName: String? = null,
            gender: Gender = Gender.UNKNOWN
        ): FamilyMember {
            return FamilyMember(
                treeId = treeId,
                firstName = firstName,
                lastName = lastName,
                gender = gender
            )
        }
    }
}

enum class Gender {
    MALE,
    FEMALE,
    OTHER,
    UNKNOWN;

    companion object {
        fun fromString(value: String): Gender {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class CareerStatus(val displayName: String) {
    UNKNOWN("Unknown"),
    STUDENT("Student"),
    EMPLOYED("Employed"),
    SELF_EMPLOYED("Self-Employed"),
    UNEMPLOYED("Unemployed"),
    RETIRED("Retired"),
    HOMEMAKER("Homemaker"),
    MILITARY("Military"),
    ENTREPRENEUR("Entrepreneur"),
    FREELANCER("Freelancer");

    companion object {
        fun fromString(value: String): CareerStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class RelationshipStatus(val displayName: String) {
    UNKNOWN("Unknown"),
    SINGLE("Single"),
    IN_RELATIONSHIP("In a Relationship"),
    ENGAGED("Engaged"),
    MARRIED("Married"),
    DOMESTIC_PARTNERSHIP("Domestic Partnership"),
    SEPARATED("Separated"),
    DIVORCED("Divorced"),
    WIDOWED("Widowed");

    companion object {
        fun fromString(value: String): RelationshipStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class EducationLevel(val displayName: String) {
    UNKNOWN("Unknown"),
    NO_FORMAL_EDUCATION("No Formal Education"),
    PRIMARY_SCHOOL("Primary School"),
    MIDDLE_SCHOOL("Middle School"),
    HIGH_SCHOOL("High School"),
    VOCATIONAL_TRAINING("Vocational Training"),
    SOME_COLLEGE("Some College"),
    ASSOCIATE_DEGREE("Associate Degree"),
    BACHELOR_DEGREE("Bachelor's Degree"),
    MASTER_DEGREE("Master's Degree"),
    DOCTORATE("Doctorate / PhD"),
    PROFESSIONAL_DEGREE("Professional Degree"),
    POST_DOCTORAL("Post-Doctoral");

    companion object {
        fun fromString(value: String): EducationLevel {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class BloodType(val displayName: String) {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    companion object {
        fun fromString(value: String): BloodType? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
