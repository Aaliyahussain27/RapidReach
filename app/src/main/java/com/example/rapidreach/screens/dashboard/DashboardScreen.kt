package com.example.rapidreach.screens.dashboard

import android.Manifest
import android.content.Context
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
import androidx.compose.material.icons.filled.Fingerprint
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
import com.google.accompanist.permissions.isGranted
import com.example.rapidreach.viewmodel.OfficialService
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import com.example.rapidreach.viewmodel.SosState
import com.example.rapidreach.viewmodel.SosViewModel
import com.example.rapidreach.viewmodel.AuthViewModel
import com.example.rapidreach.utils.SecurityUtils
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

fun checkBatteryOptimization(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
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
    val notificationPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val backgroundLocationPermission = rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    val smsPermission = rememberPermissionState(Manifest.permission.SEND_SMS)

    var showPinDialog by remember { mutableStateOf(false) }

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
                        val uid = currentUser?.id ?: "unknown"
                        val contacts = currentUser?.emergencyContacts ?: emptyList()
                        val name = currentUser?.name ?: "User"
                        sosViewModel.onSosConfirmed(uid, contacts, OfficialService.POLICE, name)
                        if (callPermission.status.isGranted) {
                            val intent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:100")
                            }
                            context.startActivity(intent)
                        }
                    }
                    spokenText.contains("ambulance") || spokenText.contains("emergency") -> {
                        val uid = currentUser?.id ?: "unknown"
                        val contacts = currentUser?.emergencyContacts ?: emptyList()
                        val name = currentUser?.name ?: "User"
                        sosViewModel.onSosConfirmed(uid, contacts, OfficialService.AMBULANCE, name)
                        if (callPermission.status.isGranted) {
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
            TopAppBar(
                title = {
                    Text(
                        text = "RapidReach",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF650927)
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color(0xFF650927))
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
        if (currentUser == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
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
                                    notificationPermission.launchPermissionRequest()
                                    checkBatteryOptimization(context)
                                    locationPermission.launchPermissionRequest()
                                    if (locationPermission.status.isGranted) {
                                        backgroundLocationPermission.launchPermissionRequest()
                                    }
                                    audioPermission.launchPermissionRequest()
                                    callPermission.launchPermissionRequest()
                                    smsPermission.launchPermissionRequest()
                                    sosViewModel.onSosPressed()
                                }
                                is SosState.Active -> {
                                    showPinDialog = true
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
                    val uid = currentUser?.id ?: return@OfficialServiceDialog
                    val contacts = currentUser?.emergencyContacts ?: emptyList()
                    val name = currentUser?.name ?: "User"
                    sosViewModel.onSosConfirmed(uid, contacts, OfficialService.POLICE, name)
                    if (callPermission.status.isGranted) {
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:100")
                        }
                        context.startActivity(intent)
                    }
                },
                onAmbulance = {
                    val uid = currentUser?.id ?: return@OfficialServiceDialog
                    val contacts = currentUser?.emergencyContacts ?: emptyList()
                    val name = currentUser?.name ?: "User"
                    sosViewModel.onSosConfirmed(uid, contacts, OfficialService.AMBULANCE, name)
                    if (callPermission.status.isGranted) {
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:108")
                        }
                        context.startActivity(intent)
                    }
                },
                onContactsOnly = {
                    val uid = currentUser?.id ?: return@OfficialServiceDialog
                    val contacts = currentUser?.emergencyContacts ?: emptyList()
                    val name = currentUser?.name ?: "User"
                    sosViewModel.onSosConfirmed(uid, contacts, null, name)
                    if (contacts.isNotEmpty() && callPermission.status.isGranted) {
                        val firstContact = contacts[0]
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:${firstContact.phone}")
                        }
                        context.startActivity(intent)
                    }
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

        if (showPinDialog) {
            PinEntryDialog(
                onConfirm = { enteredPin ->
                    if (SecurityUtils.verifyPin(context, enteredPin)) {
                        sosViewModel.onSosCancelled()
                        showPinDialog = false
                    } else {
                        android.widget.Toast.makeText(context, "Incorrect PIN. SOS continues.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onBiometric = {
                    showBiometricPrompt(
                        context = context,
                        onSuccess = {
                            sosViewModel.onSosCancelled()
                            showPinDialog = false
                        },
                        onError = { error ->
                            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onDismiss = { showPinDialog = false }
            )
        }
    }
}

fun showBiometricPrompt(
    context: Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val activity = context as? FragmentActivity ?: return
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Authentication failed.")
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Stop SOS")
        .setSubtitle("Confirm cancellation with biometric")
        .setNegativeButtonText("Use PIN")
        .build()

    biometricPrompt.authenticate(promptInfo)
}

@Composable
fun PinEntryDialog(
    onConfirm: (String) -> Unit,
    onBiometric: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val primaryColor = Color(0xFF650927)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Secure Stop",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )

                Text(
                    "Enter 4-digit PIN to stop the emergency alert",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMsg = null
                        }
                    },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMsg != null,
                    supportingText = {
                        if (errorMsg != null) {
                            Text(errorMsg!!, color = Color.Red)
                        }
                    }
                )

                Button(
                    onClick = {
                        if (pin.length == 4) {
                            onConfirm(pin)
                        } else {
                            errorMsg = "Enter 4-digit PIN"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Confirm Stop")
                }

                OutlinedButton(
                    onClick = onBiometric,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Biometric")
                }

                TextButton(onClick = onDismiss) {
                    Text("Keep SOS Active", color = primaryColor)
                }
            }
        }
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

                Button(
                    onClick = onPolice,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1e3a8a))
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Police", modifier = Modifier.padding(end = 8.dp))
                    Text("Yes — Alert Police")
                }

                Button(
                    onClick = onAmbulance,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16a34a))
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Ambulance", modifier = Modifier.padding(end = 8.dp))
                    Text("Yes — Alert Ambulance")
                }

                OutlinedButton(
                    onClick = onContactsOnly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No — Alert Contacts Only")
                }

                Button(
                    onClick = {
                        isListening = true
                        onStartSpeechRecognition()
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice", modifier = Modifier.padding(end = 8.dp))
                    Text("Speak Your Choice")
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = primaryColor)
                }
            }
        }
    }
}
