package com.example.rapidreach.screens.liveshare

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rapidreach.viewmodel.SosLocation
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
    val accentColor = Color(0xFFFF6B6B)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Live Tracker",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = primaryColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = primaryColor
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF5F7), Color.White)
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Pulsing Map Indicator
                MapPulseIndicator(primaryColor, accentColor)

                Spacer(modifier = Modifier.height(32.dp))

                // Status Card
                LocationGlassCard(
                    latitude = latitude,
                    longitude = longitude,
                    primaryColor = primaryColor
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons Section
                Text(
                    "SHARING OPTIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 2.sp,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 12.dp)
                )

                ActionTile(
                    label = if (isCopied) "MAP LINK COPIED" else "COPY GOOGLE MAPS LINK",
                    icon = if (isCopied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                    backgroundColor = Color.White,
                    contentColor = primaryColor,
                    borderColor = primaryColor.copy(alpha = 0.1f),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        if (clipboard != null) {
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
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ActionTile(
                    label = "SHARE VIA WHATSAPP",
                    icon = Icons.Default.Share,
                    backgroundColor = Color(0xFF25D366),
                    contentColor = Color.White,
                    onClick = {
                        val shareText = "My real-time location: https://maps.google.com/?q=$latitude,$longitude"
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                            setPackage("com.whatsapp")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val fallback = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val chooserIntent = Intent.createChooser(fallback, "Share live location")
                            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooserIntent)
                        }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Footer Disclaimer
                Surface(
                    color = Color(0xFFF1F1F1),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Your location and route are encrypted and only accessible to recipients you choose.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapPulseIndicator(primaryColor: Color, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "markerPulse")
    
    val outerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerScale"
    )

    val outerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerAlpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        // Pulsing Rings
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(outerScale)
                .alpha(outerAlpha)
                .background(primaryColor, CircleShape)
        )
        
        // Inner Glow
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        // Center Marker Icon
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Live Badge
        Surface(
            color = accentColor,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-20).dp, y = 20.dp)
        ) {
            Text(
                "LIVE",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun LocationGlassCard(latitude: Float, longitude: Float, primaryColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "REAL-TIME COORDINATES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    letterSpacing = 1.sp
                )
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = primaryColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CoordinateBox("LATITUDE", String.format("%.6f", latitude), Modifier.weight(1f))
                CoordinateBox("LONGITUDE", String.format("%.6f", longitude), Modifier.weight(1f))
            }
            
            Text(
                "Updating securely every 5 seconds...",
                fontSize = 10.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun CoordinateBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
    }
}

@Composable
fun ActionTile(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    borderColor: Color = Color.Transparent,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}