package com.willykez.wastatus.ui.status

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.willykez.wastatus.model.StatusItem
import com.willykez.wastatus.model.StatusType
import com.willykez.wastatus.ui.theme.LocalExtendedColors
import com.willykez.wastatus.util.AppImageLoader

private val CardShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusCard(
    status: StatusItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    showSourceBadge: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val extended = LocalExtendedColors.current

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.94f else 1f,
        animationSpec = tween(180),
        label = "cardScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(180),
        label = "cardBorder"
    )

    Box(
        modifier = Modifier
            .aspectRatio(0.82f)
            .scale(scale)
            .shadow(elevation = if (isSelected) 0.dp else 4.dp, shape = CardShape, clip = false)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(width = 3.dp, color = borderColor, shape = CardShape)
            .testTag("status_card_${status.id}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Real thumbnail: a decoded image, or an extracted video frame.
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(status.uri)
                .videoFrameMillis(0)
                .crossfade(true)
                .build(),
            imageLoader = AppImageLoader.get(context),
            contentDescription = status.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Layered scrim — dark at both edges for legible text, clear through
        // the middle so the actual photo/video frame still reads as the hero.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.28f to Color.Black.copy(alpha = 0.05f),
                        0.72f to Color.Black.copy(alpha = 0.05f),
                        1f to Color.Black.copy(alpha = 0.65f)
                    )
                )
        )

        // Selection scrim — a soft tinted wash the moment a card is picked.
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
            )
        }

        // Top-left file title / size
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = status.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = status.sizeFormatted,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
        }

        // Video play badge — soft glass circle with a subtle ring.
        if (status.type == StatusType.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.38f))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video status",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom row: timestamp/duration pill on the left, source badge (if any) on the right.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (status.type == StatusType.VIDEO && status.videoDuration != null) {
                        "${status.timestamp} • ${status.videoDuration}"
                    } else {
                        status.timestamp
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (showSourceBadge) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = status.sourceLabel,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Top-right multi-select checkmark OR saved/vault badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            when {
                isMultiSelectMode -> {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f))
                            .border(1.5.dp, Color.White.copy(alpha = if (isSelected) 0f else 0.8f), CircleShape)
                            .testTag("select_checkbox_${status.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                status.isVaulted -> StatusPillBadge(
                    icon = Icons.Default.Lock,
                    label = "Vault",
                    containerColor = extended.vaultAccentContainer,
                    contentColor = extended.onVaultAccentContainer
                )
                status.isSaved -> StatusPillBadge(
                    icon = Icons.Default.CheckCircle,
                    label = "Saved",
                    containerColor = extended.successContainer,
                    contentColor = extended.onSuccessContainer
                )
            }
        }
    }
}

@Composable
private fun StatusPillBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(color = containerColor, shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = label, color = contentColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}
