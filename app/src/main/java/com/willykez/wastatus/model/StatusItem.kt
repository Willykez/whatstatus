package com.willykez.wastatus.model

import android.net.Uri

enum class StatusType {
    IMAGE,
    VIDEO
}

enum class StatusTab {
    IMAGES,
    VIDEOS,
    SAVED,
    VAULT
}

enum class BottomNavTab(val title: String) {
    STATUS("Status"),
    DIRECT_CHAT("Direct Chat"),
    CLEANER("Cleaner"),
    SETTINGS("Settings")
}

enum class AppThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

/**
 * A real WhatsApp status file discovered on-device via the Storage Access
 * Framework. [id] is the stable SAF document URI string for the file, used
 * both as a list key and as the persistence key for the "saved" state.
 */
data class StatusItem(
    val id: String,
    val uri: Uri,
    val title: String,
    val timestamp: String,
    val lastModifiedMillis: Long,
    val type: StatusType,
    val mimeType: String,
    val sizeBytes: Long,
    val isSaved: Boolean = false,
    val isVaulted: Boolean = false,
    val videoDuration: String? = null,
    val caption: String = "",
    /** e.g. "Personal", "Personal · 1004", or "Business" — which linked WhatsApp account this came from. */
    val sourceLabel: String = "Personal"
) {
    val sizeFormatted: String
        get() = formatBytes(sizeBytes)
}

data class DirectChatMessage(
    val id: String,
    val phoneNumber: String,
    val messageText: String,
    val timestamp: String
)

data class CleanerCategory(
    val id: String,
    val title: String,
    val count: Int,
    val totalSizeBytes: Long,
    val iconName: String,
    val folderUri: Uri? = null
) {
    val totalSizeFormatted: String
        get() = formatBytes(totalSizeBytes)
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
