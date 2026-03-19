package com.example.rapidreach.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.example.rapidreach.screens.map.NearbyPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class NearbyMapViewModel : ViewModel() {
    private val _currentLocation = MutableStateFlow<android.location.Location?>(null)
    val currentLocation: StateFlow<android.location.Location?> = _currentLocation

    private val _nearbyPlaces = MutableStateFlow<List<NearbyPlace>>(emptyList())
    val nearbyPlaces: StateFlow<List<NearbyPlace>> = _nearbyPlaces

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedPlace = MutableStateFlow<NearbyPlace?>(null)
    val selectedPlace: StateFlow<NearbyPlace?> = _selectedPlace

    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet: StateFlow<Boolean> = _showBottomSheet

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context) {
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
                    _currentLocation.value = location
                    fetchNearbyPlaces(location.latitude, location.longitude)
                }
                _isLoading.value = false
            }.addOnFailureListener {
                _isLoading.value = false
            }
        } catch (e: Exception) {
            _isLoading.value = false
        }
    }

    private fun fetchNearbyPlaces(lat: Double, lon: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val places = withContext(Dispatchers.IO) {
                    val query = """
                        [out:json];
                        (
                          node["amenity"="hospital"](around:2000,$lat,$lon);
                          node["amenity"="police"](around:2000,$lat,$lon);
                        );
                        out body;
                    """.trimIndent()

                    val url = URL("https://overpass-api.de/api/interpreter")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                    val writer = OutputStreamWriter(connection.outputStream)
                    writer.write("data=" + java.net.URLEncoder.encode(query, "UTF-8"))
                    writer.flush()

                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    connection.disconnect()

                    parsePlaces(response.toString())
                }
                _nearbyPlaces.value = places
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parsePlaces(json: String): List<NearbyPlace> {
        val places = mutableListOf<NearbyPlace>()
        try {
            val root = JSONObject(json)
            val elements = root.getJSONArray("elements")
            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                val lat = element.getDouble("lat")
                val lon = element.getDouble("lon")
                val tags = element.optJSONObject("tags")
                val name = tags?.optString("name", "Unknown") ?: "Unknown"
                val amenity = tags?.optString("amenity", "Unknown") ?: "Unknown"
                val phone = tags?.optString("phone") ?: tags?.optString("contact:phone") ?: ""

                places.add(
                    NearbyPlace(
                        name = name,
                        latitude = lat,
                        longitude = lon,
                        address = tags?.optString("addr:full") ?: tags?.optString("addr:street") ?: "",
                        type = if (amenity == "hospital") "hospital" else "police",
                        phoneNumber = if (phone.isEmpty()) (if (amenity == "hospital") "102" else "100") else phone
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return places
    }

    fun selectPlace(place: NearbyPlace) {
        _selectedPlace.value = place
        _showBottomSheet.value = true
    }

    fun closeBottomSheet() {
        _showBottomSheet.value = false
    }
}
