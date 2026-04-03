package com.example.rapidreach.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rapidreach.screens.map.NearbyPlace
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
import java.net.URLEncoder

class NearbyMapViewModel : ViewModel() {
    private val _currentLocation = MutableStateFlow<android.location.Location?>(null)
    val currentLocation: StateFlow<android.location.Location?> = _currentLocation

    private val _nearbyPlaces = MutableStateFlow<List<NearbyPlace>>(emptyList())
    val nearbyPlaces: StateFlow<List<NearbyPlace>> = _nearbyPlaces

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _activeTab = MutableStateFlow("police")
    val activeTab: StateFlow<String> = _activeTab

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun setTab(tab: String, context: Context) {
        _activeTab.value = tab
        _currentLocation.value?.let { fetchNearbyPlaces(it.latitude, it.longitude, tab) }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        _isLoading.value = true
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        _currentLocation.value = location
                        fetchNearbyPlaces(location.latitude, location.longitude, _activeTab.value)
                    } else {
                        // FALLBACK: Use a default location (e.g., Delhi center) if emulator location is null
                        val fallbackLoc = android.location.Location("fallback").apply {
                            latitude = 28.6139
                            longitude = 77.2090
                        }
                        _currentLocation.value = fallbackLoc
                        fetchNearbyPlaces(fallbackLoc.latitude, fallbackLoc.longitude, _activeTab.value)
                        _isLoading.value = false
                    }
                }.addOnFailureListener {
                    _isLoading.value = false
                    // Also try fallback on failure
                    val fallbackLoc = android.location.Location("fallback").apply {
                        latitude = 28.6139
                        longitude = 77.2090
                    }
                    _currentLocation.value = fallbackLoc
                    fetchNearbyPlaces(fallbackLoc.latitude, fallbackLoc.longitude, _activeTab.value)
                }
        } catch (e: Exception) {
            _isLoading.value = false
        }
    }

    private fun fetchNearbyPlaces(lat: Double, lon: Double, type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val places = withContext(Dispatchers.IO) {
                    val amenity = when(type) {
                        "police" -> "police"
                        "hospital" -> "hospital"
                        "fire_station" -> "fire_station"
                        else -> "police"
                    }
                    
                    val query = when(amenity) {
                        "police" -> """
                            [out:json];
                            (
                              node["amenity"="police"](around:8000,$lat,$lon);
                              way["amenity"="police"](around:8000,$lat,$lon);
                              node["emergency"="police_station"](around:8000,$lat,$lon);
                            );
                            out center;
                        """.trimIndent()
                        "hospital" -> """
                            [out:json];
                            (
                              node["amenity"="hospital"](around:8000,$lat,$lon);
                              way["amenity"="hospital"](around:8000,$lat,$lon);
                              node["healthcare"="hospital"](around:8000,$lat,$lon);
                            );
                            out center;
                        """.trimIndent()
                        else -> """
                            [out:json];
                            (
                              node["amenity"="$amenity"](around:8000,$lat,$lon);
                              way["amenity"="$amenity"](around:8000,$lat,$lon);
                            );
                            out center;
                        """.trimIndent()
                    }

                    val url = URL("https://overpass-api.de/api/interpreter")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                    val writer = OutputStreamWriter(connection.outputStream)
                    writer.write("data=" + URLEncoder.encode(query, "UTF-8"))
                    writer.flush()

                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    connection.disconnect()

                    parsePlaces(response.toString(), lat, lon, type)
                }
                _nearbyPlaces.value = places
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parsePlaces(json: String, userLat: Double, userLon: Double, type: String): List<NearbyPlace> {
        val places = mutableListOf<NearbyPlace>()
        try {
            val root = JSONObject(json)
            val elements = root.optJSONArray("elements") ?: return emptyList()
            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                
                // For 'out center' we use 'center''s lat/lon, or default to node's 'lat'/'lon'
                val center = element.optJSONObject("center")
                val lat = center?.optDouble("lat") ?: element.optDouble("lat")
                val lon = center?.optDouble("lon") ?: element.optDouble("lon")
                
                val tags = element.optJSONObject("tags")
                val name = tags?.optString("name", "Unnamed $type") ?: "Unnamed $type"
                
                // Hardcoded filter for reported incorrect OSM data
                if (name.contains("Chaitra Cafe", ignoreCase = true) || name.contains("Cafe", ignoreCase = true)) {
                    continue
                }

                val phone = tags?.optString("phone") ?: tags?.optString("contact:phone") ?: ""
                val address = tags?.optString("addr:full") 
                    ?: (tags?.optString("addr:street")?.let { "${tags.optString("addr:housenumber", "")} $it" } ?: "Address not available")

                val distanceResults = FloatArray(1)
                android.location.Location.distanceBetween(userLat, userLon, lat, lon, distanceResults)
                
                places.add(
                    NearbyPlace(
                        name = name,
                        latitude = lat,
                        longitude = lon,
                        address = address,
                        type = type,
                        phoneNumber = if (phone.isEmpty()) (if (type == "hospital") "102" else "100") else phone,
                        distance = distanceResults[0] / 1000f
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return places.sortedBy { it.distance }
    }
}
