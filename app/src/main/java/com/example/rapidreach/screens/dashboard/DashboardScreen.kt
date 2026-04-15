package com.example.rapidreach.screens.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.*
import com.example.rapidreach.viewmodel.*
import com.example.rapidreach.utils.SecurityUtils
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToProfile: () -> Unit = {},
    onNavigateToFakeCall: () -> Unit = {},
    onNavigateToLiveShare: () -> Unit = {},
    onNavigateToHelpline: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToRecordings: () -> Unit = {},
    onLogout: () -> Unit = {},
    sosViewModel: SosViewModel = viewModel()
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF650927)
    val secondaryColor = Color(0xFF4A0E0E)
    val lightBg = Color(0xFFFCF7F8)
    
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

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (matches != null && matches.isNotEmpty()) {
                val spokenText = matches[0].lowercase()
                when {
                    spokenText.contains("police") -> {
                        val uid = currentUser?.id ?: "unknown"
                        val contacts = currentUser?.emergencyContacts ?: emptyList()
                        val name = currentUser?.name ?: "User"
                        sosViewModel.onSosConfirmed(uid, contacts, OfficialService.POLICE, name)
                        if (callPermission.status.isGranted) {
                            context.startActivity(Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:100") })
                        }
                    }
                    spokenText.contains("ambulance") -> {
                        val uid = currentUser?.id ?: "unknown"
                        val contacts = currentUser?.emergencyContacts ?: emptyList()
                        val name = currentUser?.name ?: "User"
                        sosViewModel.onSosConfirmed(uid, contacts, OfficialService.AMBULANCE, name)
                        if (callPermission.status.isGranted) {
                            context.startActivity(Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:108") })
                        }
                    }
                    spokenText.contains("no") -> sosViewModel.onSosDismissed()
                }
            }
        }
    }

    Scaffold(
        containerColor = lightBg,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RapidReach", fontSize = 26.sp, fontWeight = FontWeight.Black, color = primaryColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToProfile, modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.05f), CircleShape)) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = onLogout, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = primaryColor)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (currentUser == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = primaryColor) }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)
            ) {
                // Header Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Welcome back,", fontSize = 14.sp, color = Color.Gray)
                            Text(userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        
                        Surface(
                            shape = CircleShape,
                            color = if (sosState is SosState.Active) Color(0xFFFFEBEE) else Color(0xFFF1F8E9)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(if (sosState is SosState.Active) Color.Red else Color(0xFF2E7D32), CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (sosState is SosState.Active) "SOS" else "SECURE",
                                    fontSize = 11.sp, fontWeight = FontWeight.Black,
                                    color = if (sosState is SosState.Active) Color.Red else Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // SOS Button Area
                Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing Rings
                    if (sosState is SosState.Active) {
                        repeat(3) { i ->
                            val infiniteTransition = rememberInfiniteTransition(label = "")
                            val scale by infiniteTransition.animateFloat(1f, 1.8f + (i*0.2f), infiniteRepeatable(tween(2000, delayMillis = i*500), RepeatMode.Restart), label = "")
                            val alpha by infiniteTransition.animateFloat(0.3f, 0f, infiniteRepeatable(tween(2000, delayMillis = i*500), RepeatMode.Restart), label = "")
                            Box(Modifier.size(180.dp).scale(scale).background(primaryColor.copy(alpha = alpha), CircleShape))
                        }
                    }

                    // The SOS Button
                    Surface(
                        modifier = Modifier.size(200.dp).clickable {
                            when (sosState) {
                                is SosState.Idle -> {
                                    notificationPermission.launchPermissionRequest()
                                    locationPermission.launchPermissionRequest()
                                    if (locationPermission.status.isGranted) backgroundLocationPermission.launchPermissionRequest()
                                    audioPermission.launchPermissionRequest()
                                    callPermission.launchPermissionRequest()
                                    smsPermission.launchPermissionRequest()
                                    sosViewModel.onSosPressed()
                                }
                                is SosState.Active -> showPinDialog = true
                                else -> {}
                            }
                        },
                        shape = CircleShape,
                        color = Color.Transparent,
                        shadowElevation = 10.dp
                    ) {
                        Box(
                            modifier = Modifier.background(Brush.linearGradient(listOf(primaryColor, secondaryColor)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (sosState is SosState.Active) "STOP" else "SOS",
                                    fontSize = 50.sp, fontWeight = FontWeight.Black, color = Color.White
                                )
                                Text(
                                    if (sosState is SosState.Active) "Tap to cancel" else "Hold 2s for Alert",
                                    fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Utilities
                Text("Safety Dashboard", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FeatureCard("Fake Call", Icons.Default.Call, Color(0xFF673AB7), Modifier.weight(1f), onNavigateToFakeCall)
                    FeatureCard("Live Share", Icons.Default.LocationOn, Color(0xFF009688), Modifier.weight(1f), onNavigateToLiveShare)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FeatureCard("Helplines", Icons.Default.Notifications, Color(0xFFF44336), Modifier.weight(1f), onNavigateToHelpline)
                    FeatureCard("Nearby", Icons.Default.Warning, Color(0xFFFF9800), Modifier.weight(1f), onNavigateToMap)
                }
                
                Spacer(modifier = Modifier.height(30.dp))

                // Safety Library Section (Moved back to its own clean card)
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToRecordings() },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Safety Library", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                            Text("Playback SOS recordings", fontSize = 12.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Dialogs...
        if (sosState is SosState.ConfirmDialog) {
            OfficialServiceDialog(
                onPolice = {
                    val uid = currentUser?.id ?: return@OfficialServiceDialog
                    val contacts = currentUser?.emergencyContacts ?: emptyList()
                    val name = currentUser?.name ?: "User"
                    sosViewModel.onSosConfirmed(uid, contacts, OfficialService.POLICE, name)
                    if (callPermission.status.isGranted) context.startActivity(Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:100") })
                },
                onAmbulance = {
                    val uid = currentUser?.id ?: return@OfficialServiceDialog
                    val contacts = currentUser?.emergencyContacts ?: emptyList()
                    val name = currentUser?.name ?: "User"
                    sosViewModel.onSosConfirmed(uid, contacts, OfficialService.AMBULANCE, name)
                    if (callPermission.status.isGranted) context.startActivity(Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:108") })
                },
                onContactsOnly = {
                    val uid = currentUser?.id ?: return@OfficialServiceDialog
                    val contacts = currentUser?.emergencyContacts ?: emptyList()
                    val name = currentUser?.name ?: "User"
                    sosViewModel.onSosConfirmed(uid, contacts, null, name)
                    if (contacts.isNotEmpty() && callPermission.status.isGranted) {
                        context.startActivity(Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:${contacts[0].phone}") })
                    }
                },
                onDismiss = { sosViewModel.onSosDismissed() },
                onStartSpeechRecognition = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'Police' or 'Ambulance'")
                    }
                    speechRecognizerLauncher.launch(intent)
                }
            )
        }

        if (showPinDialog) {
            PinEntryDialog(
                onConfirm = { pin ->
                    if (SecurityUtils.verifyPin(context, pin)) { sosViewModel.onSosCancelled(); showPinDialog = false }
                    else Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                },
                onBiometric = {
                    showBiometricPrompt(context, { sosViewModel.onSosCancelled(); showPinDialog = false }, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() })
                },
                onDismiss = { showPinDialog = false }
            )
        }
    }
}

@Composable
fun FeatureCard(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(110.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(32.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

// Keep helper functions for dialogs/biometric but ensure they are correctly defined
fun showBiometricPrompt(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val activity = context as? FragmentActivity ?: return
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { super.onAuthenticationSucceeded(result); onSuccess() }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { super.onAuthenticationError(errorCode, errString); onError(errString.toString()) }
    })
    val promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle("Stop SOS").setSubtitle("Confirm with fingerprint").setNegativeButtonText("Use PIN").build()
    biometricPrompt.authenticate(promptInfo)
}

@Composable
fun PinEntryDialog(onConfirm: (String) -> Unit, onBiometric: () -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Secure Stop", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = pin, onValueChange = { if (it.length <= 4) pin = it }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                Button(onClick = { if (pin.length == 4) onConfirm(pin) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF650927))) { Text("Verify PIN") }
                TextButton(onClick = onBiometric) { Text("Use Biometric") }
            }
        }
    }
}

@Composable
fun OfficialServiceDialog(onPolice: () -> Unit, onAmbulance: () -> Unit, onContactsOnly: () -> Unit, onDismiss: () -> Unit, onStartSpeechRecognition: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Emergency Alert!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onPolice, modifier = Modifier.fillMaxWidth()) { Text("Police") }
                Button(onClick = onAmbulance, modifier = Modifier.fillMaxWidth()) { Text("Ambulance") }
                OutlinedButton(onClick = onContactsOnly, modifier = Modifier.fillMaxWidth()) { Text("Contacts Only") }
                IconButton(onClick = onStartSpeechRecognition, modifier = Modifier.align(Alignment.CenterHorizontally)) { Icon(Icons.Default.Mic, null) }
            }
        }
    }
}

fun checkBatteryOptimization(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
        context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:${context.packageName}") })
    }
}
