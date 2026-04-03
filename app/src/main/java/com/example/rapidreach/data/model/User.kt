package com.example.rapidreach.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val age: Int = 0,
    val gender: String = "", // Male, Female, Other, Prefer not to say
    @SerialName("user_type") val userType: String = "", // Student, Working Professional, Elderly, Other
    @SerialName("emergency_contacts") val emergencyContacts: List<EmergencyContact> = emptyList(),
    @SerialName("medical_info") val medicalInfo: MedicalInfo? = null,
    @SerialName("safety-preferences") val safetyPreferences: SafetyPreferences? = SafetyPreferences()
)

@Serializable
data class EmergencyContact(
    val name: String = "",
    val phone: String = "",
    val relation: String = "" // Family, Friend, Colleague
)

@Serializable
data class MedicalInfo(
    @SerialName("blood_group") val bloodGroup: String = "",
    val allergies: String = "",
    val medications: String = "",
    @SerialName("medical_conditions") val medicalConditions: String = ""
)

@Serializable
data class SafetyPreferences(
    @SerialName("auto_sos_enabled") val autoSOSEnabled: Boolean = false,
    @SerialName("location_sharing_enabled") val locationSharingEnabled: Boolean = true,
    @SerialName("offline_tracking_enabled") val offlineTrackingEnabled: Boolean = true,
    @SerialName("geofencing_enabled") val geofencingEnabled: Boolean = false,
    @SerialName("check_in_reminders") val checkInReminders: Boolean = false
)