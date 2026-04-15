package com.example.rapidreach.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.example.rapidreach.data.repository.SosRepository
import com.example.rapidreach.workers.SosSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class SosService : Service() {
    private val NOTIFICATION_ID = 123
    private val CHANNEL_ID = "sos_channel"

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String = ""

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private var locationCallback: LocationCallback? = null
    private var userId: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var officialService: String? = null

    private lateinit var sosRepository: SosRepository

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sosRepository = SosRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getStringExtra("userId") ?: ""
        latitude = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
        longitude = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
        officialService = intent?.getStringExtra("officialService")

        // Start foreground notification
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID, 
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start location tracking
        scope.launch {
            startLocationTracking()
        }

        // Start audio recording
        scope.launch {
            startAudioRecording()
        }

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val title = "🆘 SOS Active"
        val message = "Emergency services activated. Your location is being shared."

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SOS Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "SOS Emergency Service Notifications"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            4000L // Update every 4 seconds as per TASK.md
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    latitude = location.latitude
                    longitude = location.longitude
                    // Push to Supabase in background
                    scope.launch {
                        sosRepository.pushLiveLocation(userId, latitude, longitude)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            null
        )
    }

    private fun startAudioRecording() {
        try {
            val timestamp = System.currentTimeMillis()
            audioFilePath = "${filesDir}/sos_audio_${timestamp}.mp4"

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFilePath)

                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop and release MediaRecorder
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            // SAVE LOCALLY FIRST - using a separate scope that won't be immediately cancelled
            if (audioFilePath.isNotEmpty()) {
                val currentLat = latitude
                val currentLng = longitude
                val currentUserId = userId
                val currentService = officialService ?: ""
                val currentAudio = audioFilePath

                // Use GlobalScope or a dedicated background scope for the final save 
                // to ensure it survives service destruction
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    val logId = sosRepository.saveLocalLog(
                        userId = currentUserId,
                        latitude = currentLat,
                        longitude = currentLng,
                        audioFilePath = currentAudio,
                        officialService = currentService
                    )
                    
                    // Now try to upload and sync
                    sosRepository.syncPendingLogs()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Remove location updates
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
        }

        // Clean up service-specific job
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
