package com.willykez.wastatus.ui.cleaner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.willykez.wastatus.model.CleanerCategory
import com.willykez.wastatus.model.CleanerFileItem
import com.willykez.wastatus.model.CleanerFileType
import com.willykez.wastatus.ui.theme.LocalExtendedColors
import com.willykez.wastatus.util.AppImageLoader

@Composable
fun CleanerScreen(
    categories: List<CleanerCategory>,
    hasFolderAccess: Boolean,
    isLoading: Boolean,
    onCleanCategory: (String) -> Unit,
    onRequestFolderAccess: () -> Unit,
    openCategory: CleanerCategory?,
    categoryFiles: List<CleanerFileItem>,
    isLoadingFiles: Boolean,
    onOpenCategory: (CleanerCategory) -> Unit,
    onCloseCategory: () -> Unit,
    onDeleteSelectedFiles: (categoryId: String, ids: Set<String>) -> Unit
) {
    if (openCategory != null) {
        CleanerCategoryDetailScreen(
            category = openCategory,
            files = categoryFiles,
            isLoading = isLoadingFiles,
            onBack = onCloseCategory,
            onDeleteSelected = { ids -> onDeleteSelectedFiles(openCategory.id, ids) }
        )
        return
    }

    val extended = LocalExtendedColors.current
    val totalBytes = categories.sumOf { it.totalSizeBytes }
    val totalFormatted = com.willykez.wastatus.model.formatBytes(totalBytes)
    val maxCategoryBytes = (categories.maxOfOrNull { it.totalSizeBytes } ?: 0L).coerceAtLeast(1L)
    var pendingClean by remember { mutableStateOf<CleanerCategory?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Hero card — a bold gradient banner with the headline "freeable space" number.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, extended.vaultAccent)
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Storage Cleaner",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = if (hasFolderAccess) totalFormatted else "—",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (hasFolderAccess) {
                        "of real WhatsApp media can be freed on this device"
                    } else {
                        "Grant access to WhatsApp's media folder to scan real usage"
                    },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        if (!hasFolderAccess) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No folder connected yet",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pick your WhatsApp app folder once — WaStatus finds every linked account's cache automatically, no manual navigating.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRequestFolderAccess,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(50.dp).testTag("btn_grant_media_access")
                    ) {
                        Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect WhatsApp", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Text(
                text = "Storage Categories",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap a category to preview and choose files individually",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories, key = { it.id }) { category ->
                    CleanerCategoryCard(
                        category = category,
                        fillFraction = (category.totalSizeBytes.toFloat() / maxCategoryBytes.toFloat()).coerceIn(0f, 1f),
                        onOpen = { if (category.count > 0) onOpenCategory(category) },
                        onClean = { if (category.count > 0) pendingClean = category }
                    )
                }
            }
        }
    }

    pendingClean?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingClean = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Delete ${category.title.lowercase()}?") },
            text = {
                Text("This permanently deletes ${category.count} file(s) (${category.totalSizeFormatted}) from this device. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onCleanCategory(category.id)
                    pendingClean = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingClean = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CleanerCategoryCard(
    category: CleanerCategory,
    fillFraction: Float,
    onOpen: () -> Unit,
    onClean: () -> Unit
) {
    val extended = LocalExtendedColors.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(enabled = category.count > 0, onClick = onOpen)
            .testTag("cleaner_category_${category.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (category.iconName) {
                            "image" -> Icons.Default.Image
                            "video" -> Icons.Default.VideoLibrary
                            "mic" -> Icons.Default.Mic
                            else -> Icons.Default.Folder
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = category.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (category.count == 0) "Cleaned • 0 KB" else "${category.count} files • ${category.totalSizeFormatted}",
                            color = if (category.count == 0) extended.success else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }

                Button(
                    onClick = onClean,
                    enabled = category.count > 0,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_clean_${category.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (category.count == 0) "Done" else "Clean all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Relative-size bar — communicates which category dominates storage at a glance.
            LinearProgressIndicator(
                progress = { fillFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (category.count == 0) extended.success else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}

/**
 * File-by-file view of one category — every real file gets a thumbnail
 * (or type icon for audio/documents), a tap to preview it full-screen, and
 * a checkbox to mark it for deletion. Nothing is removed until the person
 * explicitly confirms which selected files to delete.
 */
@Composable
private fun CleanerCategoryDetailScreen(
    category: CleanerCategory,
    files: List<CleanerFileItem>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onDeleteSelected: (Set<String>) -> Unit
) {
    var selectedIds by remember(category.id) { mutableStateOf(setOf<String>()) }
    var previewFile by remember { mutableStateOf<CleanerFileItem?>(null) }
    var pendingDelete by remember { mutableStateOf(false) }
    val allSelected = files.isNotEmpty() && selectedIds.size == files.size

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("cleaner_detail_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(category.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${files.size} file(s)" + if (selectedIds.isNotEmpty()) " • ${selectedIds.size} selected" else "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (files.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            selectedIds = if (allSelected) emptySet() else files.map { it.id }.toSet()
                        },
                        modifier = Modifier.testTag("cleaner_select_all")
                    ) {
                        Text(if (allSelected) "Deselect All" else "Select All")
                    }
                }
            }

            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                files.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Nothing here to clean", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().testTag("cleaner_file_grid")
                ) {
                    gridItems(files, key = { it.id }) { file ->
                        CleanerFileTile(
                            file = file,
                            isSelected = file.id in selectedIds,
                            isSelectionMode = selectedIds.isNotEmpty(),
                            onToggleSelect = {
                                selectedIds = if (file.id in selectedIds) selectedIds - file.id else selectedIds + file.id
                            },
                            onPreview = { previewFile = file }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedIds.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Button(
                onClick = { pendingDelete = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_delete_selected_files")
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Delete Selected (${selectedIds.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Delete ${selectedIds.size} file(s)?") },
            text = { Text("This permanently deletes the selected file(s) from this device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSelected(selectedIds)
                    selectedIds = emptySet()
                    pendingDelete = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) { Text("Cancel") }
            }
        )
    }

    previewFile?.let { file ->
        CleanerFilePreviewDialog(
            file = file,
            isSelected = file.id in selectedIds,
            onToggleSelect = {
                selectedIds = if (file.id in selectedIds) selectedIds - file.id else selectedIds + file.id
            },
            onDeleteThis = {
                onDeleteSelected(setOf(file.id))
                selectedIds = selectedIds - file.id
                previewFile = null
            },
            onDismiss = { previewFile = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CleanerFileTile(
    file: CleanerFileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onPreview: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("cleaner_file_${file.id}")
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() else onPreview() },
                onLongClick = onToggleSelect
            )
    ) {
        when (file.type) {
            CleanerFileType.IMAGE, CleanerFileType.GIF, CleanerFileType.VIDEO -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(file.uri)
                        .videoFrameMillis(0)
                        .crossfade(true)
                        .build(),
                    imageLoader = AppImageLoader.get(context),
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (file.type == CleanerFileType.VIDEO) {
                    Box(
                        modifier = Modifier.align(Alignment.Center).size(28.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
            CleanerFileType.AUDIO -> FileTypeIconBackground(icon = Icons.Default.Mic)
            CleanerFileType.DOCUMENT -> FileTypeIconBackground(icon = Icons.Default.Description)
        }

        // Selection checkbox — always tappable so a person can multi-select
        // without needing a long-press first.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onToggleSelect),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Select",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Surface(
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                text = file.sizeFormatted,
                color = Color.White,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun FileTypeIconBackground(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
    }
}

/** Full-screen preview for one Cleaner file — decide right here to keep it or delete it. */
@Composable
private fun CleanerFilePreviewDialog(
    file: CleanerFileItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onDeleteThis: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 96.dp), contentAlignment = Alignment.Center) {
                when (file.type) {
                    CleanerFileType.IMAGE, CleanerFileType.GIF -> AsyncImage(
                        model = ImageRequest.Builder(context).data(file.uri).crossfade(true).build(),
                        imageLoader = AppImageLoader.get(context),
                        contentDescription = file.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    CleanerFileType.VIDEO, CleanerFileType.AUDIO -> CleanerMediaPlayer(uri = file.uri)
                    CleanerFileType.DOCUMENT -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(file.name, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { openWithExternalApp(context, file) }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open with...")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Text(file.name, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${file.sizeFormatted} • ${file.sourceLabel}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onToggleSelect,
                        modifier = Modifier.weight(1f).testTag("cleaner_preview_keep_toggle"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(if (isSelected) "Marked for deletion" else "Keep")
                    }
                    Button(
                        onClick = onDeleteThis,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).testTag("cleaner_preview_delete_button")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

/** Minimal Media3 player used for previewing a video or audio Cleaner file before deciding its fate. */
@Composable
private fun CleanerMediaPlayer(uri: android.net.Uri) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    DisposableEffect(uri) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = true } },
        modifier = Modifier.fillMaxWidth().height(280.dp)
    )
}

/** Hands the raw file off to any other app that can open it, via a real system chooser. */
private fun openWithExternalApp(context: android.content.Context, file: CleanerFileItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(file.uri, file.mimeType)
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
