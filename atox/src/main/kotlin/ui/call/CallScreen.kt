package ltd.evilcorp.atox.ui.call

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ltd.evilcorp.atox.R
import ltd.evilcorp.core.model.Contact
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import ltd.evilcorp.atox.util.PermissionManager
import ltd.evilcorp.atox.ui.common.ContactAvatar
import ltd.evilcorp.domain.feature.CallState

@Composable
fun CallScreen(
    contactState: State<Contact?>,
    callState: State<CallState>,
    sendingAudioState: State<Boolean>,
    speakerphoneOnState: State<Boolean>,
    connectedAtState: State<Long>,
    permissionManager: PermissionManager,
    onMinimize: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    BackHandler(onBack = onMinimize)

    val contact = contactState.value
    val currentCallState = callState.value
    val connectedAt = connectedAtState.value

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!sendingAudioState.value) {
                onToggleMic()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.call_mic_permission_needed), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionManager.canRecordAudio()) {
            permissionLauncher.launch(permissionManager.recordAudioPermission)
        }
    }

    val name = contact?.name?.ifEmpty { stringResource(R.string.contact_default_name) }
        ?: stringResource(R.string.contact_default_name)

    val statusText = when (currentCallState) {
        is CallState.OutgoingRequesting -> stringResource(R.string.call_screen_requesting)
        is CallState.OutgoingWaiting -> stringResource(R.string.call_screen_waiting)
        is CallState.Connecting -> stringResource(R.string.call_screen_connecting)
        is CallState.OutgoingRinging -> stringResource(R.string.call_screen_ringing)
        is CallState.Active -> formatCallDuration(connectedAt)
        is CallState.IncomingRinging -> stringResource(R.string.incoming_call)
        CallState.Idle -> stringResource(R.string.call_screen_calling)
    }

    // Call ring animation (pulsing circles)
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B1429), // Premium deep violet
                        Color(0xFF0F0A1A), // Deep indigo
                        Color(0xFF08040F)  // Pure midnight black
                    ),
                ),
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onMinimize,
            modifier = Modifier
                .statusBarsPadding()
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.return_to_chat), modifier = Modifier.size(32.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp) // Compact outer Box removes excessive margins
            ) {
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            alpha = alpha
                        )
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(144.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ContactAvatar(
                            name = name,
                            publicKey = contact?.publicKey.orEmpty(),
                            avatarUri = contact?.avatarUri.orEmpty(),
                            size = 124.dp,
                            fontSize = 40.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // Reduced spacer for compact, elegant spacing

            Text(
                text = name,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.08f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)), // Vibrant glowing green for status indicator dot!
                    )
                    Text(
                        text = statusText,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        // Control buttons row at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isMicMuted = !sendingAudioState.value
                val micIcon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic
                val micTint = Color.White
                val micBg = if (isMicMuted) Color(0xFFBA1A1A) else Color.White.copy(alpha = 0.15f)

                IconButton(
                    onClick = {
                        if (permissionManager.canRecordAudio()) {
                            onToggleMic()
                        } else {
                            permissionLauncher.launch(permissionManager.recordAudioPermission)
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(micBg),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = micTint),
                ) {
                    Icon(
                        imageVector = micIcon,
                        contentDescription = "Toggle Microphone",
                        tint = micTint,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Drop Call Button (Red, in the center)
                FilledIconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFE84A5F), // Premium soft red/coral
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.end_call),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Speakerphone Toggle Button
                val isSpeakerOn = speakerphoneOnState.value
                val speakerIcon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff
                val speakerTint = Color.White
                val speakerBg = if (isSpeakerOn) Color(0xFF2196F3) else Color.White.copy(alpha = 0.15f)

                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(speakerBg),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = speakerTint),
                ) {
                    Icon(
                        imageVector = speakerIcon,
                        contentDescription = "Toggle Speakerphone",
                        tint = speakerTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun formatCallDuration(connectedAt: Long): String {
    if (connectedAt <= 0L) {
        return "00:00"
    }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(connectedAt) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            kotlinx.coroutines.delay(1_000)
        }
    }
    val totalSeconds = ((now - connectedAt) / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
