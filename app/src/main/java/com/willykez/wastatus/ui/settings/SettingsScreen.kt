package com.willykez.wastatus.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willykez.wastatus.model.AppThemeMode
import com.willykez.wastatus.ui.theme.LocalExtendedColors

@Composable
fun SettingsScreen(
    autoSaveEnabled: Boolean,
    onAutoSaveChanged: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsChanged: (Boolean) -> Unit,
    autoDetectEnabled: Boolean,
    onAutoDetectChanged: (Boolean) -> Unit,
    hasNotificationAccess: Boolean,
    onOpenNotificationAccessSettings: () -> Unit,
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorChanged: (Boolean) -> Unit,
    personalConnected: Boolean,
    businessConnected: Boolean,
    onConnectPersonal: () -> Unit,
    onConnectBusiness: () -> Unit
) {
    val extended = LocalExtendedColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Settings",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Folders, background behavior, and appearance",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }

        item {
            SettingsGroup(title = "Folders") {
                SettingsRow(
                    title = "WhatsApp (Personal)",
                    subtitle = if (personalConnected) {
                        "Connected — auto-detects every linked account, tap to change"
                    } else {
                        "Not connected — tap to grant one-time access"
                    },
                    icon = Icons.Default.Folder,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconContainer = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onConnectPersonal,
                    testTag = "settings_personal_folder",
                    trailingDot = if (personalConnected) extended.success else null
                )
                SettingsDivider()
                SettingsRow(
                    title = "WhatsApp Business",
                    subtitle = if (businessConnected) "Connected — tap to change" else "Optional — tap to connect a Business account",
                    icon = Icons.Default.Folder,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconContainer = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onConnectBusiness,
                    testTag = "settings_business_folder",
                    trailingDot = if (businessConnected) extended.success else null
                )
            }
        }

        item {
            SettingsGroup(title = "Behavior") {
                SettingsSwitchRow(
                    title = "Auto-Save New Statuses",
                    subtitle = "Automatically download viewed statuses to your gallery",
                    icon = Icons.Default.SaveAlt,
                    iconTint = extended.success,
                    iconContainer = extended.successContainer,
                    checked = autoSaveEnabled,
                    onCheckedChange = onAutoSaveChanged
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = "Status Alerts",
                    subtitle = "Notify when new contacts post a status",
                    icon = Icons.Default.Notifications,
                    iconTint = extended.warning,
                    iconContainer = extended.warningContainer,
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsChanged
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = "Auto-Detect New Statuses",
                    subtitle = "Rescan in the background on a WhatsApp notification — never reads its content, only which app it came from",
                    icon = Icons.Default.NotificationsActive,
                    iconTint = extended.warning,
                    iconContainer = extended.warningContainer,
                    checked = autoDetectEnabled,
                    onCheckedChange = onAutoDetectChanged
                )
                if (autoDetectEnabled && !hasNotificationAccess) {
                    SettingsDivider()
                    SettingsRow(
                        title = "Notification Access Required",
                        subtitle = "Auto-Detect needs a one-time system permission — tap to grant it",
                        icon = Icons.Default.Notifications,
                        iconTint = MaterialTheme.colorScheme.error,
                        iconContainer = MaterialTheme.colorScheme.errorContainer,
                        onClick = onOpenNotificationAccessSettings,
                        testTag = "settings_notification_access"
                    )
                }
            }
        }

        item {
            SettingsGroup(title = "Appearance") {
                SettingsSwitchRow(
                    title = "Dynamic Color (Material You)",
                    subtitle = "Match the app's palette to your wallpaper on Android 12+",
                    icon = Icons.Default.Palette,
                    iconTint = extended.vaultAccent,
                    iconContainer = extended.vaultAccentContainer,
                    checked = dynamicColorEnabled,
                    onCheckedChange = onDynamicColorChanged
                )
                SettingsDivider()
                ThemeModeRow(themeMode = themeMode, onThemeModeChanged = onThemeModeChanged)
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "WaStatus v2.1.0",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "com.willykez.wastatus — Status Saver & Batch Downloader\nReads media only from folders you explicitly grant.",
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        item {
            // Deliberately plain/neutral (not gradient) — a legal notice should
            // read as sober, not decorative.
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "WaStatus is an independent, unofficial tool and is not affiliated with, " +
                        "endorsed by, sponsored by, or in any way officially connected to WhatsApp LLC, " +
                        "Meta Platforms, Inc., or any of their subsidiaries or affiliates. " +
                        "\"WhatsApp\" and the WhatsApp logo are trademarks of WhatsApp LLC. " +
                        "All product and company names are trademarks™ or registered® trademarks of " +
                        "their respective holders. Use of these names does not imply endorsement.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("trademark_disclaimer")
                )
            }
        }
    }
}

/** A titled card grouping several related rows — the standard "grouped settings list" layout. */
@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        modifier = Modifier.padding(start = 68.dp)
    )
}

@Composable
private fun ThemeModeRow(themeMode: AppThemeMode, onThemeModeChanged: (AppThemeMode) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAvatar(icon = Icons.Default.Brightness4, tint = MaterialTheme.colorScheme.primary, container = MaterialTheme.colorScheme.primaryContainer)
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "Theme",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AppThemeMode.entries.forEach { mode ->
                val isSelected = mode == themeMode
                FilterChip(
                    selected = isSelected,
                    onClick = { onThemeModeChanged(mode) },
                    label = { Text(mode.label, fontSize = 12.sp) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("theme_mode_${mode.name}")
                )
            }
        }
    }
}

@Composable
private fun IconAvatar(icon: ImageVector, tint: Color, container: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconContainer: Color,
    onClick: () -> Unit,
    testTag: String,
    trailingDot: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconAvatar(icon = icon, tint = iconTint, container = iconContainer)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }
        if (trailingDot != null) {
            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(trailingDot))
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconContainer: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            IconAvatar(icon = icon, tint = iconTint, container = iconContainer)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
