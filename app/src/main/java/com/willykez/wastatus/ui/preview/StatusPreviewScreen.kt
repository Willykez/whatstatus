package com.willykez.wastatus.ui.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.willykez.wastatus.model.StatusItem
import com.willykez.wastatus.model.StatusType
import com.willykez.wastatus.ui.theme.LocalExtendedColors
import com.willykez.wastatus.util.AppImageLoader

@Composable
fun StatusPreviewScreen(
    status: StatusItem,
    onBack: () -> Unit,
    onSaveStatus: (String) -> Unit,
    onShare: (StatusItem) -> Unit,
    onRepost: (StatusItem) -> Unit,
    onToggleVault: (StatusItem) -> Unit
) {
    val context = LocalContext.current
    val extended = LocalExtendedColors.current
    var isControlsVisible by remember { mutableStateOf(true) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("preview_screen_${status.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(status.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (status.type == StatusType.IMAGE) {
                                if (zoomScale > 1f) {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    zoomScale = 2.5f
                                }
                            }
                        },
                        onTap = { isControlsVisible = !isControlsVisible }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (status.type == StatusType.IMAGE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(status.id) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                panOffset = if (zoomScale > 1f) panOffset + pan else Offset.Zero
                            }
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(status.uri)
                            .crossfade(true)
                            .build(),
                        imageLoader = AppImageLoader.get(context),
                        contentDescription = status.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = panOffset.x,
                                translationY = panOffset.y
                            )
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (isControlsVisible) 110.dp else 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (zoomScale > 1.1f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                                contentDescription = "Zoom status",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zoom: ${(zoomScale * 100).toInt()}% • Double-tap to reset",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                RealVideoPlayer(status = status)
            }
        }

        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("preview_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = status.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = "${status.timestamp} • ${status.sizeFormatted} • ${status.sourceLabel}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.32f))
                            .padding(horizontal = 2.dp)
                    ) {
                        IconButton(onClick = { onToggleVault(status) }, modifier = Modifier.testTag("preview_vault_button")) {
                            Icon(
                                imageVector = if (status.isVaulted) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (status.isVaulted) "Remove from Vault" else "Add to Vault",
                                tint = if (status.isVaulted) extended.vaultAccent else Color.White
                            )
                        }
                        IconButton(onClick = { onRepost(status) }, modifier = Modifier.testTag("preview_repost_button")) {
                            Icon(imageVector = Icons.Default.Repeat, contentDescription = "Repost to Status", tint = Color.White)
                        }
                        IconButton(onClick = { onShare(status) }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSaveStatus(status.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status.isSaved) extended.success else MaterialTheme.colorScheme.primary,
                            contentColor = if (status.isSaved) extended.onSuccess else MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("preview_save_button")
                    ) {
                        Icon(
                            imageVector = if (status.isSaved) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (status.isSaved) "Saved" else "Save",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isControlsVisible && status.caption.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Text(text = status.caption, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (status.type == StatusType.IMAGE) "Image Status • Pinch to Zoom" else "Video Status",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/** Real, hardware-accelerated video playback via Media3 ExoPlayer — not a simulated progress bar. */
@Composable
private fun RealVideoPlayer(status: StatusItem) {
    val context = LocalContext.current
    val exoPlayer = remember(status.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(status.uri))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(status.id) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
