package com.example.rapidreach.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.example.rapidreach.screens.map.NearbyPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

class NearbyMapViewModel : ViewModel() {
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    private val _nearbyPlaces = MutableStateFlow<List<NearbyPlace>>(emptyList())
    val nearbyPlaces: StateFlow<List<NearbyPlace>> = _nearbyPlaces

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedPlace = MutableStateFlow<NearbyPlace?>(null)
    val selectedPlace: StateFlow<NearbyPlace?> = _selectedPlace

    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet: StateFlow<Boolean> = _showBottomSheet

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Simulated nearby places database (in production, use Google Places API)
    private val mockPlaces = listOf(
        // Police Stations
        NearbyPlace(
            name = "City Police Station",
            latitude = 20.5957,
            longitude = 78.9629,
            address = "123 Main Street, City Center",
            type = "police"
        ),
        NearbyPlace(
            name = "North District Police",
            latitude = 20.6100,
            longitude = 78.9700,
            address = "456 North Avenue, North Area",
            type = "police"
        ),
        NearbyPlace(
            name = "South Police Outpost",
            latitude = 20.5750,
            longitude = 78.9500,
            address = "789 South Road, South Zone",
            type = "police"
        ),
        // Hospitals
        NearbyPlace(
            name = "City General Hospital",
            latitude = 20.5900,
            longitude = 78.9700,
            address = "100 Health Street, Medical District",
            type = "hospital"
        ),
        NearbyPlace(
            name = "Emergency Care Center",
            latitude = 20.6050,
            longitude = 78.9550,
            address = "250 Care Avenue, Hospital Row",
            type = "hospital"
        ),
        NearbyPlace(
            name = "St. Mary's Medical",
            latitude = 20.5800,
            longitude = 78.9400,
            address = "300 Mary Lane, West Hospital",
            type = "hospital"
        )
    )

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context) {
        // Check permissions
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        _isLoading.value = true
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val currentLoc = LatLng(location.latitude, location.longitude)
                    _currentLocation.value = currentLoc
                    _nearbyPlaces.value = getNearbyPlaces(currentLoc)
                }
                _isLoading.value = false
            }.addOnFailureListener {
                // Fallback to IP-based location (Mumbai, India)
                _currentLocation.value = LatLng(20.5957, 78.9629)
                _nearbyPlaces.value = getNearbyPlaces(LatLng(20.5957, 78.9629))
                _isLoading.value = false
            }
        } catch (e: Exception) {
            // Fallback location
            _currentLocation.value = LatLng(20.5957, 78.9629)
            _nearbyPlaces.value = getNearbyPlaces(LatLng(20.5957, 78.9629))
            _isLoading.value = false
        }
    }

    private fun getNearbyPlaces(userLocation: LatLng): List<NearbyPlace> {
        return mockPlaces.map { place ->
            val distance = calculateDistance(
                userLocation.latitude, userLocation.longitude,
                place.latitude, place.longitude
            ).toFloat()
            place.copy(distance = distance)
        }.sortedBy { it.distance }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun selectPlace(place: NearbyPlace) {
        _selectedPlace.value = place
        _showBottomSheet.value = true
    }

    fun closeBottomSheet() {
        _showBottomSheet.value = false
    }

    fun showLegend() {
        // In a full app, this would show a legend dialog
        // showing what the different marker colors mean
    }
}
