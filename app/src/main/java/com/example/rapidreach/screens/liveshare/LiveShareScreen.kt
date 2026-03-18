package com.example.rapidreach.screens.liveshare

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LiveShareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF650927)
    val scope = rememberCoroutineScope()

    var latitude by remember { mutableStateOf(0f) }
    var longitude by remember { mutableStateOf(0f) }
    var isCopied by remember { mutableStateOf(false) }

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Request permission on launch
    LaunchedEffect(Unit) {
        locationPermissionState.launchPermissionRequest()
    }

    // Get current location periodically if permission is granted
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            while (true) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            latitude = location.latitude.toFloat()
                            longitude = location.longitude.toFloat()
                        }
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
                delay(5000)
            }
        }
    }

    // Pulsing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val dotColor by infiniteTransition.animateColor(
        initialValue = primaryColor,
        targetValue = Color(0xFFFF6B6B),
        animationSpec = infiniteRepeatable(
            animation = tween(1500)
        ),
        label = "dotColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Live Location Sharing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = primaryColor,
                    navigationIconContentColor = primaryColor
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Live Pulsing Indicator
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFFFF0F3), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(dotColor, CircleShape)
                        .scale(0.8f)
                )
            }

            Text(
                "LIVE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )

            // Location Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F3)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Your Location",
                        fontWeight = FontWeight.SemiBold,
                        color = primaryColor,
                        fontSize = 14.sp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, shape = MaterialTheme.shapes.medium)
                            .padding(12.dp)
                    ) {
                        Text(
                            "Latitude:  $latitude",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            "Longitude: $longitude",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Text(
                        "Updates every 5 seconds",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Copy Maps Link Button
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(
                        "location",
                        "https://maps.google.com/?q=$latitude,$longitude"
                    )
                    clipboard.setPrimaryClip(clip)
                    isCopied = true
                    scope.launch {
                        delay(2000)
                        isCopied = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                    tint = Color.White
                )
                Text(
                    if (isCopied) "✓ Copied!" else "📋 Copy Maps Link",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Share via WhatsApp Button
            Button(
                onClick = {
                    val shareText = "My live location: https://maps.google.com/?q=$latitude,$longitude"
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // WhatsApp not installed — fallback to generic share
                        val fallback = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(fallback, "Share location via"))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16a34a)),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                    tint = Color.White
                )
                Text(
                    "Share via WhatsApp",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "Your location is shared only with people you choose, until you stop sharing.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}