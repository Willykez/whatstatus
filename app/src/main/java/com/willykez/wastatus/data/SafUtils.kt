package com.willykez.wastatus.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.willykez.wastatus.model.StatusType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * One discovered WhatsApp account "Media" folder — either the classic
 * single-account layout (`WhatsApp/Media`) or one of the newer per-account
 * folders WhatsApp creates for each linked account
 * (`WhatsApp/accounts/<id>/Media`). [label] is what the UI shows the user;
 * [mediaFolder] is the account's real `Media` directory, the parent of both
 * `.Statuses` and the `WhatsApp Images` / `WhatsApp Video` / etc. folders
 * the Cleaner scans.
 */
data class WhatsAppAccountRoot(
    val label: String,
    val mediaFolder: DocumentFile
)

/**
 * Helpers for real on-device access to WhatsApp's media folders via the
 * Storage Access Framework. No storage permission is required — the user
 * grants a persistable, scoped folder URI once (at the app-level "WhatsApp"
 * folder, not the deeply nested status folder) and WaStatus auto-discovers
 * every account underneath it, including linked accounts added through
 * WhatsApp's own multi-account feature.
 */
object SafUtils {

    // One-time grant targets: the *app-level* folder, not `.Statuses` or
    // `Media` directly. Granting here also grants every folder underneath —
    // `Media/.Statuses`, `accounts/<id>/Media/.Statuses`, and the Cleaner's
    // `WhatsApp Images` / `WhatsApp Video` / etc. subfolders for every
    // linked account — from a single picker trip.
    const val WHATSAPP_APP_ROOT_PATH = "Android/media/com.whatsapp/WhatsApp"
    const val WHATSAPP_BUSINESS_APP_ROOT_PATH = "Android/media/com.whatsapp.w4b/WhatsApp Business"

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    private val VIDEO_EXTENSIONS = setOf("mp4", "3gp", "mkv", "webm")

    /**
     * Builds a URI hint for the given relative path on the primary storage
     * volume. Pass this as the input to Activity Result API's
     * [androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree],
     * which uses it to jump the picker straight to that folder on Android 8+
     * (EXTRA_INITIAL_URI). If the OEM picker doesn't honor it, it simply
     * falls back to its normal root — never a hard failure.
     */
    fun buildInitialUri(relativePath: String): Uri? = runCatching {
        DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:$relativePath"
        )
    }.getOrNull()

    fun takePersistablePermission(context: Context, uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
    }

    fun releasePersistablePermission(context: Context, uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.releasePersistableUriPermission(uri, flags) }
    }

    /** True if we still hold a live, persisted grant for [uri]. */
    fun hasLivePermission(context: Context, uri: Uri): Boolean {
        return context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
    }

    /** Direct, case-insensitive child directory lookup — no recursion, no globbing surprises. */
    fun childFolder(parent: DocumentFile?, name: String): DocumentFile? =
        parent?.listFiles()?.firstOrNull { it.isDirectory && it.name.equals(name, ignoreCase = true) }

    /**
     * Walks a granted "WhatsApp" (or "WhatsApp Business") app folder and
     * returns one [WhatsAppAccountRoot] per account found underneath it:
     * the classic primary account at `Media/`, plus one entry per linked
     * account at `accounts/<id>/Media/`. Robust to either folder being
     * absent (older WhatsApp versions have no `accounts/` folder at all;
     * a fresh multi-account setup may lack a top-level `Media/` if the
     * user's first account was also migrated into `accounts/`).
     */
    fun discoverAccountRoots(context: Context, appRootTreeUri: Uri, sourceLabel: String): List<WhatsAppAccountRoot> {
        val appRoot = DocumentFile.fromTreeUri(context, appRootTreeUri) ?: return emptyList()
        return discoverAccountRootsFrom(appRoot, sourceLabel)
    }

    /**
     * Same discovery logic as [discoverAccountRoots], but operating on an
     * already-resolved [DocumentFile] instead of a SAF tree [Uri]. Split out
     * so it can be exercised directly in unit tests against a real,
     * temp-directory-backed [DocumentFile.fromFile] tree — no SAF grant,
     * Context, or MediaStore involved — while production code still goes
     * through [discoverAccountRoots] for the real SAF-permission path.
     */
    fun discoverAccountRootsFrom(appRoot: DocumentFile, sourceLabel: String): List<WhatsAppAccountRoot> {
        val roots = mutableListOf<WhatsAppAccountRoot>()

        childFolder(appRoot, "Media")?.let { roots += WhatsAppAccountRoot(sourceLabel, it) }

        childFolder(appRoot, "accounts")?.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { accountFolder ->
                childFolder(accountFolder, "Media")?.let { media ->
                    val accountName = accountFolder.name?.takeIf { it.isNotBlank() } ?: "Linked"
                    val label = if (roots.isEmpty()) sourceLabel else "$sourceLabel · $accountName"
                    roots += WhatsAppAccountRoot(label, media)
                }
            }

        return roots
    }

    /** The `.Statuses` folder inside an account's `Media` directory, if present. */
    fun statusesFolderFor(mediaFolder: DocumentFile): DocumentFile? = childFolder(mediaFolder, ".Statuses")

    fun statusTypeFromName(name: String): StatusType? {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
        return when {
            ext in IMAGE_EXTENSIONS -> StatusType.IMAGE
            ext in VIDEO_EXTENSIONS -> StatusType.VIDEO
            else -> null
        }
    }

    fun mimeTypeFromName(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "3gp" -> "video/3gpp"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            else -> "application/octet-stream"
        }
    }

    fun folderStats(folder: DocumentFile?): Pair<Int, Long> {
        if (folder == null) return 0 to 0L
        val files = folder.listFiles().filter { it.isFile }
        return files.size to files.sumOf { it.length() }
    }

    fun friendlyTimestamp(millis: Long): String {
        if (millis <= 0L) return ""
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = millis }
        val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val wasYesterday = yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        return when {
            sameDay -> timeFmt.format(millis)
            wasYesterday -> "Yesterday"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(millis)
        }
    }

    fun titleFromFileName(name: String): String {
        val base = name.substringBeforeLast('.')
        return base.replace('_', ' ').replace('-', ' ').trim().ifBlank { "WhatsApp Status" }
    }
}
