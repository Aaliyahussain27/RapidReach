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
            // Upload to Supabase Postgrest
            postgrest["sos_logs"]
                .insert(log)

            // Upload audio file if it exists
            if (log.audioFilePath.isNotEmpty()) {
                uploadAudioFile(log.userId, log.audioFilePath)
            }

            // Mark as synced in Room
            sosLogDao.markSynced(log.id)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getUnsyncedLogs(): List<SosLogEntity> = sosLogDao.getUnsynced()
    suspend fun markAsSynced(id: Long) = sosLogDao.markSynced(id)

    suspend fun syncPendingLogs() {
        try {
            val unsynced = getUnsyncedLogs()
            for (log in unsynced) {
                uploadAndMarkSynced(log)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun pushLiveLocation(userId: String, latitude: Double, longitude: Double) {
        try {
            postgrest["live_tracking"]
                .upsert(
                    mapOf(
                        "userId" to userId,
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "timestamp" to System.currentTimeMillis()
                    )
                ) {
                    filter {
                        eq("userId", userId)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun notifyContactsViaPush(contacts: List<EmergencyContact>, userName: String, lat: Double, lng: Double) {
        // Write an SOS alert to Supabase Postgrest
        // A webhook or edge function can trigger based on this insert to send push notifications
        val alertData = mapOf(
            "type" to "SOS_ALERT",
            "from_name" to userName,
            "latitude" to lat,
            "longitude" to lng,
            "timestamp" to System.currentTimeMillis(),
            "contacts" to contacts.map { it.phone }
        )
        try {
            postgrest["sos_alerts"].insert(alertData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
