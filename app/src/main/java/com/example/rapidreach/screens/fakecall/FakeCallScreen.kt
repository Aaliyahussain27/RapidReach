package com.example.rapidreach.screens.fakecall

import android.media.Ringtone
import android.media.RingtoneManager
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FakeCallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF650927)
    var ringtone: Ringtone? by remember { mutableStateOf(null) }
    var callDuration by remember { mutableStateOf(0) }

    // Initialize and play ringtone
    LaunchedEffect(Unit) {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.play()
    }

    // Auto-dismiss after 30 seconds
    LaunchedEffect(Unit) {
        for (i in 0 until 30) {
            delay(1000)
            callDuration = i
        }
        ringtone?.stop()
        onBack()
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            ringtone?.stop()
        }
    }

    // Pulsing animation for accept button
    val infiniteTransition = rememberInfiniteTransition(label = "callPulse")
    val acceptButtonColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF16a34a),
        targetValue = Color(0xFF22c55e),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ),
        label = "acceptColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D1B2E),
                        Color(0xFF1A0F1B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Caller Avatar
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color(0xFF4a4a4a), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "M",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Caller Name
            Text(
                "Mom",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Call Status
            Text(
                "Incoming Call",
                fontSize = 16.sp,
                color = Color(0xFFBDBDBD)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Call Duration
            Text(
                "$callDuration sec",
                fontSize = 14.sp,
                color = Color(0xFF9DBFE0)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline Button
                IncomingCallButton(
                    icon = Icons.Default.Close,
                    backgroundColor = Color(0xFFD32F2F),
                    onClick = {
                        ringtone?.stop()
                        onBack()
                    },
                    rotation = 45f
                )

                // Accept Button
                IncomingCallButton(
                    icon = Icons.Default.Call,
                    backgroundColor = acceptButtonColor,
                    onClick = {
                        ringtone?.stop()
                        onBack()
                    }
                )
            }
        }
    }
}

@Composable
fun IncomingCallButton(
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    rotation: Float = 0f
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .scale(scaleX = if (rotation > 0) -1f else 1f, scaleY = 1f)
        )
    }
}
