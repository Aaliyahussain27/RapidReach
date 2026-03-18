package com.example.rapidreach.screens.helpline

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rapidreach.data.model.User

data class Helpline(
    val name: String,
    val number: String,
    val category: String,
    val priority: Int = 999 
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelplineScreen(
    currentUser: User?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF650927)
    var searchQuery by remember { mutableStateOf("") }

    // All helplines (Emojis removed)
    val allHelplines = listOf(
        Helpline("Police Station", "100", "Police", 1),
        Helpline("Mahila Helpline", "1091", "Women", 2),
        Helpline("Ambulance", "108", "Medical", 3),
        Helpline("Fire Station", "101", "Fire", 4),
        Helpline("National Emergency", "112", "Emergency", 5),
        Helpline("Domestic Violence", "181", "Women", 6),
        Helpline("Child Helpline", "1098", "Child", 7),
        Helpline("Senior Citizen", "14567", "Elderly", 8),
        Helpline("Disaster Management", "1078", "Disaster", 9),
        Helpline("Road Accident", "1073", "Traffic", 10),
        Helpline("Cyber Crime", "1930", "Cyber", 11),
        Helpline("Student Mental Health", "9152987821", "Mental Health", 12)
    )

    // Filtered and Sorted helplines
    val filteredHelplines = allHelplines.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.category.contains(searchQuery, ignoreCase = true) 
    }.sortedBy { helpline ->
        when {
            // Priority for Women: Police and Mahila Helpline first
            currentUser?.userType == "Woman" -> {
                when (helpline.name) {
                    "Police Station" -> 0
                    "Mahila Helpline" -> 1
                    else -> helpline.priority + 10
                }
            }
            // Priority for Elderly: Hospital (Ambulance) first
            currentUser?.userType == "Elderly" -> {
                when (helpline.category) {
                    "Medical" -> 0
                    "Elderly" -> 1
                    else -> helpline.priority + 10
                }
            }
            else -> helpline.priority
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = {
                        Text(
                            "Emergency Helplines",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
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
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search by name or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(filteredHelplines) { helpline ->
                HelplineCard(helpline, primaryColor) {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:${helpline.number}")
                    context.startActivity(intent)
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HelplineCard(
    helpline: Helpline,
    primaryColor: Color,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    helpline.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    helpline.category,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    helpline.number,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = primaryColor
                )
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .background(primaryColor, shape = MaterialTheme.shapes.small)
                        .size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
