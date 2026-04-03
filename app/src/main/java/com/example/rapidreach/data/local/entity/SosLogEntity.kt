package com.example.rapidreach.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Entity(tableName = "sos_logs")
data class SosLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @SerialName("user_id") val userId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("audio_file_path") val audioFilePath: String = "",
    @SerialName("official_service") val officialService: String = "",
    @Transient val synced: Boolean = false
)
