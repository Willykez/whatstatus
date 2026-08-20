package com.willykez.wastatus.ui.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willykez.wastatus.model.StatusItem
import com.willykez.wastatus.model.StatusTab
import com.willykez.wastatus.ui.theme.LocalExtendedColors

@Composable
fun StatusTabScreen(
    statuses: List<StatusItem>,
    currentTab: StatusTab,
    onTabSelected: (StatusTab) -> Unit,
    selectedIds: Set<String>,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onStatusClick: (StatusItem) -> Unit,
    onBatchDownload: () -> Unit,
    hasFolderAccess: Boolean,
    isLoading: Boolean,
    showSourceBadges: Boolean,
    availableSources: List<String>,
    activeSourceFilter: String?,
    onSourceFilterChanged: (String?) -> Unit,
    onRequestFolderAccess: () -> Unit
) {
    val isMultiSelectMode = selectedIds.isNotEmpty() && currentTab != StatusTab.VAULT

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SegmentedTabRow(currentTab = currentTab, onTabSelected = onTabSelected)

            if (availableSources.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeSourceFilter == null,
                        onClick = { onSourceFilterChanged(null) },
                        label = { Text("All", fontSize = 12.sp) },
                        modifier = Modifier.testTag("source_filter_all")
                    )
                    availableSources.forEach { source ->
                        FilterChip(
                            selected = activeSourceFilter == source,
                            onClick = { onSourceFilterChanged(source) },
                            label = { Text(source, fontSize = 12.sp) },
                            modifier = Modifier.testTag("source_filter_$source")
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isMultiSelectMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("multi_select_header")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onClearSelection, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear selection",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${selectedIds.size} selected",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        TextButton(onClick = onSelectAll, modifier = Modifier.testTag("btn_select_all")) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select All", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            when {
                !hasFolderAccess && currentTab != StatusTab.VAULT -> FolderAccessEmptyState(onRequestFolderAccess)
                isLoading && currentTab != StatusTab.VAULT -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                statuses.isEmpty() -> EmptyStatusState(currentTab)
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 130.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("status_grid")
                    ) {
                        items(statuses, key = { it.id }) { item ->
                            val isSelected = item.id in selectedIds
                            StatusCard(
                                status = item,
                                isSelected = isSelected,
                                isMultiSelectMode = isMultiSelectMode,
                                showSourceBadge = showSourceBadges,
                                onClick = {
                                    if (isMultiSelectMode) onToggleSelect(item.id) else onStatusClick(item)
                                },
                                onLongClick = { if (currentTab != StatusTab.VAULT) onToggleSelect(item.id) }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isMultiSelectMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
        ) {
            Button(
                onClick = onBatchDownload,
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_batch_download")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Download Selected (${selectedIds.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(
            visible = !isMultiSelectMode && statuses.isNotEmpty() && currentTab != StatusTab.SAVED && currentTab != StatusTab.VAULT,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 108.dp)
        ) {
            FloatingActionButton(
                onClick = onSelectAll,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(58.dp)
                    .testTag("fab_quick_select")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Select statuses to download",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/** A pill-style segmented control — replaces the plain underlined tab row. */
@Composable
private fun SegmentedTabRow(currentTab: StatusTab, onTabSelected: (StatusTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SegmentedTabItem("Images", currentTab == StatusTab.IMAGES, { onTabSelected(StatusTab.IMAGES) }, Modifier.weight(1f).testTag("tab_images"))
        SegmentedTabItem("Videos", currentTab == StatusTab.VIDEOS, { onTabSelected(StatusTab.VIDEOS) }, Modifier.weight(1f).testTag("tab_videos"))
        SegmentedTabItem("Saved", currentTab == StatusTab.SAVED, { onTabSelected(StatusTab.SAVED) }, Modifier.weight(1f).testTag("tab_saved"))
        SegmentedTabItem("Vault", currentTab == StatusTab.VAULT, { onTabSelected(StatusTab.VAULT) }, Modifier.weight(1f).testTag("tab_vault"))
    }
}

@Composable
private fun SegmentedTabItem(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(200),
        label = "segmentBg"
    )
    Box(
        modifier = modifier
            .shadow(elevation = if (isSelected) 2.dp else 0.dp, shape = RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FolderAccessEmptyState(onRequestFolderAccess: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GradientIconBadge(icon = Icons.Default.FolderOff)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Connect your WhatsApp folder",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "One grant is all it takes — WaStatus auto-detects every linked WhatsApp account underneath it, no manual folder digging.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestFolderAccess,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("btn_grant_status_access")
            ) {
                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect WhatsApp", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyStatusState(currentTab: StatusTab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GradientIconBadge(icon = Icons.Default.Inbox)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = when (currentTab) {
                    StatusTab.SAVED -> "No saved statuses yet"
                    StatusTab.VAULT -> "Your Vault is empty"
                    else -> "Nothing here right now"
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (currentTab) {
                    StatusTab.SAVED -> "Long-press any status or use batch download to save."
                    StatusTab.VAULT -> "Open a status and tap \"Add to Vault\" to keep it permanently, even after it disappears from WhatsApp."
                    else -> "Open WhatsApp Status to load some first."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

/** A soft gradient circular icon badge used across every empty state for a consistent, polished look. */
@Composable
private fun GradientIconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val extended = LocalExtendedColors.current
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        extended.vaultAccentContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
        )
    }
}
