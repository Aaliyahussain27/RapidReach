package com.example.rapidreach.screens.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rapidreach.viewmodel.NearbyMapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

data class NearbyPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val type: String, // "police" or "hospital"
    val phoneNumber: String = "100",
    val distance: Float = 0f
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NearbyMapScreen(
    onBack: () -> Unit,
    viewModel: NearbyMapViewModel = viewModel()
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF650927)
    
    // Permission state
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status is PermissionStatus.Granted) {
            viewModel.getCurrentLocation(context)
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Collect state from ViewModel
    val currentLocation by viewModel.currentLocation.collectAsState()
    val nearbyPlaces by viewModel.nearbyPlaces.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedPlace by viewModel.selectedPlace.collectAsState()
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()

    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Handle MapView Lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Nearby Help",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF650927)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF650927)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF650927))
                    }
                    IconButton(onClick = { /* Handle */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF650927))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            AndroidView(
                factory = {
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
                        controller.setZoom(15.0)
                        
                        // User location overlay
                        val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                        myLocationOverlay.enableMyLocation()
                        myLocationOverlay.enableFollowLocation()
                        overlays.add(myLocationOverlay)
                    }
                },
                update = { map ->
                    currentLocation?.let { location ->
                        map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                    }

                    // Clear and add markers
                    val currentMarkers = map.overlays.filterIsInstance<Marker>()
                    map.overlays.removeAll(currentMarkers)

                    nearbyPlaces.forEach { place ->
                        val marker = Marker(map)
                        marker.position = GeoPoint(place.latitude, place.longitude)
                        marker.title = place.name
                        marker.subDescription = place.type.uppercase()
                        
                        // Custom color heuristic for OSMDroid
                        val icon = context.getDrawable(android.R.drawable.ic_dialog_map)
                        icon?.setTint(if (place.type == "hospital") 0xFFFF0000.toInt() else 0xFF0000FF.toInt())
                        marker.icon = icon

                        marker.setOnMarkerClickListener { m, _ ->
                            viewModel.selectPlace(place)
                            m.showInfoWindow()
                            true
                        }
                        map.overlays.add(marker)
                    }
                    map.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

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
                    onDismiss = { viewModel.closeBottomSheet() }
                )
            }
        }
    }
}

@Composable
fun NearbyPlaceBottomSheet(
    place: NearbyPlace,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = (if (place.type == "hospital") Color(0xFFFFEBEE) else Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (place.type == "hospital") Icons.Default.MedicalServices else Icons.Default.Policy,
                                contentDescription = place.type,
                                tint = if (place.type == "police") Color(0xFF1976D2) else Color(0xFFD32F2F),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            place.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            if (place.type == "hospital") "MEDICAL FACILITY" else "POLICE / SECURITY",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = Color(0xFFEEEEEE))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            place.address.ifEmpty { "Address not available" },
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    }
                    if (place.phoneNumber.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                place.phoneNumber,
                                fontSize = 14.sp,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Call Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${place.phoneNumber}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16a34a)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CALL", fontWeight = FontWeight.Bold)
                    }

                    // Navigate Button
                    Button(
                        onClick = {
                            val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${place.latitude},${place.longitude}")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GUIDE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}