package com.example.rapidreach.data.repository

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import com.example.rapidreach.data.local.RapidReachDatabase
import com.example.rapidreach.data.local.entity.SosLogEntity
import com.example.rapidreach.data.model.EmergencyContact
import com.example.rapidreach.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SosLogInsertDto(
    @SerialName("user_id") val userId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    @SerialName("audio_file_path") val audioFilePath: String? = null,
    @SerialName("official_service") val officialService: String? = null
)

@Serializable
data class LiveTrackingDto(
    @SerialName("user_id") val userId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

@Serializable
data class SosAlertDto(
    val type: String,
    @SerialName("from_name") val fromName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val contacts: List<String>
)

class SosRepository(private val context: Context) {
    private val postgrest = SupabaseClient.client.postgrest
    private val storage = SupabaseClient.client.storage
    private val database = RapidReachDatabase.getInstance(context)
    private val sosLogDao = database.sosLogDao()

    suspend fun saveLocalLog(
        userId: String,
        latitude: Double,
        longitude: Double,
        audioFilePath: String = "",
        officialService: String = ""
    ): Long {
        val log = SosLogEntity(
            userId = userId,
            latitude = latitude,
            longitude = longitude,
            audioFilePath = audioFilePath,
            officialService = officialService,
            synced = false
        )
        return sosLogDao.insert(log)
    }

    fun sendSmsFallback(contacts: List<EmergencyContact>, latitude: Double, longitude: Double) {
        try {
            val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            val mapsLink = "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"
            val message = "EMERGENCY: I need help! My location: $mapsLink"

            for (contact in contacts) {
                if (contact.phone.isNotBlank()) {
                    smsManager?.sendTextMessage(contact.phone, null, message, null, null)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseError", "Error uploading to sos_alerts: ${e.message}", e)
            e.printStackTrace()
        }
    }

    suspend fun uploadAudioFile(userId: String, audioFilePath: String): Result<String> {
        return try {
            val file = File(audioFilePath)
            if (audioFilePath.isEmpty() || !file.exists()) {
                return Result.failure(Exception("Audio file not found"))
            }

            val fileName = "${userId}/${file.name}"
            val bucket = storage.from("audio")
            
            // Read file bytes
            val bytes = file.readBytes()
            bucket.upload(fileName, bytes) {
                upsert = true
            }

            val downloadUrl = bucket.publicUrl(fileName)
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAndMarkSynced(log: SosLogEntity): Boolean {
        return try {
            var updatedLog = log
            
            // 1. Upload audio file if it exists and get remote URL
            if (log.audioFilePath.isNotEmpty() && !log.audioFilePath.startsWith("http")) {
                val uploadResult = uploadAudioFile(log.userId, log.audioFilePath)
                uploadResult.onSuccess { remoteUrl ->
                    updatedLog = log.copy(audioFilePath = remoteUrl)
                    // Update local DB with remote URL
                    sosLogDao.updateAudioPath(log.id, remoteUrl)
                }
            }

            // 2. Upload to Supabase Postgrest (synced = true)
            val insertData = SosLogInsertDto(
                userId = updatedLog.userId,
                latitude = updatedLog.latitude,
                longitude = updatedLog.longitude,
                timestamp = updatedLog.timestamp,
                audioFilePath = updatedLog.audioFilePath.takeIf { it.isNotBlank() },
                officialService = updatedLog.officialService.takeIf { it.isNotBlank() }
            )
            
            // Delete existing log with same timestamp to act as an Upsert
            try {
                postgrest["sos_logs"].delete {
                    filter {
                        eq("user_id", updatedLog.userId)
                        eq("timestamp", updatedLog.timestamp)
                    }
                }
            } catch (e: Exception) {
                // Ignore if it doesn't exist
            }
            
            postgrest["sos_logs"].insert(insertData)

            // 3. Mark as synced in Room
            sosLogDao.markSynced(log.id)

            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseError", "Error uploading to sos_logs: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    suspend fun updateAudioPath(id: Long, path: String) = sosLogDao.updateAudioPath(id, path)

    fun getAllLogsFlow(): kotlinx.coroutines.flow.Flow<List<SosLogEntity>> = sosLogDao.getAllLogs()

    suspend fun getUnsyncedLogs(): List<SosLogEntity> = sosLogDao.getUnsynced()
    suspend fun markAsSynced(id: Long) = sosLogDao.markSynced(id)

    suspend fun syncWithRemote(userId: String) {
        try {
            // 1. Fetch remote logs for this user
            val remoteLogs = postgrest["sos_logs"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<SosLogEntity>()

            // 2. Insert into local Room DB (Room will ignore if ID matches or replace if OnConflictStrategy.REPLACE)
            for (log in remoteLogs) {
                sosLogDao.insert(log.copy(synced = true))
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseError", "Error uploading to sos_alerts: ${e.message}", e)
            e.printStackTrace()
        }
    }

    suspend fun syncPendingLogs() {
        try {
            val unsynced = getUnsyncedLogs()
            for (log in unsynced) {
                uploadAndMarkSynced(log)
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseError", "Error uploading to sos_alerts: ${e.message}", e)
            e.printStackTrace()
        }
    }

    suspend fun pushLiveLocation(userId: String, latitude: Double, longitude: Double) {
        try {
            postgrest["live_tracking"]
                .upsert(
                    LiveTrackingDto(
                        userId = userId,
                        latitude = latitude,
                        longitude = longitude,
                        timestamp = System.currentTimeMillis()
                    )
                ) {
                    filter {
                        eq("userId", userId)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseError", "Error uploading to sos_alerts: ${e.message}", e)
            e.printStackTrace()
        }
    }

    suspend fun notifyContactsViaPush(contacts: List<EmergencyContact>, userName: String, lat: Double, lng: Double) {
        // Write an SOS alert to Supabase Postgrest
        // A webhook or edge function can trigger based on this insert to send push notifications
        val alertData = SosAlertDto(
            type = "SOS_ALERT",
            fromName = userName,
            latitude = lat,
            longitude = lng,
            timestamp = System.currentTimeMillis(),
            contacts = contacts.map { it.phone }
        )
        try {
            postgrest["sos_alerts"].insert(alertData)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseError", "Error uploading to sos_alerts: ${e.message}", e)
            e.printStackTrace()
        }
    }
}
