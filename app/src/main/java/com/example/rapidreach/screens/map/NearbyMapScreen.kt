package com.example.rapidreach.screens.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rapidreach.viewmodel.NearbyMapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.example.rapidreach.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

data class NearbyPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val type: String,
    val phoneNumber: String = "",
    val distance: Float = 0f
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NearbyMapScreen(
    onBack: () -> Unit,
    viewModel: NearbyMapViewModel = viewModel()
) {
    val context = LocalContext.current
    val primaryColor = PrimaryMaroon
    
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status is PermissionStatus.Granted) {
            viewModel.getCurrentLocation(context)
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    val nearbyPlaces by viewModel.nearbyPlaces.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    
    var selectedPlace by remember { mutableStateOf<NearbyPlace?>(null) }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(it.latitude, it.longitude), 15f
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = true
                ),
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionState.status is PermissionStatus.Granted
                )
            ) {
                nearbyPlaces.forEach { place ->
                    Marker(
                        state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                        title = place.name,
                        snippet = "Tap for details",
                        onClick = {
                            selectedPlace = place
                            false
                        }
                    )
                }
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(44.dp)
                    .background(Color.White, CircleShape)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 64.dp) 
            ) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        CategoryChip(
                            label = "Hospitals",
                            icon = Icons.Default.LocalHospital,
                            isSelected = activeTab == "hospital",
                            color = PrimaryMaroon
                        ) { viewModel.setTab("hospital", context) }
                    }
                    
                    item {
                        CategoryChip(
                            label = "Police",
                            icon = Icons.Default.Policy,
                            isSelected = activeTab == "police",
                            color = PoliceBlue
                        ) { viewModel.setTab("police", context) }
                    }
                    
                    item {
                        CategoryChip(
                            label = "Fire",
                            icon = Icons.Default.LocalFireDepartment,
                            isSelected = activeTab == "fire_station",
                            color = ErrorRed
                        ) { viewModel.setTab("fire_station", context) }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = selectedPlace != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
            ) {
                selectedPlace?.let { place ->
                    PlaceDetailCard(place) {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${place.phoneNumber}")
                        }
                        context.startActivity(intent)
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = primaryColor
                )
            } else if (nearbyPlaces.isEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 40.dp)
                ) {
                    Text(
                        "No emergency services found nearby. Try switching categories or checking your connection.",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    color: Color,
    contentColor: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color else Color.White,
        contentColor = if (isSelected) contentColor else Color.Black,
        shadowElevation = 6.dp,
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
        }
    }
}

@Composable
fun PlaceDetailCard(place: NearbyPlace, onCall: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = SuccessGreen,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "NEAREST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = SuccessDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = place.name.lowercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = place.address,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                        )
                    }
                }
                
                // Wait Time Badge
                Surface(
                    color = NeutralVariant,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("WAIT TIME", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
                        Text("8m", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DeepNavy, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoBadge(Icons.Default.MedicalServices, "DEPARTMENT", "24/7 ER", Modifier.weight(1f))
                InfoBadge(Icons.Default.Directions, "TRAFFIC", "Light", Modifier.weight(1f))
            }

            Button(
                onClick = onCall,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                shape = RoundedCornerShape(30.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Call Now", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
                }
            }
        }
    }
}

@Composable
fun InfoBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier) {
    Surface(
        color = BackgroundWhite,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
            Column {
                Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
            }
        }
    }
}