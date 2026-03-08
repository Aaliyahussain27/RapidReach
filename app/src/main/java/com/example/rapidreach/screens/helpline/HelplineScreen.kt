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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    val emoji: String,
    val category: String,
    val priority: Int = 999 // for sorting user-type relevance
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelplineScreen(
    currentUser: User?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF650927)

    // All helplines
    val allHelplines = listOf(
        Helpline("Police", "100", "👮", "Police", 1),
        Helpline("Ambulance", "108", "🚑", "Medical", 2),
        Helpline("Fire", "101", "🔥", "Fire", 3),
        Helpline("National Emergency", "112", "🆘", "Emergency", 4),
        Helpline("Women's Helpline", "1091", "👩", "Women", 100),
        Helpline("Domestic Violence", "181", "🤝", "Women", 101),
        Helpline("Child Helpline", "1098", "👧", "Child", 200),
        Helpline("Senior Citizen", "14567", "👴", "Elderly", 300),
        Helpline("Disaster Management", "1078", "⛑️", "Disaster", 400),
        Helpline("Road Accident", "1073", "🚗", "Traffic", 500),
        Helpline("Cyber Crime", "1930", "💻", "Cyber", 600),
        Helpline("Student Mental Health (iCall)", "9152987821", "🧠", "Mental Health", 50)
    )

    // Sort based on user type
    val sortedHelplines = allHelplines.sortedBy { helpline ->
        when {
            currentUser?.userType == "Student" && helpline.category == "Mental Health" -> 0
            currentUser?.userType == "Student" && helpline.category == "Women" -> 1
            currentUser?.userType == "Elderly" && helpline.category == "Elderly" -> 0
            currentUser?.userType == "Elderly" && helpline.category == "Senior Citizen" -> 0
            currentUser?.userType == "Woman" && helpline.category == "Women" -> 0
            currentUser?.userType == "Child" && helpline.category == "Child" -> 0
            helpline.priority < 10 -> helpline.priority + 1000
            else -> helpline.priority
        }
    }

    Scaffold(
        topBar = {
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sortedHelplines) { helpline ->
                HelplineCard(helpline, primaryColor) {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:${helpline.number}")
                    context.startActivity(intent)
                }
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
            containerColor = if (helpline.priority < 10) Color(0xFFFFF3F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (helpline.priority < 10) 2.dp else 1.dp
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        helpline.emoji,
                        fontSize = 20.sp
                    )
                    Column {
                        Text(
                            helpline.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                        Text(
                            helpline.category,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    helpline.number,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = primaryColor
                )
                Button(
                    onClick = onCall,
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
