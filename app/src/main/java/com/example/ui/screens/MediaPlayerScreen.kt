package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.DownloadItem
import com.example.ui.MainViewModel
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun MediaPlayerOverlay(
    item: DownloadItem,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("media_player_overlay")
    ) {
        if (item.isAudioOnly) {
            AudioPlayerContent(item = item, onClose = onClose)
        } else {
            VideoPlayerContent(item = item, onClose = onClose)
        }
    }
}

@Composable
fun VideoPlayerContent(
    item: DownloadItem,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var videoProgress by remember { mutableFloatStateOf(0f) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var durationSeconds by remember { mutableIntStateOf(0) }
    var currentPositionSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(item.localPath) {
        while (true) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    isPlaying = true
                    currentPositionSeconds = vv.currentPosition / 1000
                    val dur = vv.duration
                    if (dur > 0) {
                        durationSeconds = dur / 1000
                        videoProgress = currentPositionSeconds.toFloat() / durationSeconds.toFloat()
                    }
                } else {
                    isPlaying = false
                }
            }
            delay(500)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Player header top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { /* Fullscreen toggle */ }) {
                Icon(Icons.Default.Fullscreen, contentDescription = "", tint = Color.White)
            }
        }

        // Integrated Native Video Player Wrapper
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(Uri.fromFile(File(item.localPath)))
                        val mediaController = MediaController(ctx)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        setOnPreparedListener { player ->
                            player.isLooping = true
                            durationSeconds = duration / 1000
                            start()
                        }
                        videoViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Custom Controllers timeline setup
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
                        onValueChange = { 
                            videoProgress = it 
                            videoViewRef?.let { vv ->
                                val targetMs = (it * durationSeconds * 1000).toInt()
                                vv.seekTo(targetMs)
                            }
                        },
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

            // Control Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    videoViewRef?.let { vv ->
                        val target = (vv.currentPosition - 10000).coerceAtLeast(0)
                        vv.seekTo(target)
                    }
                }) {
                    Icon(Icons.Default.Replay10, contentDescription = "", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(24.dp))

                FloatingActionButton(
                    onClick = {
                        videoViewRef?.let { vv ->
                            if (vv.isPlaying) {
                                vv.pause()
                                isPlaying = false
                            } else {
                                vv.start()
                                isPlaying = true
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Trigger Playback"
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                IconButton(onClick = {
                    videoViewRef?.let { vv ->
                        val target = (vv.currentPosition + 10000).coerceAtMost(vv.duration)
                        vv.seekTo(target)
                    }
                }) {
                    Icon(Icons.Default.Forward10, contentDescription = "", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun AudioPlayerContent(
    item: DownloadItem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    val mediaPlayer = remember { MediaPlayer() }
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

    // Rotational vinyl disk configuration setup
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B24))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Player header top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text = "Audio Workspace",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.Share, contentDescription = "", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.weight(0.15f))

        // Large Spinning Audio Vinyl illustration
        Box(
            modifier = Modifier
                .size(260.dp)
                .rotate(if (isPlaying) rotationAngle else 0f)
                .background(Color.Black, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Draw Vinyl Grooves
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color.DarkGray, radius = size.minDimension / 2.2f, style = Stroke(width = 2f))
                drawCircle(color = Color.DarkGray, radius = size.minDimension / 2.6f, style = Stroke(width = 1f))
                drawCircle(color = Color.DarkGray, radius = size.minDimension / 3.2f, style = Stroke(width = 1.5f))
            }

            // Central Colored Disc Core
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.15f))

        Text(
            text = item.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            text = "Codec: MP3 Audio | Quality: MP3 320kbps",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Glowing sound wave visualizer
        LiveWaveVisualizer(isPlaying = isPlaying)

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

        // Replay/Play/Forward buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val file = File(item.localPath)
                if (file.exists() && file.length() > 50000) {
                    val targetMs = (mediaPlayer.currentPosition - 10000).coerceAtLeast(0)
                    mediaPlayer.seekTo(targetMs)
                } else {
                    currentPositionSeconds = (currentPositionSeconds - 10).coerceAtLeast(0)
                }
            }) {
                Icon(Icons.Default.Replay10, contentDescription = "", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.width(28.dp))

            FloatingActionButton(
                onClick = { isPlaying = !isPlaying },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Trigger Playback",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            IconButton(onClick = {
                val file = File(item.localPath)
                if (file.exists() && file.length() > 50000) {
                    val targetMs = (mediaPlayer.currentPosition + 10000).coerceAtMost(mediaPlayer.duration)
                    mediaPlayer.seekTo(targetMs)
                } else {
                    currentPositionSeconds = (currentPositionSeconds + 10).coerceAtMost(durationSeconds)
                }
            }) {
                Icon(Icons.Default.Forward10, contentDescription = "", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // Playback Speeds Slider option selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(2.0f, 1.5f, 1.0f, 0.5f).forEach { speed ->
                SpeedSelectorChip(
                    speed = speed,
                    isSelected = playbackSpeed == speed,
                    onClick = { playbackSpeed = speed }
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = ":Speed",
                color = Color.Gray,
                fontSize = 11.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun SpeedSelectorChip(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp)),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
        border = Stroke(width = if (isSelected) 2f else 1f).let {
            androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f))
        }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "${speed}x", color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LiveWaveVisualizer(isPlaying: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barsCount = 28
        val infiniteTransition = rememberInfiniteTransition(label = "waves")
        
        for (i in 0 until barsCount) {
            val waveHeight by if (isPlaying) {
                infiniteTransition.animateFloat(
                    initialValue = 10f,
                    targetValue = 40f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = (500 + (i * 20) % 500),
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "wave-$i"
                )
            } else {
                remember { mutableStateOf(8f) }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(3.dp)
                    .height(waveHeight.dp)
                    .background(
                        color = if (i % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        shape = CircleShape
                    )
            )
        }
    }
}

fun formatPosition(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
