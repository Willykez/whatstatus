package com.willykez.wastatus.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willykez.wastatus.model.BottomNavTab

/**
 * A floating, pill-shaped nav bar — margin on every side, rounded fully,
 * elevated above the content — instead of a full-bleed classic bottom bar.
 * The selected item grows to reveal its label; unselected items stay
 * icon-only for a calmer, more focused strip.
 */
@Composable
fun WaStatusBottomNav(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    statusBadgeCount: Int = 0
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 10.dp,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .testTag("bottom_nav")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavItem(
                    tab = BottomNavTab.STATUS,
                    currentTab = currentTab,
                    icon = Icons.Default.PhotoCamera,
                    onClick = { onTabSelected(BottomNavTab.STATUS) },
                    testTag = "nav_status",
                    badgeCount = statusBadgeCount
                )
                NavItem(
                    tab = BottomNavTab.DIRECT_CHAT,
                    currentTab = currentTab,
                    icon = Icons.AutoMirrored.Filled.Chat,
                    onClick = { onTabSelected(BottomNavTab.DIRECT_CHAT) },
                    testTag = "nav_direct_chat"
                )
                NavItem(
                    tab = BottomNavTab.CLEANER,
                    currentTab = currentTab,
                    icon = Icons.Default.CleaningServices,
                    onClick = { onTabSelected(BottomNavTab.CLEANER) },
                    testTag = "nav_cleaner"
                )
                NavItem(
                    tab = BottomNavTab.SETTINGS,
                    currentTab = currentTab,
                    icon = Icons.Default.Settings,
                    onClick = { onTabSelected(BottomNavTab.SETTINGS) },
                    testTag = "nav_settings"
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    tab: BottomNavTab,
    currentTab: BottomNavTab,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    badgeCount: Int = 0
) {
    val isSelected = tab == currentTab

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isSelected) 16.dp else 12.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = if (badgeCount > 0) "${tab.title} ($badgeCount new)" else tab.title,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 7.dp, y = (-5).dp)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .testTag("${testTag}_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = expandHorizontally(animationSpec = tween(220)) + fadeIn(tween(220)),
            exit = shrinkHorizontally(animationSpec = tween(180)) + fadeOut(tween(140))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tab.title,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
