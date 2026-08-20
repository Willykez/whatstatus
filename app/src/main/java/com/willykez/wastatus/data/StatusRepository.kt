package com.willykez.wastatus.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.willykez.wastatus.model.AppThemeMode
import com.willykez.wastatus.model.CleanerCategory
import com.willykez.wastatus.model.DirectChatMessage
import com.willykez.wastatus.model.StatusItem
import com.willykez.wastatus.model.StatusTab
import com.willykez.wastatus.model.StatusType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

data class BatchDownloadSummary(
    val totalCount: Int,
    val newlySavedCount: Int,
    val totalBytesSaved: Long,
    val savedIds: List<String>
)

/**
 * One real file the Cleaner removed from WhatsApp's folder — but not
 * actually gone yet. [cacheFile] holds a byte-for-byte copy in the app's
 * private cache, and [originalParentFolder] is the live [DocumentFile] for
 * the exact folder it came from, kept as an object (not just its URI) since
 * that's what makes writing it back with [DocumentFile.createFile] work
 * correctly even for a nested subfolder, not just a SAF tree root.
 */
data class CleanBackupEntry(
    val originalParentFolder: DocumentFile,
    val fileName: String,
    val mimeType: String,
    val cacheFile: File
)

/** A just-performed Clean that can still be undone, or finalized once the undo window passes. */
data class PendingCleanBackup(
    val categoryId: String,
    val categoryTitle: String,
    val entries: List<CleanBackupEntry>
)

/** Named, real subfolders inside WhatsApp's Media directory the Cleaner scans. */
private val CLEANER_FOLDERS = listOf(
    Triple("images", "Photos Received & Sent", "WhatsApp Images"),
    Triple("videos", "Videos Received & Sent", "WhatsApp Video"),
    Triple("voice", "Voice Notes", "WhatsApp Voice Notes"),
    Triple("docs", "Documents", "WhatsApp Documents"),
    Triple("gifs", "Animated GIFs", "WhatsApp Animated Gifs")
)

private fun iconForCategory(id: String): String = when (id) {
    "images" -> "image"
    "videos" -> "video"
    "voice" -> "mic"
    else -> "folder"
}

/**
 * Single source of truth for real, on-device data. Nothing here is mocked:
 * status listings come from the user-granted SAF folder, saved state is
 * tracked in DataStore, gallery saves go through MediaStore, and cleaner
 * stats/deletes operate on the actual WhatsApp media folder.
 */
class StatusRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = PreferencesManager(appContext)

    private val _statuses = MutableStateFlow<List<StatusItem>>(emptyList())
    val statuses: StateFlow<List<StatusItem>> = _statuses.asStateFlow()

    private val _cleanerCategories = MutableStateFlow<List<CleanerCategory>>(emptyList())
    val cleanerCategories: StateFlow<List<CleanerCategory>> = _cleanerCategories.asStateFlow()

    private val _directChatHistory = MutableStateFlow<List<DirectChatMessage>>(emptyList())
    val directChatHistory: StateFlow<List<DirectChatMessage>> = _directChatHistory.asStateFlow()

    private val _vaultItems = MutableStateFlow<List<StatusItem>>(emptyList())
    val vaultItems: StateFlow<List<StatusItem>> = _vaultItems.asStateFlow()

    private val _isLoadingStatuses = MutableStateFlow(false)
    val isLoadingStatuses: StateFlow<Boolean> = _isLoadingStatuses.asStateFlow()

    private val _isLoadingCleaner = MutableStateFlow(false)
    val isLoadingCleaner: StateFlow<Boolean> = _isLoadingCleaner.asStateFlow()

    val whatsappRootUri = prefs.whatsappRootUri
    val whatsappBusinessRootUri = prefs.whatsappBusinessRootUri
    val autoSaveEnabled = prefs.autoSaveEnabled
    val notificationsEnabled = prefs.notificationsEnabled
    val themeMode = prefs.themeMode
    val dynamicColorEnabled = prefs.dynamicColorEnabled
    val onboardingComplete = prefs.onboardingComplete
    val autoDetectEnabled = prefs.autoDetectEnabled
    val newStatusBadgeCount = prefs.newStatusBadgeCount

    fun getStatusesByTab(tab: StatusTab): List<StatusItem> {
        val current = _statuses.value
        return when (tab) {
            StatusTab.IMAGES -> current.filter { it.type == StatusType.IMAGE }
            StatusTab.VIDEOS -> current.filter { it.type == StatusType.VIDEO }
            StatusTab.SAVED -> current.filter { it.isSaved }
            StatusTab.VAULT -> _vaultItems.value
        }
    }

    suspend fun setWhatsAppRoot(uri: Uri?) {
        prefs.setWhatsAppRootUri(uri)
        refreshStatuses()
        refreshCleanerCategories()
    }

    suspend fun setWhatsAppBusinessRoot(uri: Uri?) {
        prefs.setWhatsAppBusinessRootUri(uri)
        refreshStatuses()
        refreshCleanerCategories()
    }

    suspend fun setAutoSaveEnabled(value: Boolean) = prefs.setAutoSaveEnabled(value)
    suspend fun setNotificationsEnabled(value: Boolean) = prefs.setNotificationsEnabled(value)
    suspend fun setThemeMode(mode: AppThemeMode) = prefs.setThemeMode(mode)
    suspend fun setDynamicColorEnabled(value: Boolean) = prefs.setDynamicColorEnabled(value)
    suspend fun setOnboardingCompleted() = prefs.setOnboardingComplete(true)
    suspend fun setAutoDetectEnabled(value: Boolean) = prefs.setAutoDetectEnabled(value)
    suspend fun setNewStatusBadge(count: Int) = prefs.setNewStatusBadgeCount(count)
    suspend fun clearNewStatusBadge() = prefs.setNewStatusBadgeCount(0)

    /** Every account (personal, linked, and business) currently reachable through granted, still-live SAF permissions. */
    private suspend fun liveAccountRoots(): List<WhatsAppAccountRoot> {
        val roots = mutableListOf<WhatsAppAccountRoot>()
        prefs.whatsappRootUri.first()?.let { uri ->
            if (SafUtils.hasLivePermission(appContext, uri)) {
                roots += SafUtils.discoverAccountRoots(appContext, uri, "Personal")
            }
        }
        prefs.whatsappBusinessRootUri.first()?.let { uri ->
            if (SafUtils.hasLivePermission(appContext, uri)) {
                roots += SafUtils.discoverAccountRoots(appContext, uri, "Business")
            }
        }
        return roots
    }

    /** Re-scans every granted account's `.Statuses` folder from disk — legacy layout and every linked account. Safe to call repeatedly. */
    suspend fun refreshStatuses() = withContext(Dispatchers.IO) {
        _isLoadingStatuses.value = true
        try {
            val savedIds = prefs.currentSavedStatusIds()
            val vaultedOriginalIds = currentVaultedOriginalIds()
            val accountRoots = liveAccountRoots()
            if (accountRoots.isEmpty()) {
                _statuses.value = emptyList()
                return@withContext
            }

            val items = mutableListOf<StatusItem>()
            for (account in accountRoots) {
                val statusesFolder = SafUtils.statusesFolderFor(account.mediaFolder) ?: continue
                val files = statusesFolder.listFiles().filter { doc ->
                    doc.isFile && doc.name?.let { SafUtils.statusTypeFromName(it) != null } == true
                }
                files.forEach { doc -> documentToStatusItem(doc, savedIds, vaultedOriginalIds, account.label)?.let { items += it } }
            }
            _statuses.value = items.sortedByDescending { it.lastModifiedMillis }
        } finally {
            _isLoadingStatuses.value = false
        }
    }

    /** The set of original status IDs (not the permanent vault copy's own ID) already vaulted — lets the source card show a "Vault" badge and lets [addToVault] avoid duplicate copies. */
    private suspend fun currentVaultedOriginalIds(): Set<String> {
        val json = prefs.vaultItemsJson.first()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                array.getJSONObject(i).optString("originalId").takeIf { it.isNotBlank() }
            }.toSet()
        }.getOrDefault(emptySet())
    }

    private fun documentToStatusItem(doc: DocumentFile, savedIds: Set<String>, vaultedOriginalIds: Set<String>, sourceLabel: String): StatusItem? {
        val name = doc.name ?: return null
        val type = SafUtils.statusTypeFromName(name) ?: return null
        val id = doc.uri.toString()
        val duration = if (type == StatusType.VIDEO) extractVideoDuration(doc.uri) else null
        return StatusItem(
            id = id,
            uri = doc.uri,
            title = SafUtils.titleFromFileName(name),
            timestamp = SafUtils.friendlyTimestamp(doc.lastModified()),
            lastModifiedMillis = doc.lastModified(),
            type = type,
            mimeType = doc.type ?: SafUtils.mimeTypeFromName(name),
            sizeBytes = doc.length(),
            isSaved = id in savedIds,
            isVaulted = id in vaultedOriginalIds,
            videoDuration = duration,
            caption = SafUtils.titleFromFileName(name),
            sourceLabel = sourceLabel
        )
    }

    private fun extractVideoDuration(uri: Uri): String? {
        val retriever = android.media.MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(appContext, uri)
            val millis = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: return null
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }.also {
            runCatching { retriever.release() }
        }.getOrNull()
    }

    /** Re-scans every granted account's media folders to compute real, combined storage usage. */
    suspend fun refreshCleanerCategories() = withContext(Dispatchers.IO) {
        _isLoadingCleaner.value = true
        try {
            val accountRoots = liveAccountRoots()
            if (accountRoots.isEmpty()) {
                _cleanerCategories.value = emptyList()
                return@withContext
            }

            _cleanerCategories.value = CLEANER_FOLDERS.map { (id, title, folderName) ->
                var count = 0
                var bytes = 0L
                accountRoots.forEach { account ->
                    val folder = SafUtils.childFolder(account.mediaFolder, folderName)
                    val sentFolder = SafUtils.childFolder(folder, "Sent")
                    val (topCount, topBytes) = SafUtils.folderStats(folder)
                    val (sentCount, sentBytes) = SafUtils.folderStats(sentFolder)
                    count += topCount + sentCount
                    bytes += topBytes + sentBytes
                }
                CleanerCategory(
                    id = id,
                    title = title,
                    count = count,
                    totalSizeBytes = bytes,
                    iconName = iconForCategory(id)
                )
            }
        } finally {
            _isLoadingCleaner.value = false
        }
    }

    /** Saves one status into the public gallery via MediaStore and marks it saved. */
    suspend fun saveStatus(id: String): Boolean = withContext(Dispatchers.IO) {
        val item = _statuses.value.find { it.id == id } ?: return@withContext false
        if (item.isSaved) return@withContext false
        val fileName = buildSavedFileName(item)
        val result = MediaSaver.saveToGallery(appContext, item.uri, fileName, item.mimeType, item.type)
        if (result != null) {
            prefs.markSaved(id)
            refreshStatuses()
            true
        } else {
            false
        }
    }

    /** Batch-saves multiple selected statuses into the gallery. */
    suspend fun saveStatusesBatch(ids: Set<String>): BatchDownloadSummary = withContext(Dispatchers.IO) {
        val targets = _statuses.value.filter { it.id in ids }
        var newlySavedCount = 0
        var totalBytes = 0L
        val savedIds = mutableListOf<String>()

        for (item in targets) {
            totalBytes += item.sizeBytes
            if (!item.isSaved) {
                val fileName = buildSavedFileName(item)
                val result = MediaSaver.saveToGallery(appContext, item.uri, fileName, item.mimeType, item.type)
                if (result != null) {
                    newlySavedCount++
                    savedIds.add(item.id)
                }
            } else {
                savedIds.add(item.id)
            }
        }
        if (savedIds.isNotEmpty()) prefs.markSavedBatch(savedIds)
        refreshStatuses()

        BatchDownloadSummary(
            totalCount = targets.size,
            newlySavedCount = newlySavedCount,
            totalBytesSaved = totalBytes,
            savedIds = savedIds
        )
    }

    /**
     * The Vault is a permanent, keep-forever collection — distinct from
     * "Saved" because WhatsApp status media vanishes from the source
     * `.Statuses` folder after ~24h. Vaulting copies the file into its own
     * "WaStatus Vault" gallery album (so it survives independently) and
     * tracks the permanent copy's own MediaStore URI in DataStore.
     */
    suspend fun loadVault() = withContext(Dispatchers.IO) {
        val json = prefs.vaultItemsJson.first()
        val entries = runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i -> array.getJSONObject(i) }
        }.getOrDefault(emptyList())

        val items = entries.mapNotNull { obj ->
            runCatching {
                val uriString = obj.getString("id")
                val uri = Uri.parse(uriString)
                // Confirm the permanent copy is still actually there before showing it.
                appContext.contentResolver.openInputStream(uri)?.close()
                StatusItem(
                    id = uriString,
                    uri = uri,
                    title = obj.getString("title"),
                    timestamp = SafUtils.friendlyTimestamp(obj.optLong("savedAtMillis", 0L)),
                    lastModifiedMillis = obj.optLong("savedAtMillis", 0L),
                    type = if (obj.getString("type") == "VIDEO") StatusType.VIDEO else StatusType.IMAGE,
                    mimeType = obj.getString("mimeType"),
                    sizeBytes = obj.optLong("sizeBytes", 0L),
                    isSaved = true,
                    isVaulted = true,
                    videoDuration = obj.optString("videoDuration", "").takeIf { it.isNotBlank() },
                    caption = obj.getString("title"),
                    sourceLabel = obj.optString("sourceLabel", "Vault")
                )
            }.getOrNull()
        }
        _vaultItems.value = items.sortedByDescending { it.lastModifiedMillis }
    }

    /** Copies a status into the permanent Vault album. Returns true if it was newly added (false if already vaulted or the item wasn't found). */
    suspend fun addToVault(id: String): Boolean = withContext(Dispatchers.IO) {
        if (id in currentVaultedOriginalIds() || _vaultItems.value.any { it.id == id }) return@withContext false
        val item = _statuses.value.find { it.id == id } ?: _vaultItems.value.find { it.id == id } ?: return@withContext false
        val fileName = "WaStatusVault_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.${
            item.uri.lastPathSegment?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
                ?: if (item.type == StatusType.VIDEO) "mp4" else "jpg"
        }"
        val permanentUri = MediaSaver.saveToGallery(
            appContext, item.uri, fileName, item.mimeType, item.type, albumName = MediaSaver.VAULT_ALBUM_NAME
        ) ?: return@withContext false

        val entry = JSONObject().apply {
            put("id", permanentUri.toString())
            put("originalId", item.id)
            put("title", item.title)
            put("mimeType", item.mimeType)
            put("type", item.type.name)
            put("sizeBytes", item.sizeBytes)
            put("savedAtMillis", System.currentTimeMillis())
            put("videoDuration", item.videoDuration ?: "")
            put("sourceLabel", item.sourceLabel)
        }
        val json = prefs.vaultItemsJson.first()
        val array = runCatching { JSONArray(json) }.getOrDefault(JSONArray())
        array.put(entry)
        prefs.setVaultItemsJson(array.toString())
        loadVault()
        refreshStatuses()
        true
    }

    /** Permanently deletes a Vault entry's own copy — does not touch the original WhatsApp status. */
    suspend fun removeFromVault(vaultId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { appContext.contentResolver.delete(Uri.parse(vaultId), null, null) }

        val json = prefs.vaultItemsJson.first()
        val array = runCatching { JSONArray(json) }.getOrDefault(JSONArray())
        val kept = JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.optString("id") != vaultId) kept.put(obj)
        }
        prefs.setVaultItemsJson(kept.toString())
        loadVault()
        refreshStatuses()
        true
    }

    private fun buildSavedFileName(item: StatusItem): String {
        val ext = item.uri.lastPathSegment?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
            ?: if (item.type == StatusType.VIDEO) "mp4" else "jpg"
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        return "WaStatus_$stamp.$ext"
    }

    /**
     * Removes every real file in a cleaner category's folder(s), across
     * every granted account — but backs each one up to the app's private
     * cache first, so [undoClean] can restore them if the person taps
     * "Undo" on the confirmation snackbar. Nothing is truly, unrecoverably
     * gone until [commitCleanBackup] runs (once the undo window expires).
     */
    suspend fun cleanCategory(categoryId: String): PendingCleanBackup? = withContext(Dispatchers.IO) {
        val accountRoots = liveAccountRoots()
        if (accountRoots.isEmpty()) return@withContext null
        val category = CLEANER_FOLDERS.find { it.first == categoryId } ?: return@withContext null
        val (_, categoryTitle, folderName) = category

        val backupDir = File(appContext.cacheDir, "pending_clean/$categoryId").apply { mkdirs() }
        val entries = mutableListOf<CleanBackupEntry>()

        accountRoots.forEach { account ->
            val folder = SafUtils.childFolder(account.mediaFolder, folderName)
            val sentFolder = SafUtils.childFolder(folder, "Sent")
            listOfNotNull(folder, sentFolder).forEach { dir ->
                dir.listFiles().filter { it.isFile }.forEach { doc ->
                    backUpThenDelete(doc, dir, backupDir)?.let { entries += it }
                }
            }
        }

        refreshCleanerCategories()
        if (entries.isEmpty()) null else PendingCleanBackup(categoryId, categoryTitle, entries)
    }

    /** Backs up one file's bytes to the cache, deletes the real one, and only reports success if BOTH steps worked. */
    private fun backUpThenDelete(doc: DocumentFile, parentFolder: DocumentFile, backupDir: File): CleanBackupEntry? {
        val name = doc.name ?: return null
        val mimeType = doc.type ?: SafUtils.mimeTypeFromName(name)
        val cacheFile = File(backupDir, "${System.nanoTime()}_$name")

        val backedUp = runCatching {
            appContext.contentResolver.openInputStream(doc.uri)?.use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            } != null
        }.getOrDefault(false)
        if (!backedUp) {
            cacheFile.delete()
            return null
        }

        if (!doc.delete()) {
            // Real file survives untouched — the cache copy was never needed.
            cacheFile.delete()
            return null
        }

        return CleanBackupEntry(parentFolder, name, mimeType, cacheFile)
    }

    /** Restores every file in a backup back to its original folder. Returns true only if every file came back. */
    suspend fun undoClean(backup: PendingCleanBackup): Boolean = withContext(Dispatchers.IO) {
        var allRestored = true
        backup.entries.forEach { entry ->
            val restored = runCatching {
                val newDoc = entry.originalParentFolder.createFile(entry.mimeType, entry.fileName) ?: return@runCatching false
                appContext.contentResolver.openOutputStream(newDoc.uri)?.use { output ->
                    entry.cacheFile.inputStream().use { input -> input.copyTo(output) }
                } != null
            }.getOrDefault(false)
            if (restored) entry.cacheFile.delete() else allRestored = false
        }
        refreshCleanerCategories()
        allRestored
    }

    /** Called once the undo window has passed without the person tapping Undo — reclaims the temporary cache backup. */
    fun commitCleanBackup(backup: PendingCleanBackup) {
        backup.entries.forEach { it.cacheFile.delete() }
    }

    suspend fun loadChatHistory() = withContext(Dispatchers.IO) {
        val json = prefs.chatHistoryJson.first()
        val list = runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                DirectChatMessage(
                    id = obj.getString("id"),
                    phoneNumber = obj.getString("phoneNumber"),
                    messageText = obj.optString("messageText", ""),
                    timestamp = obj.getString("timestamp")
                )
            }
        }.getOrDefault(emptyList())
        _directChatHistory.value = list
    }

    suspend fun addDirectChatMessage(phoneNumber: String, messageText: String) = withContext(Dispatchers.IO) {
        val newMessage = DirectChatMessage(
            id = System.currentTimeMillis().toString(),
            phoneNumber = phoneNumber,
            messageText = messageText,
            timestamp = SafUtils.friendlyTimestamp(System.currentTimeMillis())
        )
        val updated = listOf(newMessage) + _directChatHistory.value
        _directChatHistory.value = updated
        persistChatHistory(updated)
    }

    private suspend fun persistChatHistory(list: List<DirectChatMessage>) {
        val array = JSONArray()
        list.take(100).forEach { msg ->
            val obj = JSONObject()
            obj.put("id", msg.id)
            obj.put("phoneNumber", msg.phoneNumber)
            obj.put("messageText", msg.messageText)
            obj.put("timestamp", msg.timestamp)
            array.put(obj)
        }
        prefs.setChatHistoryJson(array.toString())
    }
}
