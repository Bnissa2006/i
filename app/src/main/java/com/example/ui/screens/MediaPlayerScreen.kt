package com.example.ui.screens

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.DownloadItem
import com.example.ui.MainViewModel
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPlayerContainer(
    viewModel: MainViewModel,
    lang: String,
    modifier: Modifier = Modifier
) {
    val playingVideo by viewModel.playingVideo.collectAsState()
    val playingAudio by viewModel.playingAudio.collectAsState()

    if (playingVideo != null) {
        VideoPlayerView(
            item = playingVideo!!,
            onClose = { viewModel.setPlayingVideo(null) },
            modifier = modifier
        )
    } else if (playingAudio != null) {
        AudioPlayerView(
            item = playingAudio!!,
            onClose = { viewModel.setPlayingAudio(null) },
            modifier = modifier
        )
    } else {
        // Fallback UI if opened without arguments
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Select a media file from Downloads or Files to play", color = Color.White)
        }
    }
}

@Composable
fun VideoPlayerView(
    item: DownloadItem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFullScreen by remember { mutableStateOf(false) }

    // Simulated timeline states since mock downloaded fragments contain brief dummy packets
    var videoProgress by remember { mutableFloatStateOf(0f) }
    var durationSeconds by remember { mutableIntStateOf(168) } // Average mock song duration
    val currentPositionSeconds = (videoProgress * durationSeconds).toInt()

    LaunchedEffect(isPlaying, playbackSpeed) {
        if (isPlaying) {
            while (videoProgress < 1.0f) {
                delay((1000 / playbackSpeed).toLong())
                videoProgress = (videoProgress + (1f / durationSeconds)).coerceAtMost(1.0f)
            }
            isPlaying = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_screen")
    ) {
        // Control bar header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close player", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Full screen toggle helper
            IconButton(onClick = { isFullScreen = !isFullScreen }) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Full Screen Toggle",
                    tint = Color.White
                )
            }
        }

        // Active Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isFullScreen) 1f else 0.65f)
                .background(Color.DarkGray)
        ) {
            val file = File(item.localPath)
            if (file.exists() && file.length() > 50000) { // If it's a real sizeable loaded file, bind native renderer
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoPath(item.localPath)
                            val mediaController = MediaController(ctx)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            setOnPreparedListener { mp ->
                                durationSeconds = (duration / 1000).coerceAtLeast(1)
                                mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                                start()
                            }
                        }
                    },
                    update = { view ->
                        if (isPlaying) {
                            view.start()
                        } else {
                            view.pause()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Highly elegant mock visual player background when in preview mode with placeholder files
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Playing: [Video Feed - ${item.resolution}]",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Built-in Screen Renderer Active",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Playback Custom controls row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Slider timeline and metrics wrapped in LTR to avoid reversed presentation on Arabic/RTL devices
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column {
                        Slider(
                            value = videoProgress,
                            onValueChange = { videoProgress = it },
                            modifier = Modifier.fillMaxWidth().testTag("video_timeline_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = formatPosition(currentPositionSeconds), fontSize = 11.sp, color = Color.Gray)
                            Text(text = formatPosition(durationSeconds), fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Core control button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back 10s
                    IconButton(onClick = {
                        videoProgress = (videoProgress - (10f / durationSeconds)).coerceAtLeast(0f)
                    }) {
                        Icon(imageVector = Icons.Default.Replay10, contentDescription = "Back 10 Seconds", modifier = Modifier.size(28.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Big play pause
                    FloatingActionButton(
                        onClick = { isPlaying = !isPlaying },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(54.dp).testTag("video_play_toggle")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play toggle",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Forward 10s
                    IconButton(onClick = {
                        videoProgress = (videoProgress + (10f / durationSeconds)).coerceAtMost(1.0f)
                    }) {
                        Icon(imageVector = Icons.Default.Forward10, contentDescription = "Forward 10 Seconds", modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback speed chips toggle selector
                Text(
                    text = "Playback Speed Rate:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
                    speeds.forEach { speed ->
                        val isSel = playbackSpeed == speed
                        FilterChip(
                            selected = isSel,
                            onClick = { playbackSpeed = speed },
                            label = { Text("${speed}x", fontSize = 11.sp) },
                            modifier = Modifier.testTag("speed_chip_${speed}x")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPlayerView(
    item: DownloadItem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    val mediaPlayer = remember { android.media.MediaPlayer() }
    var durationSeconds by remember { mutableIntStateOf(214) }
    var currentPositionSeconds by remember { mutableIntStateOf(0) }
    var audioProgress by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    DisposableEffect(item.localPath) {
        val file = File(item.localPath)
        if (file.exists() && file.length() > 50000) {
            try {
                mediaPlayer.setDataSource(item.localPath)
                mediaPlayer.prepare()
                durationSeconds = (mediaPlayer.duration / 1000).coerceAtLeast(1)
                if (isPlaying) {
                    mediaPlayer.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isPlaying, playbackSpeed) {
        val file = File(item.localPath)
        val hasRealFile = file.exists() && file.length() > 50000

        while (true) {
            if (hasRealFile) {
                try {
                    mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed)
                } catch (e: Exception) {}

                if (isPlaying) {
                    if (!mediaPlayer.isPlaying) {
                        try { mediaPlayer.start() } catch (e: Exception) {}
                    }
                    currentPositionSeconds = mediaPlayer.currentPosition / 1000
                    if (!isSeeking && durationSeconds > 0) {
                        audioProgress = currentPositionSeconds.toFloat() / durationSeconds.toFloat()
                    }
                } else {
                    if (mediaPlayer.isPlaying) {
                        try { mediaPlayer.pause() } catch (e: Exception) {}
                    }
                }
            } else {
                if (isPlaying) {
                    delay((1000 / playbackSpeed).toLong())
                    currentPositionSeconds = (currentPositionSeconds + 1).coerceAtMost(durationSeconds)
                    audioProgress = currentPositionSeconds.toFloat() / durationSeconds.toFloat()
                    if (audioProgress >= 1.0f) {
                        isPlaying = false
                    }
                }
            }
            delay(250)
        }
    }

    // Disk rotating angle transition
    val infiniteTransition = rememberInfiniteTransition(label = "rotating_disk")
    val rotationDiskAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotating_disk_angle"
    )

    // Animated bouncers for rhythmic audio wave simulation
    val waveAnimationMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_anim"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                )
            )
            .padding(16.dp)
            .testTag("audio_player_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Core header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close player")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Audio Workspace",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // Large Rotating Album Vinyl Disc
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .rotate(if (isPlaying) rotationDiskAngle else 0f),
            contentAlignment = Alignment.Center
        ) {
            // Outlined track markings
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color.DarkGray, radius = size.minDimension / 2.2f, style = Stroke(2.dp.toPx()))
                drawCircle(color = Color.Gray, radius = size.minDimension / 3.2f, style = Stroke(1.dp.toPx()))
                drawCircle(color = Color.DarkGray, radius = size.minDimension / 4.4f, style = Stroke(1.dp.toPx()))
            }

            // Beautiful inner label representing SnapTube branded vinyl disk
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title and Quality Metas
        Text(
            text = item.title,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Text(
            text = "Codec: MP3 Audio | Quality: ${item.resolution}",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.weight(0.1f))

        // Dynamic soundwave visualizer using Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 24.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = 6.dp.toPx()
                val spacing = 4.dp.toPx()
                val totalBars = (size.width / (barWidth + spacing)).toInt()
                val midY = size.height / 2f

                for (i in 0 until totalBars) {
                    // Compose a wave pattern
                    val factor = sin((i.toDouble() / totalBars.toDouble()) * Math.PI * 4.0)
                    val waveHeight = (size.height * 0.8f * factor).toFloat() * waveAnimationMultiplier

                    val x = i * (barWidth + spacing)
                    val startY = midY - (waveHeight / 2)
                    val endY = midY + (waveHeight / 2)

                    drawRoundRect(
                        color = if (i.toFloat() / totalBars.toFloat() <= audioProgress) {
                            primaryColorGradient(i, totalBars)
                        } else {
                            Color.LightGray
                        },
                        topLeft = androidx.compose.ui.geometry.Offset(x, startY.coerceAtLeast(0f)),
                        size = androidx.compose.ui.geometry.Size(barWidth, (endY - startY).coerceAtLeast(4.dp.toPx())),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress metrics seek layout wrapped in LTR to ensure consistent presentation across Arabic locales
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column {
                Slider(
                    value = audioProgress,
                    onValueChange = { 
                        audioProgress = it 
                        isSeeking = true
                    },
                    onValueChangeFinished = {
                        val file = File(item.localPath)
                        if (file.exists() && file.length() > 50000) {
                            try {
                                val targetMs = (audioProgress * durationSeconds * 1000).toInt()
                                mediaPlayer.seekTo(targetMs)
                            } catch (e: Exception) {}
                        } else {
                            currentPositionSeconds = (audioProgress * durationSeconds).toInt()
                        }
                        isSeeking = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .testTag("audio_timeline_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatPosition(currentPositionSeconds), fontSize = 11.sp, color = Color.Gray)
                    Text(text = formatPosition(durationSeconds), fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback control group row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                audioProgress = (audioProgress - (10f / durationSeconds)).coerceAtLeast(0f)
            }) {
                Icon(imageVector = Icons.Default.Replay10, contentDescription = "Rewind", modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            FloatingActionButton(
                onClick = { isPlaying = !isPlaying },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(54.dp)
                    .testTag("audio_play_toggle")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Playback Control Toggle",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(onClick = {
                audioProgress = (audioProgress + (10f / durationSeconds)).coerceAtMost(1.0f)
            }) {
                Icon(imageVector = Icons.Default.Forward10, contentDescription = "Skip Forward", modifier = Modifier.size(28.dp))
            }
        }

        // Adjustable rate selector bottom row
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Speed:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { rSpeed ->
                val isSelected = playbackSpeed == rSpeed
                InputChip(
                    selected = isSelected,
                    onClick = { playbackSpeed = rSpeed },
                    label = { Text("${rSpeed}x", fontSize = 10.sp) },
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))
    }
}

// Custom visual audio wave bouncers index color gradients mapping
fun primaryColorGradient(index: Int, total: Int): Color {
    val fraction = index.toFloat() / total.toFloat()
    return if (fraction < 0.5f) {
        Color(0xFFD0BCFF) // Polish primary Lavender representation
    } else {
        Color(0xFFEFB8C8) // Polish tertiary Rose representation
    }
}

// Media clock parsing
fun formatPosition(posSec: Int): String {
    val mins = posSec / 60
    val secs = posSec % 60
    return String.format("%02d:%02d", mins, secs)
}
