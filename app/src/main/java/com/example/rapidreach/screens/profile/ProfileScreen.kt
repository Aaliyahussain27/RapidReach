@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.rapidreach.screens.profile

import androidx.compose.foundation.background
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rapidreach.data.model.EmergencyContact
import com.example.rapidreach.data.model.User
import com.example.rapidreach.viewmodel.ProfileViewModel
import com.example.rapidreach.utils.SecurityUtils
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.animation.AnimatedVisibility

@Composable
fun ProfileScreen(
    currentUser: User?,
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val primaryColor = Color(0xFF650927)
    val isLoading by viewModel.isLoading.collectAsState()
    var editingContacts by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var localEmergencyContacts by remember { mutableStateOf(currentUser?.emergencyContacts ?: emptyList()) }

    val displayUser = currentUser ?: User()
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("RapidReachPrefs", Context.MODE_PRIVATE)
    
    var customAudioUri by remember { 
        mutableStateOf(sharedPrefs.getString("custom_fake_call_uri", null)) 
    }
    var customAudioName by remember { 
        mutableStateOf(sharedPrefs.getString("custom_fake_call_name", "Default TTS Message")) 
    }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                // Take persistable permission to access file after reboot
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                
                // Get filename
                var fileName = "Custom Audio"
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                customAudioUri = it.toString()
                customAudioName = fileName
                sharedPrefs.edit()
                    .putString("custom_fake_call_uri", it.toString())
                    .putString("custom_fake_call_name", fileName)
                    .apply()
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Profile",
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            if (editingContacts && localEmergencyContacts.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingContacts = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.updateEmergencyContacts(currentUser!!.copy(
                                    emergencyContacts = localEmergencyContacts
                                ))
                                editingContacts = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Edit profile button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { editingContacts = !editingContacts }) {
                        Icon(
                            if (editingContacts) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (editingContacts) "Done" else "Edit Contacts")
                    }
                }

                // Avatar and Name Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = primaryColor
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = Color.White,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }

                    Text(
                        displayUser.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Text(
                        displayUser.userType,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                HorizontalDivider()

                // User Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Personal Information",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        InfoRow("Email", displayUser.email)
                        InfoRow("Phone", displayUser.phone)
                        InfoRow("Gender", displayUser.gender)
                        InfoRow("Age", displayUser.age.toString())

                        if (displayUser.medicalInfo != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                "Medical Information",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            InfoRow("Blood Group", displayUser.medicalInfo!!.bloodGroup)
                            InfoRow("Allergies", displayUser.medicalInfo!!.allergies)
                            InfoRow("Medications", displayUser.medicalInfo!!.medications)
                        }
                    }
                }

                // Emergency Contacts Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val contactCount = localEmergencyContacts.size
                            Text(
                                "Emergency Contacts ($contactCount/5)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            if (editingContacts && localEmergencyContacts.size < 5) {
                                IconButton(
                                    onClick = { showAddContactDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Contact",
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (localEmergencyContacts.isEmpty()) {
                            Text(
                                "No emergency contacts added yet",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        } else {
                            localEmergencyContacts.forEachIndexed { index, contact ->
                                EmergencyContactCard(
                                    contact = contact,
                                    onDelete = {
                                        if (editingContacts) {
                                            localEmergencyContacts = localEmergencyContacts.toMutableList().apply {
                                                removeAt(index)
                                            }
                                        }
                                    },
                                    isEditing = editingContacts
                                )
                            }
                        }
                    }
                }

                // Fake Call Customization Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Fake Call Customization",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Text(
                            "Choose the audio message to play when you answer a fake call.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Current Message",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    customAudioName ?: "Default Message",
                                    fontSize = 12.sp,
                                    color = primaryColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Row {
                                if (customAudioUri != null) {
                                    IconButton(
                                        onClick = {
                                            customAudioUri = null
                                            customAudioName = "Default TTS Message"
                                            sharedPrefs.edit()
                                                .remove("custom_fake_call_uri")
                                                .remove("custom_fake_call_name")
                                                .apply()
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Reset", tint = Color.Gray)
                                    }
                                }
                                Button(
                                    onClick = { audioLauncher.launch(arrayOf("audio/*")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text("Pick Audio", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Security PIN Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Emergency Security",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        
                        val context = LocalContext.current
                        val isPinSet = SecurityUtils.isPinSet(context)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "SOS Deactivation PIN",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (isPinSet) "4-digit PIN configured" else "No PIN set — Security risk!",
                                    fontSize = 12.sp,
                                    color = if (isPinSet) Color.Gray else Color.Red
                                )
                            }
                            
                            Button(
                                onClick = { showSetPinDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(if (isPinSet) "Update" else "Set PIN", fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        AddContactDialog(
            onAdd = { name, phone, relation ->
                if (localEmergencyContacts.size < 5) {
                    localEmergencyContacts = (localEmergencyContacts + EmergencyContact(
                        name = name,
                        phone = phone,
                        relation = relation
                    )).toMutableList()
                }
                showAddContactDialog = false
            },
            onDismiss = { showAddContactDialog = false }
        )
    }

    if (showSetPinDialog) {
        SetPinDialog(
            onDismiss = { showSetPinDialog = false },
            onPinSet = { pin ->
                SecurityUtils.savePin(context, pin)
                showSetPinDialog = false
            }
        )
    }
}

@Composable
fun SetPinDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val primaryColor = Color(0xFF650927)

    Dialog(onDismissRequest = onDismiss) {
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
                Text("Set Security PIN", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = primaryColor)
                Text("Choose a 4-digit PIN to stop your SOS alert.", fontSize = 14.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("Enter 4-digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = Color.Red, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (pin.length != 4) {
                            errorMsg = "PIN must be 4 digits"
                        } else if (pin != confirmPin) {
                            errorMsg = "PINs do not match"
                        } else {
                            onPinSet(pin)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Save PIN")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Text(
            if (value.isEmpty() || value == "0") "-" else value,
            fontSize = 13.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EmergencyContactCard(
    contact: EmergencyContact,
    onDelete: () -> Unit,
    isEditing: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE8DADC), MaterialTheme.shapes.small),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9FA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    contact.phone,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    contact.relation,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            if (isEditing) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddContactDialog(
    onAdd: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = Color(0xFF650927)
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRelation by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Add Emergency Contact",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Relation Dropdown
                var expanded by remember { mutableStateOf(false) }
                val relationOptions = listOf("Family", "Friend", "Colleague")

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedRelation,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Relation") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        relationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedRelation = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isNotEmpty() && phone.isNotEmpty() && selectedRelation.isNotEmpty()) {
                                onAdd(name, phone, selectedRelation)
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        enabled = name.isNotEmpty() && phone.isNotEmpty() && selectedRelation.isNotEmpty()
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}
