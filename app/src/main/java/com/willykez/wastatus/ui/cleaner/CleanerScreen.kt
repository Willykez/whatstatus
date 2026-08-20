package com.willykez.wastatus.ui.cleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willykez.wastatus.model.CleanerCategory
import com.willykez.wastatus.ui.theme.LocalExtendedColors

@Composable
fun CleanerScreen(
    categories: List<CleanerCategory>,
    hasFolderAccess: Boolean,
    isLoading: Boolean,
    onCleanCategory: (String) -> Unit,
    onRequestFolderAccess: () -> Unit
) {
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
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Storage Cleaner",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = if (hasFolderAccess) totalFormatted else "—",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (hasFolderAccess) {
                        "of real WhatsApp media can be freed on this device"
                    } else {
                        "Grant access to WhatsApp's media folder to scan real usage"
                    },
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
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
                        text = if (category.count == 0) "Done" else "Clean",
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
