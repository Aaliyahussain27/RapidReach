package com.example.rapidreach.screens.helpline

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rapidreach.data.local.entity.CustomHelplineEntity
import com.example.rapidreach.data.model.User
import com.example.rapidreach.viewmodel.HelplineViewModel
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
    val isStatic: Boolean = false,
    val isCustom: Boolean = false,
    val originalEntity: CustomHelplineEntity? = null
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HelplineScreen(
    currentUser: User?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF650927)
    val backgroundColor = Color(0xFFFDFDFD)
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val viewModel: HelplineViewModel = viewModel()
    val customHelplinesDb by viewModel.customHelplines.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newHelplineName by remember { mutableStateOf("") }
    var newHelplineNumber by remember { mutableStateOf("") }

    var nearbyHelplines by remember { mutableStateOf<List<Helpline>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

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
                                val query = when(type) {
                                    "police" -> "[out:json][timeout:25];(nwr[\"amenity\"=\"police\"](around:8000,${location.latitude},${location.longitude});nwr[\"emergency\"=\"police_station\"](around:8000,${location.latitude},${location.longitude}););out center;"
                                    "hospital" -> "[out:json][timeout:25];(nwr[\"amenity\"=\"hospital\"](around:8000,${location.latitude},${location.longitude});nwr[\"healthcare\"=\"hospital\"](around:8000,${location.latitude},${location.longitude}););out center;"
                                    else -> "[out:json][timeout:25];nwr[\"amenity\"=\"$type\"](around:8000,${location.latitude},${location.longitude});out center;"
                                }
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
                                            if (name.contains("Cafe", ignoreCase = true)) continue
                                            val center = el.optJSONObject("center")
                                            val elLat = center?.optDouble("lat") ?: el.optDouble("lat")
                                            val elLon = center?.optDouble("lon") ?: el.optDouble("lon")
                                            if (elLat.isNaN() || elLon.isNaN()) continue
                                            val distResults = FloatArray(1)
                                            android.location.Location.distanceBetween(location.latitude, location.longitude, elLat, elLon, distResults)
                                            result.add(Helpline(name, phone, category, tags.optString("addr:street", ""), distResults[0] / 1000f))
                                        }
                                    }
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                        result
                    }
                    nearbyHelplines = fetchedList.sortedBy { it.distance }
                }
            } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
        } else { locationPermissionState.launchPermissionRequest() }
    }

    val customHelplines = customHelplinesDb.map { Helpline(it.name, it.number, "PERSONAL", isCustom = true, originalEntity = it) }
    val allHelplines = staticHelplines + customHelplines + nearbyHelplines
    val filteredHelplines = allHelplines.filter { (selectedCategory == "ALL" || it.category == selectedCategory) && (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)) }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Column(modifier = Modifier.background(Color.White).statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = primaryColor) }
                    Text("Safety Directory", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = primaryColor)
                }
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).shadow(2.dp, RoundedCornerShape(12.dp)),
                    placeholder = { Text("Search help...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                
                val categories = listOf("ALL", "MEDICAL", "POLICE", "WOMEN", "CHILD", "FIRE", "PERSONAL")
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            modifier = Modifier.clickable { selectedCategory = category },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.05f),
                            border = if (isSelected) null else BorderStroke(1.dp, primaryColor.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.White else primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = primaryColor, contentColor = Color.White, shape = CircleShape) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = primaryColor)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(filteredHelplines) { helpline ->
                    HelplineCard(
                        helpline = helpline,
                        onCall = { context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${helpline.number}") }) },
                        onDelete = if (helpline.isCustom) { { helpline.originalEntity?.let { viewModel.deleteCustomHelpline(it) } } } else null
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Personal Contact") },
            text = {
                Column {
                    OutlinedTextField(value = newHelplineName, onValueChange = { newHelplineName = it }, label = { Text("Name") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newHelplineNumber, onValueChange = { newHelplineNumber = it }, label = { Text("Number") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newHelplineName.isNotBlank() && newHelplineNumber.isNotBlank()) {
                        viewModel.addCustomHelpline(newHelplineName, newHelplineNumber)
                        showAddDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(primaryColor)) { Text("Add") }
            }
        )
    }
}

@Composable
fun HelplineCard(helpline: Helpline, onCall: () -> Unit, onDelete: (() -> Unit)? = null) {
    val primaryColor = Color(0xFF650927)
    val (icon, color, bgColor) = when (helpline.category) {
        "MEDICAL" -> Triple(Icons.Default.MedicalServices, Color(0xFFD32F2F), Color(0xFFFFEBEE))
        "POLICE" -> Triple(Icons.Default.Policy, Color(0xFF1976D2), Color(0xFFE3F2FD))
        "FIRE" -> Triple(Icons.Default.LocalFireDepartment, Color(0xFFF57C00), Color(0xFFFFF3E0))
        "WOMEN" -> Triple(Icons.Default.Face, Color(0xFFC2185B), Color(0xFFFCE4EC))
        "CHILD" -> Triple(Icons.Default.Face, Color(0xFF7B1FA2), Color(0xFFF3E5F5))
        else -> Triple(Icons.Default.Call, Color(0xFF455A64), Color(0xFFECEFF1))
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onCall() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).background(bgColor, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(helpline.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(helpline.number, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = primaryColor)
                if (helpline.distance > 0) Text(String.format("%.1f km away", helpline.distance), fontSize = 11.sp, color = Color.Gray)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray) }
            }
            IconButton(onClick = onCall, modifier = Modifier.background(primaryColor.copy(alpha = 0.05f), CircleShape)) {
                Icon(Icons.Default.Call, contentDescription = null, tint = primaryColor)
            }
        }
    }
}
