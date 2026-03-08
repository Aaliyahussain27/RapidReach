package com.example.rapidreach.screens.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.rapidreach.viewmodel.NearbyMapViewModel
import kotlinx.coroutines.delay

data class NearbyPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val type: String, // "police" or "hospital"
    val distance: Float = 0f
)

@Composable
fun NearbyMapScreen(
    onBack: () -> Unit,
    viewModel: com.example.rapidreach.viewmodel.NearbyMapViewModel = viewModel()
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF650927)
    
    // Collect state from ViewModel
    val currentLocation by viewModel.currentLocation.collectAsState()
    val nearbyPlaces by viewModel.nearbyPlaces.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedPlace by viewModel.selectedPlace.collectAsState()
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getCurrentLocation(context)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: LatLng(20.5937, 78.9629), // Default India center
            12f
        )
    }

    LaunchedEffect(currentLocation) {
        if (currentLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(currentLocation!!, 12f)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor)
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    "Nearby Help",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Find nearby police stations and hospitals",
                fontSize = 12.sp,
                color = Color(0xFFE8DADC),
                modifier = Modifier.padding(start = 40.dp)
            )
        }

        // Google Map
        if (currentLocation != null) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    compassEnabled = true
                )
            ) {
                // Current location marker
                currentLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "You are here",
                        snippet = "Your current location"
                    )
                }

                // Police markers (blue)
                nearbyPlaces
                    .filter { place -> place.type == "police" }
                    .forEach { place ->
                        Marker(
                            state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                            title = place.name,
                            snippet = place.address,
                            onClick = { 
                                viewModel.selectPlace(place)
                                true 
                            }
                        )
                    }

                // Hospital markers (red)
                nearbyPlaces
                    .filter { place -> place.type == "hospital" }
                    .forEach { place ->
                        Marker(
                            state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                            title = place.name,
                            snippet = place.address,
                            onClick = { 
                                viewModel.selectPlace(place)
                                true 
                            }
                        )
                    }
            }
        } else if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = primaryColor
            )
        }

        // Loading Indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = primaryColor
            )
        }

        // Bottom Sheet with Place Details
        if (showBottomSheet && selectedPlace != null) {
            NearbyPlaceBottomSheet(
                place = selectedPlace!!,
                onDismiss = { viewModel.closeBottomSheet() },
                onNavigate = {
                    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${selectedPlace!!.latitude},${selectedPlace!!.longitude}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            )
        }

        // Floating Action Button for Legend
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { viewModel.showLegend() },
                containerColor = primaryColor,
                modifier = Modifier.size(40.dp)
            ) {
                Text("?", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NearbyPlaceBottomSheet(
    place: NearbyPlace,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit
) {
    val primaryColor = Color(0xFF650927)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) { }
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (place.type == "police") 
                            Icons.Default.LocationOn 
                        else 
                            Icons.Default.LocationOn,
                        contentDescription = place.type,
                        tint = if (place.type == "police") Color.Blue else Color.Red,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            place.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            place.type.uppercase(),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                Text(
                    place.address,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                Text(
                    "Distance: ${String.format("%.2f", place.distance)} km away",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = primaryColor
                        )
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = onNavigate,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = "Navigate",
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp)
                        )
                        Text("Navigate")
                    }
                }
            }
        }
    }
}
