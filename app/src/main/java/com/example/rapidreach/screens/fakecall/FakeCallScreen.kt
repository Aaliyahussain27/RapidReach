package com.example.rapidreach.screens.fakecall

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.speech.tts.TextToSpeech
import java.util.Locale
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
    var isCallEnded by remember { mutableStateOf(false) }
    var isCallAccepted by remember { mutableStateOf(false) }
    var ringtonePlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var messagePlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var textToSpeech: TextToSpeech? by remember { mutableStateOf(null) }
    var incomingDuration by remember { mutableStateOf(0) }
    var activeDuration by remember { mutableStateOf(0) }

    fun endCall() {
        if (!isCallEnded) {
            isCallEnded = true
            try {
                ringtonePlayer?.stop()
                ringtonePlayer?.release()
                ringtonePlayer = null
                
                messagePlayer?.stop()
                messagePlayer?.release()
                messagePlayer = null

                textToSpeech?.stop()
                textToSpeech?.shutdown()
                textToSpeech = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onBack()
        }
    }

    fun playDefaultMessage() {
        val resId = context.resources.getIdentifier("fake_message", "raw", context.packageName)
        if (resId != 0) {
            messagePlayer = MediaPlayer.create(context, resId)
            messagePlayer?.start()
        } else {
            // Fallback to TTS with the specific safety phrase
            val phrase = "Don't worry beta, I have your live location on my phone. " +
                    "The police tracking is also enabled. I am reaching the landmark in 5 minutes. " +
                    "Just keep walking and stay on the call."
            textToSpeech?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "fake_call_msg")
        }
    }

    fun acceptCall() {
        if (!isCallAccepted) {
            isCallAccepted = true
            try {
                // Stop ringtone
                ringtonePlayer?.stop()
                ringtonePlayer?.release()
                ringtonePlayer = null

                // Start prerecorded message OR Custom Picked File OR TTS
                val sharedPrefs = context.getSharedPreferences("RapidReachPrefs", Context.MODE_PRIVATE)
                val customUriString = sharedPrefs.getString("custom_fake_call_uri", null)
                
                if (customUriString != null) {
                    try {
                        val customUri = Uri.parse(customUriString)
                        messagePlayer = MediaPlayer().apply {
                            setDataSource(context, customUri)
                            prepare()
                            start()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Fallback if custom URI fails (e.g., file deleted)
                        playDefaultMessage()
                    }
                } else {
                    playDefaultMessage()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Initialize TTS
    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
    }

    // Initialize and play ringtone
    LaunchedEffect(Unit) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtonePlayer = MediaPlayer.create(context, uri)
            ringtonePlayer?.isLooping = true
            ringtonePlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Call Timers
    LaunchedEffect(isCallAccepted, isCallEnded) {
        if (!isCallAccepted && !isCallEnded) {
            // Incoming call duration
            for (i in 0 until 30) {
                if (isCallAccepted || isCallEnded) break
                delay(1000)
                incomingDuration = i
            }
            if (!isCallAccepted && !isCallEnded) {
                endCall()
            }
        } else if (isCallAccepted && !isCallEnded) {
            // Active call duration
            while (!isCallEnded) {
                delay(1000)
                activeDuration++
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                ringtonePlayer?.stop()
                ringtonePlayer?.release()
                messagePlayer?.stop()
                messagePlayer?.release()
                textToSpeech?.stop()
                textToSpeech?.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                if (isCallAccepted) "Ongoing Call..." else "Incoming Call",
                fontSize = 18.sp,
                color = if (isCallAccepted) Color(0xFF22c55e) else Color(0xFFBDBDBD),
                fontWeight = if (isCallAccepted) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Call Duration
            if (isCallAccepted) {
                val minutes = activeDuration / 60
                val seconds = activeDuration % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Text(
                    "${30 - incomingDuration} sec remaining",
                    fontSize = 14.sp,
                    color = Color(0xFF9DBFE0)
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // End/Decline Button
                IncomingCallButton(
                    icon = Icons.Default.Close,
                    backgroundColor = Color(0xFFD32F2F),
                    onClick = { endCall() },
                    rotation = 45f
                )

                if (!isCallAccepted) {
                    // Accept Button
                    IncomingCallButton(
                        icon = Icons.Default.Call,
                        backgroundColor = acceptButtonColor,
                        onClick = { acceptCall() }
                    )
                }
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
