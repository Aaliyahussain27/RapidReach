package com.example.rapidreach.data.repository

import android.content.Context
import android.telephony.SmsManager
import com.example.rapidreach.data.local.RapidReachDatabase
import com.example.rapidreach.data.local.entity.SosLogEntity
import com.example.rapidreach.data.model.EmergencyContact
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

class SosRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
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
            val smsManager = context.getSystemService(SmsManager::class.java)
            val mapsLink = "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"
            val message = "🆘 EMERGENCY: I need help! My location: $mapsLink"

            for (contact in contacts) {
                smsManager?.sendTextMessage(contact.phone, null, message, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun uploadAudioFile(userId: String, audioFilePath: String): Result<String> {
        return try {
            if (audioFilePath.isEmpty() || !File(audioFilePath).exists()) {
                return Result.failure(Exception("Audio file not found"))
            }

            val fileName = File(audioFilePath).name
            val storageRef = storage.reference
                .child("audio")
                .child(userId)
                .child(fileName)

            val fileUri = android.net.Uri.fromFile(File(audioFilePath))
            storageRef.putFile(fileUri).await()

            val downloadUrl = storageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAndMarkSynced(log: SosLogEntity): Boolean {
        return try {
            // Upload to Firestore
            firestore.collection("sos_logs")
                .add(log)
                .await()

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

    suspend fun syncPendingLogs() {
        try {
            val unsynced = sosLogDao.getUnsynced()
            for (log in unsynced) {
                uploadAndMarkSynced(log)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun pushLiveLocation(userId: String, latitude: Double, longitude: Double) {
        try {
            firestore.collection("live_tracking")
                .document(userId)
                .set(
                    mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
