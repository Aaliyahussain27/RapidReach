package com.example.rapidreach.screens.recordings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rapidreach.data.local.entity.SosLogEntity
import com.example.rapidreach.viewmodel.SosRecordingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosRecordingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SosRecordingsViewModel = viewModel()
) {
    val logs by viewModel.allLogs.collectAsState()
    val primaryColor = Color(0xFF650927)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Library", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFDFDFD)
    ) { padding ->
        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("No recordings found", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logs) { log ->
                    RecordingItem(log, onPlay = { viewModel.playRecording(it) }, primaryColor)
                }
            }
        }
    }
}

@Composable
fun RecordingItem(log: SosLogEntity, onPlay: (String) -> Unit, primaryColor: Color) {
    var isPlaying by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Emergency Recording",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = primaryColor
                )
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (log.officialService.isNotEmpty()) {
                    Text(
                        text = "Notified: ${log.officialService}",
                        fontSize = 11.sp,
                        color = primaryColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (log.audioFilePath.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onPlay(log.audioFilePath)
                        isPlaying = !isPlaying
                    },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = primaryColor)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play"
                    )
                }
            }
        }
    }
}
