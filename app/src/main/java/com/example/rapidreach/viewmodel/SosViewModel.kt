package com.example.rapidreach.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.example.rapidreach.data.model.EmergencyContact
import com.example.rapidreach.services.SosService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SosState {
    object Idle : SosState()
    object ConfirmDialog : SosState()
    data class Active(val isOnline: Boolean, val officialService: String? = null) : SosState()
    object Cancelled : SosState()
}

enum class OfficialService {
    POLICE, AMBULANCE
}

class SosViewModel(application: Application) : AndroidViewModel(application) {
    private val _sosState = MutableStateFlow<SosState>(SosState.Idle)
    val sosState: StateFlow<SosState> = _sosState

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    private val _isLoadingLocation = MutableStateFlow(false)
    val isLoadingLocation: StateFlow<Boolean> = _isLoadingLocation

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    fun onSosPressed() {
        _sosState.value = SosState.ConfirmDialog
    }

    fun onSosDismissed() {
        _sosState.value = SosState.Idle
    }

    @SuppressLint("MissingPermission")
    fun onSosConfirmed(
        userId: String,
        emergencyContacts: List<EmergencyContact> = emptyList(),
        officialService: OfficialService? = null
    ) {
        viewModelScope.launch {
            _isLoadingLocation.value = true

            // Get current location
            getCurrentLocationForSos { latitude, longitude ->
                viewModelScope.launch {
                    val isOnline = isNetworkAvailable()
                    val serviceName = when (officialService) {
                        OfficialService.POLICE -> "POLICE"
                        OfficialService.AMBULANCE -> "AMBULANCE"
                        null -> null
                    }

                    // Set state to Active
                    _sosState.value = SosState.Active(
                        isOnline = isOnline,
                        officialService = serviceName
                    )

                    // Start SOS Service
                    val intent = Intent(getApplication<Application>().applicationContext, SosService::class.java).apply {
                        putExtra("userId", userId)
                        putExtra("latitude", latitude)
                        putExtra("longitude", longitude)
                        putExtra("officialService", serviceName)
                    }

                    if (ContextCompat.checkSelfPermission(
                            getApplication(),
                            Manifest.permission.FOREGROUND_SERVICE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        getApplication<Application>().startForegroundService(intent)
                    }

                    // TODO: Call SosRepository.saveLocalLog()
                    // TODO: if online -> pushLiveLocation(); else -> sendSmsFallback()
                    // TODO: if officialService != null -> Intent.ACTION_CALL to tel:100 or tel:108
                    // TODO: Schedule SyncWorker.schedule()

                    _isLoadingLocation.value = false
                }
            }
        }
    }

    fun onSosCancelled() {
        _sosState.value = SosState.Idle
        val intent = Intent(getApplication<Application>().applicationContext, SosService::class.java)
        getApplication<Application>().stopService(intent)
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationForSos(callback: (Double, Double) -> Unit) {
        // Check permissions
        if (ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Fallback location if permissions not granted
            callback(20.5957, 78.9629)
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    callback(location.latitude, location.longitude)
                    _currentLocation.value = LatLng(location.latitude, location.longitude)
                } else {
                    // Fallback if location is null
                    callback(20.5957, 78.9629)
                }
            }.addOnFailureListener {
                // Fallback if location fails
                callback(20.5957, 78.9629)
            }
        } catch (e: Exception) {
            // Fallback if exception
            callback(20.5957, 78.9629)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager ?: return false

        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }
}
