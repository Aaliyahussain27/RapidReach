package com.example.rapidreach.data.model

import java.io.Serializable

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val age: Int = 0,
    val gender: String = "", // Male, Female, Other, Prefer not to say
    val userType: String = "", // Student, Working Professional, Elderly, Other
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val medicalInfo: MedicalInfo? = null,
    val safetyPreferences: SafetyPreferences = SafetyPreferences()
) : Serializable

data class EmergencyContact(
    val name: String = "",
    val phone: String = "",
    val relation: String = "" // Family, Friend, Colleague
) : Serializable

data class MedicalInfo(
    val bloodGroup: String = "",
    val allergies: String = "",
    val medications: String = "",
    val medicalConditions: String = ""
) : Serializable

data class SafetyPreferences(
    val autoSOSEnabled: Boolean = false,
    val locationSharingEnabled: Boolean = true,
    val offlineTrackingEnabled: Boolean = true,
    val geofencingEnabled: Boolean = false,
    val checkInReminders: Boolean = false
) : Serializable