package com.example.rapidreach.screens.helpline

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.rapidreach.data.model.User
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Helpline(
    val name: String,
    val number: String,
    val category: String,
    val address: String = "",
    val distance: Float = 0f,
    val isStatic: Boolean = false
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HelplineScreen(
    currentUser: User?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF4A0E0E) // Darker maroon from image
    val backgroundColor = Color(0xFFFFFBFA) // Light cream background
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var nearbyHelplines by remember { mutableStateOf<List<Helpline>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Static National Helplines
    val staticHelplines = listOf(
        Helpline("Ambulance", "108", "MEDICAL", isStatic = true),
        Helpline("Police", "112", "POLICE", isStatic = true),
        Helpline("Fire", "101", "FIRE", isStatic = true),
        Helpline("Women Safety", "181", "WOMEN", isStatic = true),
        Helpline("Child Helpline", "1098", "CHILD", isStatic = true)
    )

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status is PermissionStatus.Granted) {
            isLoading = true
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                
                if (location != null) {
                    val fetchedList = withContext(Dispatchers.IO) {
                        val result = mutableListOf<Helpline>()
                        val types = listOf("police" to "POLICE", "hospital" to "MEDICAL", "fire_station" to "FIRE")
                        
                        for (typeEntry in types) {
                            val type = typeEntry.first
                            val category = typeEntry.second
                            
                            try {
                                val query = """
                                    [out:json];
                                    node["amenity"="$type"](around:5000,${location.latitude},${location.longitude});
                                    out body;
                                """.trimIndent()

                                val url = URL("https://overpass-api.de/api/interpreter")
                                val conn = url.openConnection() as HttpURLConnection
                                conn.requestMethod = "POST"
                                conn.doOutput = true
                                
                                val writer = OutputStreamWriter(conn.outputStream)
                                writer.write("data=" + URLEncoder.encode(query, "UTF-8"))
                                writer.flush()

                                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                                val response = reader.readText()
                                reader.close()
                                
                                val jsonMessage = JSONObject(response)
                                val elements = jsonMessage.optJSONArray("elements")
                                if (elements != null) {
                                    for (i in 0 until minOf(elements.length(), 3)) {
                                        val el = elements.getJSONObject(i)
                                        val tags = el.optJSONObject("tags") ?: continue
                                        val phone = tags.optString("phone") ?: tags.optString("contact:phone") ?: ""
                                        
                                        if (phone.isNotEmpty()) {
                                            val name = tags.optString("name", "Nearby $category")
                                            // Hardcoded filter for "Chaitra Cafe" reported by user
                                            if (name.contains("Chaitra Cafe", ignoreCase = true) || name.contains("Cafe", ignoreCase = true)) {
                                                continue
                                            }
                                            
                                            val elLat = el.getDouble("lat")
                                            val elLon = el.getDouble("lon")
                                            val distResults = FloatArray(1)
                                            android.location.Location.distanceBetween(location.latitude, location.longitude, elLat, elLon, distResults)
                                            
                                            result.add(Helpline(
                                                name = name,
                                                number = phone,
                                                category = category,
                                                address = tags.optString("addr:street", ""),
                                                distance = distResults[0] / 1000f
                                            ))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        result
                    }
                    nearbyHelplines = fetchedList.sortedBy { it.distance }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Combine and Filter
    val allHelplines = staticHelplines + nearbyHelplines
    val filteredHelplines = allHelplines.filter { 
        (selectedCategory == "ALL" || it.category == selectedCategory) &&
        (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Column(
                modifier = Modifier
                    .background(backgroundColor)
                    .statusBarsPadding()
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search helplines...", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFEEEEEE),
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                
                // Category Chips
                val categories = listOf("ALL", "MEDICAL", "POLICE", "WOMEN", "CHILD", "FIRE")
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            modifier = Modifier.clickable { selectedCategory = category },
                            shape = CircleShape,
                            color = if (isSelected) primaryColor else Color.Transparent,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = primaryColor)
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(filteredHelplines) { helpline ->
                    HelplineCard(helpline) {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${helpline.number}")
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
fun HelplineCard(
    helpline: Helpline,
    onCall: () -> Unit
) {
    val iconColor: Color
    val iconBgColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector

    when (helpline.category) {
        "MEDICAL" -> {
            icon = Icons.Default.MedicalServices
            iconColor = Color(0xFFB71C1C)
            iconBgColor = Color(0xFFFFF0F0)
        }
        "POLICE" -> {
            icon = Icons.Default.Policy
            iconColor = Color(0xFF0D47A1)
            iconBgColor = Color(0xFFF0F4FF)
        }
        "FIRE" -> {
            icon = Icons.Default.LocalFireDepartment
            iconColor = Color(0xFFE65100)
            iconBgColor = Color(0xFFFFF3E0)
        }
        "WOMEN" -> {
            icon = Icons.Default.Face // Standard icon
            iconColor = Color(0xFFA18800)
            iconBgColor = Color(0xFFFFFDE7)
        }
        "CHILD" -> {
            icon = Icons.Default.Face // Standard icon
            iconColor = Color(0xFF880E4F)
            iconBgColor = Color(0xFFFCE4EC)
        }
        else -> {
            icon = Icons.Default.Call
            iconColor = Color.Gray
            iconBgColor = Color(0xFFF5F5F5)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(Color.White, shape = RoundedCornerShape(24.dp))
            .clickable { onCall() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconBgColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Name and Number
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = helpline.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                Text(
                    text = helpline.number,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A0E0E)
                )
                if (!helpline.isStatic && helpline.distance > 0) {
                    Text(
                        String.format("%.1f km away", helpline.distance),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Call Button
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
                onClick = onCall
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = Color(0xFF4A0E0E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
