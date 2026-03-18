package com.example.rapidreach.screens.dashboard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.example.rapidreach.viewmodel.OfficialService
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import com.example.rapidreach.viewmodel.SosState
import com.example.rapidreach.viewmodel.SosViewModel
import com.example.rapidreach.viewmodel.AuthViewModel

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreview() {
    MaterialTheme {
        DashboardScreen()
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToProfile: () -> Unit = {},
    onNavigateToFakeCall: () -> Unit = {},
    onNavigateToLiveShare: () -> Unit = {},
    onNavigateToHelpline: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onLogout: () -> Unit = {},
    sosViewModel: SosViewModel = viewModel()
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF650927)
    val sosState by sosViewModel.sosState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val userName = currentUser?.name ?: "User"
    val userType = currentUser?.userType ?: "User"

    // Permission states
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val callPermission = rememberPermissionState(Manifest.permission.CALL_PHONE)

    // Speech Recognizer launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (matches != null && matches.isNotEmpty()) {
                val spokenText = matches[0].lowercase()
                
                // Process speech input
                when {
                    spokenText.contains("police") -> {
                        sosViewModel.onSosConfirmed("userId", emptyList(), OfficialService.POLICE)
                        if (callPermission.status == PermissionStatus.Granted) {
                            val intent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:100")
                            }
                            context.startActivity(intent)
                        }
                    }
                    spokenText.contains("ambulance") || spokenText.contains("emergency") -> {
                        sosViewModel.onSosConfirmed("userId", emptyList(), OfficialService.AMBULANCE)
                        if (callPermission.status == PermissionStatus.Granted) {
                            val intent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:108")
                            }
                            context.startActivity(intent)
                        }
                    }
                    spokenText.contains("no") || spokenText.contains("cancel") -> {
                        sosViewModel.onSosDismissed()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "RapidReach",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF650927)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Open menu */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF650927))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color(0xFF650927))
                    }
                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF650927))
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color(0xFF650927))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFFDFDFD)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Greeting and User info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, $userName",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        text = "Category: $userType",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }


        Spacer(modifier = Modifier.height(20.dp))

        // Active Status Chip
        if (sosState is SosState.Active) {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        "SOS ACTIVE — Help is on the way",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3F5), shape = CircleShape)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SOS BUTTON with animation
        val infiniteTransition = rememberInfiniteTransition(label = "sosPulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (sosState is SosState.Active) 1.1f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500)
            ),
            label = "sosScale"
        )

        val buttonColor by infiniteTransition.animateColor(
            initialValue = if (sosState is SosState.Active) Color(0xFF8B0000) else primaryColor,
            targetValue = if (sosState is SosState.Active) Color(0xFFCB0000) else primaryColor,
            animationSpec = infiniteRepeatable(
                animation = tween(1500)
            ),
            label = "sosColor"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .shadow(20.dp, CircleShape)
                .background(buttonColor, CircleShape)
                .scale(scale)
                .clickable {
                    when (sosState) {
                        is SosState.Idle -> {
                            locationPermission.launchPermissionRequest()
                            audioPermission.launchPermissionRequest()
                            callPermission.launchPermissionRequest()
                            sosViewModel.onSosPressed()
                        }
                        is SosState.Active -> {
                            sosViewModel.onSosCancelled()
                        }
                        else -> {}
                    }
                }
        ) {
            Text(
                text = if (sosState is SosState.Active) "STOP" else "SOS",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        // ACTION BUTTONS
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            FeatureButton("Fake Call", Icons.Default.Call) {
                onNavigateToFakeCall()
            }
            FeatureButton("Share Live", Icons.Default.LocationOn) {
                onNavigateToLiveShare()
            }
            FeatureButton("Helplines", Icons.Default.Notifications) {
                onNavigateToHelpline()
            }
            FeatureButton("Nearby", Icons.Default.Warning) {
                onNavigateToMap()
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Safety Card based on user type
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F5))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    when (userType) {
                        "Student" -> "Awareness: Be cautious during late-night commutes"
                        "Elderly" -> "Tip: Keep emergency contacts updated"
                        "Child" -> "Safety: Always share your location with guardians"
                        else -> "👤 Stay safe and aware of your surroundings"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
            }
            }
        }
    }

    // Official Service Dialog with Speech Recognizer
    if (sosState is SosState.ConfirmDialog) {
        OfficialServiceDialog(
            onPolice = {
                sosViewModel.onSosConfirmed("userId", emptyList(), OfficialService.POLICE)
                if (callPermission.status == PermissionStatus.Granted) {
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:100")
                    }
                    context.startActivity(intent)
                }
            },
            onAmbulance = {
                sosViewModel.onSosConfirmed("userId", emptyList(), OfficialService.AMBULANCE)
                if (callPermission.status == PermissionStatus.Granted) {
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:108")
                    }
                    context.startActivity(intent)
                }
            },
            onContactsOnly = {
                sosViewModel.onSosConfirmed("userId", emptyList(), null)
            },
            onDismiss = {
                sosViewModel.onSosDismissed()
            },
            onStartSpeechRecognition = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'Police', 'Ambulance', or 'No'")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizerLauncher.launch(intent)
            }
        )
    }
}

@Composable
fun FeatureButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        ElevatedButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun OfficialServiceDialog(
    onPolice: () -> Unit,
    onAmbulance: () -> Unit,
    onContactsOnly: () -> Unit,
    onDismiss: () -> Unit,
    onStartSpeechRecognition: () -> Unit
) {
    val primaryColor = Color(0xFF650927)
    var isListening by remember { mutableStateOf(false) }
    var listeningText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .animateContentSize(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Would you like to alert official services?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    "Help will be dispatched to your location. Speak or select an option below.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Listening Indicator
                if (isListening) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), shape = MaterialTheme.shapes.medium)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Listening",
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "Listening... Say your choice",
                            fontSize = 12.sp,
                            color = primaryColor,
                            fontWeight = FontWeight.Medium
                        )
                        if (listeningText.isNotEmpty()) {
                            Text(
                                "You said: $listeningText",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Police Button
                Button(
                    onClick = onPolice,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1e3a8a) // Dark blue
                    )
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Police",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Yes — Alert Police")
                }

                // Ambulance Button
                Button(
                    onClick = onAmbulance,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16a34a) // Green
                    )
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Ambulance",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Yes — Alert Ambulance")
                }

                // Contacts Only Button
                OutlinedButton(
                    onClick = onContactsOnly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No — Alert Contacts Only")
                }

                // Speech Recognition Button
                Button(
                    onClick = {
                        isListening = true
                        onStartSpeechRecognition()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    )
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("🎤 Speak Your Choice")
                }

                // Cancel Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = primaryColor)
                }
            }
        }
    }
}
