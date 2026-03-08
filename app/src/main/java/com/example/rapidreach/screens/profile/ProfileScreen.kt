@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.rapidreach.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
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
import com.example.rapidreach.data.model.MedicalInfo
import com.example.rapidreach.data.model.User
import com.example.rapidreach.viewmodel.ProfileViewModel

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
    var localEmergencyContacts by remember { mutableStateOf(currentUser?.emergencyContacts ?: emptyList()) }

    val displayUser = currentUser ?: User()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFDFD))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                "Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { editingContacts = !editingContacts }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar and Name Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(100.dp),
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
                            Text(
                                "Emergency Contacts (${localEmergencyContacts.size}/5)",
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

                        if (editingContacts && localEmergencyContacts.isNotEmpty()) {
                            HorizontalDivider()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { editingContacts = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = primaryColor
                                    )
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        viewModel.updateEmergencyContacts(currentUser!!.copy(  // ← user → currentUser!!
                                            emergencyContacts = localEmergencyContacts
                                        ))
                                        editingContacts = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryColor
                                    )
                                ) {
                                    Text("Save")
                                }
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
            value.ifEmpty { "-" },
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
    val primaryColor = Color(0xFF650927)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Color(0xFFE8DADC),
                MaterialTheme.shapes.small
            ),
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
    var relation by remember { mutableStateOf("") }
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
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
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
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
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
