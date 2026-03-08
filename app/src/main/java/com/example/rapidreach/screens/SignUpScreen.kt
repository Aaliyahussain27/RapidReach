package com.example.rapidreach.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rapidreach.data.model.EmergencyContact
import com.example.rapidreach.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSignupSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var categoryError by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var emergencyContacts by remember { mutableStateOf(listOf(EmergencyContact())) }

    val primaryColor = Color(0xFF650927)
    val errorColor = Color(0xFFB3261E)
    val lightBackground = Color(0xFFFFF0F3)
    val scrollState = rememberScrollState()

    val categoryOptions = listOf("Woman", "Student", "Elderly", "General")
    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
    val relationOptions = listOf("Family", "Friend", "Colleague")

    fun validatePhone(value: String): String = when {
        value.isEmpty() -> "Phone number is required"
        value.length != 10 -> "Phone number must be exactly 10 digits"
        !value.all { c -> c.isDigit() } -> "Phone number must contain only digits"
        else -> ""
    }

    fun validatePassword(value: String): String = when {
        value.isEmpty() -> "Password is required"
        value.length < 6 -> "Password must be at least 6 characters"
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            modifier = Modifier.align(Alignment.Start)
        )

        Text(
            text = "Join us for a safer tomorrow",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Full Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone
        OutlinedTextField(
            value = phone,
            onValueChange = { value ->
                phone = value
                phoneError = validatePhone(value)
            },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = phoneError.isNotEmpty(),
            shape = MaterialTheme.shapes.medium
        )
        if (phoneError.isNotEmpty()) {
            Text(
                text = phoneError,
                color = errorColor,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Dropdown
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("I am a...") },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                isError = categoryError.isNotEmpty(),
                shape = MaterialTheme.shapes.medium
            )

            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categoryOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            category = option
                            categoryError = ""
                            categoryExpanded = false
                        }
                    )
                }
            }
        }
        if (categoryError.isNotEmpty()) {
            Text(
                text = categoryError,
                color = errorColor,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gender Dropdown
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender (Optional)") },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                genderOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            gender = option
                            genderExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Emergency Contacts Section
        Text(
            "Emergency Contacts",
            fontWeight = FontWeight.SemiBold,
            color = primaryColor,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            "Add at least 1 contact who will be alerted during SOS",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        emergencyContacts.forEachIndexed { index, contact ->
            var relationExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = lightBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Contact ${index + 1}",
                        fontWeight = FontWeight.Medium,
                        color = primaryColor,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contact.name,
                        onValueChange = { newVal ->
                            emergencyContacts = emergencyContacts.toMutableList().also {
                                it[index] = it[index].copy(name = newVal)
                            }
                        },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contact.phone,
                        onValueChange = { newVal ->
                            emergencyContacts = emergencyContacts.toMutableList().also {
                                it[index] = it[index].copy(phone = newVal)
                            }
                        },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = relationExpanded,
                        onExpandedChange = { relationExpanded = !relationExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = contact.relation.ifEmpty { "Relation" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Relation") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(relationExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = relationExpanded,
                            onDismissRequest = { relationExpanded = false }
                        ) {
                            relationOptions.forEach { rel ->
                                DropdownMenuItem(
                                    text = { Text(rel) },
                                    onClick = {
                                        emergencyContacts = emergencyContacts.toMutableList().also {
                                            it[index] = it[index].copy(relation = rel)
                                        }
                                        relationExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (emergencyContacts.size > 1) {
                        TextButton(
                            onClick = {
                                emergencyContacts = emergencyContacts.toMutableList().also {
                                    it.removeAt(index)
                                }
                            }
                        ) {
                            Text("Remove", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (emergencyContacts.size < 5) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { emergencyContacts = emergencyContacts + EmergencyContact() },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, primaryColor)
            ) {
                Text("+ Add Another Contact", color = primaryColor, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { value ->
                password = value
                passwordError = validatePassword(value)
            },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = passwordError.isNotEmpty(),
            shape = MaterialTheme.shapes.medium
        )
        if (passwordError.isNotEmpty()) {
            Text(
                text = passwordError,
                color = errorColor,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Submit Button
        Button(
            onClick = {
                categoryError = if (category.isEmpty()) "Please select a category" else ""
                if (categoryError.isEmpty() && phoneError.isEmpty() && passwordError.isEmpty() && emergencyContacts.isNotEmpty()) {
                    authViewModel.signup(
                        name = name,
                        email = email,
                        phone = phone,
                        password = password,
                        confirmPassword = confirmPassword,
                        userType = category,
                        gender = gender,
                        emergencyContacts = emergencyContacts
                    )
                    onSignupSuccess()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = MaterialTheme.shapes.medium,
            enabled = name.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() &&
                    password.isNotEmpty() && confirmPassword.isNotEmpty() && category.isNotEmpty()
        ) {
            Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", color = Color.Gray, fontSize = 14.sp)
            TextButton(onClick = onBackToLogin) {
                Text(
                    "Login",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}