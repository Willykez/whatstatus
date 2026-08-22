package com.willykez.wastatus.ui.preview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.layout.ContentScale
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
import coil.request.videoFrameMillis
import com.willykez.wastatus.model.StatusItem
import com.willykez.wastatus.model.StatusType
import com.willykez.wastatus.ui.theme.LocalExtendedColors
import com.willykez.wastatus.util.AppImageLoader

/**
 * Full-screen status viewer, styled after Samsung Gallery's image viewer:
 * a minimal top bar (back + info only), a full-bleed image/video, a
 * horizontally scrollable thumbnail filmstrip of every other item in the
 * current list, and a flat icon-only action row along the bottom.
 */
@Composable
fun StatusPreviewScreen(
    status: StatusItem,
    filmstripItems: List<StatusItem>,
    onBack: () -> Unit,
    onSelect: (StatusItem) -> Unit,
    onSaveStatus: (String) -> Unit,
    onShare: (StatusItem) -> Unit,
    onRepost: (StatusItem) -> Unit,
    onToggleVault: (StatusItem) -> Unit
) {
    val context = LocalContext.current
    val extended = LocalExtendedColors.current
    var isControlsVisible by remember { mutableStateOf(true) }
    var isInfoVisible by remember { mutableStateOf(false) }
    var isMoreMenuVisible by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val filmstripState = rememberLazyListState()

    // Reset zoom and re-center the filmstrip whenever the viewed item changes.
    LaunchedEffect(status.id) {
        zoomScale = 1f
        panOffset = Offset.Zero
        val index = filmstripItems.indexOfFirst { it.id == status.id }
        if (index >= 0) {
            filmstripState.animateScrollToItem(index.coerceAtMost(filmstripItems.lastIndex).coerceAtLeast(0))
        }
    }

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
                }
            } else {
                RealVideoPlayer(status = status)
            }
        }

        // Minimal top bar — back + info, both circular glass buttons, no title text.
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
                            listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GlassIconButton(onClick = onBack, testTag = "preview_back_button") {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    GlassIconButton(onClick = { isInfoVisible = !isInfoVisible }, testTag = "preview_info_button") {
                        Icon(Icons.Default.Info, contentDescription = "Details", tint = Color.White)
                    }
                }
            }
        }

        // Details panel — title, timestamp, size, source, and caption — tucked
        // away behind the info icon instead of always overlaid on the image.
        AnimatedVisibility(
            visible = isInfoVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("preview_info_panel")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(status.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${status.timestamp} • ${status.sizeFormatted} • ${status.sourceLabel}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    if (status.caption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(status.caption, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }

        // Bottom cluster — thumbnail filmstrip above a flat icon action row,
        // matching Samsung Gallery's viewer layout.
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .navigationBarsPadding()
            ) {
                if (filmstripItems.size > 1) {
                    LazyRow(
                        state = filmstripState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .testTag("preview_filmstrip")
                    ) {
                        items(filmstripItems, key = { it.id }) { thumb ->
                            FilmstripThumbnail(
                                item = thumb,
                                isActive = thumb.id == status.id,
                                onClick = { onSelect(thumb) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomBarIcon(
                        icon = if (status.isVaulted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (status.isVaulted) "Remove from Vault" else "Add to Vault",
                        tint = if (status.isVaulted) extended.vaultAccent else Color.White,
                        onClick = { onToggleVault(status) },
                        testTag = "preview_vault_button"
                    )
                    BottomBarIcon(
                        icon = if (status.isSaved) Icons.Default.CheckCircle else Icons.Default.Download,
                        contentDescription = if (status.isSaved) "Saved" else "Save",
                        tint = if (status.isSaved) extended.success else Color.White,
                        onClick = { onSaveStatus(status.id) },
                        testTag = "preview_save_button"
                    )
                    BottomBarIcon(
                        icon = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        onClick = { onShare(status) },
                        testTag = "preview_share_button"
                    )
                    BottomBarIcon(
                        icon = Icons.Default.Repeat,
                        contentDescription = "Repost to Status",
                        tint = Color.White,
                        onClick = { onRepost(status) },
                        testTag = "preview_repost_button"
                    )
                    Box {
                        BottomBarIcon(
                            icon = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White,
                            onClick = { isMoreMenuVisible = true },
                            testTag = "preview_more_button"
                        )
                        DropdownMenu(expanded = isMoreMenuVisible, onDismissRequest = { isMoreMenuVisible = false }) {
                            DropdownMenuItem(
                                text = { Text("Details") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    isMoreMenuVisible = false
                                    isInfoVisible = !isInfoVisible
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open with...") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                                onClick = {
                                    isMoreMenuVisible = false
                                    openWithExternalApp(context, status)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassIconButton(onClick: () -> Unit, testTag: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun BottomBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    IconButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun FilmstripThumbnail(item: StatusItem, isActive: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val width = if (isActive) 56.dp else 48.dp

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isActive) {
                    Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.08f))
                }
            )
            .clickable(onClick = onClick)
            .testTag("preview_thumb_${item.id}")
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .videoFrameMillis(0)
                .crossfade(true)
                .build(),
            imageLoader = AppImageLoader.get(context),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (item.type == StatusType.VIDEO) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                item.videoDuration?.let {
                    Text(text = it, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** Hands the raw file off to any other app that can open it, via a real system chooser. */
private fun openWithExternalApp(context: android.content.Context, status: StatusItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(status.uri, status.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Open with"))
    }.onFailure {
        if (it is ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Couldn't open this file", Toast.LENGTH_SHORT).show()
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
