package com.willykez.wastatus

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.willykez.wastatus.data.SafUtils
import com.willykez.wastatus.data.StatusRepository
import com.willykez.wastatus.model.AppThemeMode
import com.willykez.wastatus.model.BottomNavTab
import com.willykez.wastatus.model.StatusItem
import com.willykez.wastatus.model.StatusTab
import com.willykez.wastatus.notifications.WaStatusNotificationListener
import com.willykez.wastatus.ui.chat.DirectChatScreen
import com.willykez.wastatus.ui.cleaner.CleanerScreen
import com.willykez.wastatus.ui.header.WaStatusHeader
import com.willykez.wastatus.ui.navigation.WaStatusBottomNav
import com.willykez.wastatus.ui.onboarding.OnboardingScreen
import com.willykez.wastatus.ui.preview.StatusPreviewScreen
import com.willykez.wastatus.ui.settings.SettingsScreen
import com.willykez.wastatus.ui.status.StatusTabScreen
import com.willykez.wastatus.ui.theme.WaStatusTheme
import com.willykez.wastatus.work.AutoSaveWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repository by lazy { StatusRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by repository.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
            val dynamicColorEnabled by repository.dynamicColorEnabled.collectAsState(initial = true)

            WaStatusTheme(themeMode = themeMode, dynamicColor = dynamicColorEnabled) {
                WaStatusApp(repository = repository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaStatusApp(repository: StatusRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allStatuses by repository.statuses.collectAsState()
    val vaultItems by repository.vaultItems.collectAsState()
    val directChatHistory by repository.directChatHistory.collectAsState()
    val cleanerCategories by repository.cleanerCategories.collectAsState()
    val isLoadingStatuses by repository.isLoadingStatuses.collectAsState()
    val isLoadingCleaner by repository.isLoadingCleaner.collectAsState()
    val cleanerFiles by repository.cleanerFiles.collectAsState()
    val isLoadingCleanerFiles by repository.isLoadingCleanerFiles.collectAsState()
    var openCleanerCategory by remember { mutableStateOf<com.willykez.wastatus.model.CleanerCategory?>(null) }

    val whatsappRootUri by repository.whatsappRootUri.collectAsState(initial = null)
    val whatsappBusinessRootUri by repository.whatsappBusinessRootUri.collectAsState(initial = null)
    val autoSaveEnabled by repository.autoSaveEnabled.collectAsState(initial = false)
    val notificationsEnabled by repository.notificationsEnabled.collectAsState(initial = true)
    val autoDetectEnabled by repository.autoDetectEnabled.collectAsState(initial = false)
    val themeMode by repository.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
    val dynamicColorEnabled by repository.dynamicColorEnabled.collectAsState(initial = true)
    val onboardingComplete by repository.onboardingComplete.collectAsState(initial = true)
    val newStatusBadgeCount by repository.newStatusBadgeCount.collectAsState(initial = 0)

    // Either grant is enough to power both the Status and Cleaner tabs — each
    // one auto-discovers every linked account underneath it.
    val hasAnyFolderAccess = whatsappRootUri != null || whatsappBusinessRootUri != null
    // Only bother labeling cards by account once there's more than one real source to tell apart.
    val showSourceBadges = remember(allStatuses) { allStatuses.map { it.sourceLabel }.distinct().size > 1 }
    // Top-level sources ("Personal" / "Business") for the filter chip row — collapsed past the "· accountId" suffix.
    val availableTopSources = remember(allStatuses) {
        allStatuses.map { it.sourceLabel.substringBefore(" · ") }.distinct()
    }

    var currentNavTab by remember { mutableStateOf(BottomNavTab.STATUS) }
    var currentStatusTab by remember { mutableStateOf(StatusTab.IMAGES) }
    var activeSourceFilter by remember { mutableStateOf<String?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var previewStatusId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var hasNotificationAccess by remember { mutableStateOf(WaStatusNotificationListener.hasNotificationAccess(context)) }
    val snackbarHostState = remember { SnackbarHostState() }

    val previewStatus = remember(previewStatusId, allStatuses, vaultItems) {
        previewStatusId?.let { id -> (allStatuses + vaultItems).find { it.id == id } }
    }

    // Load real data once, then re-scan whenever the granted folders change.
    LaunchedEffect(Unit) {
        repository.loadChatHistory()
        repository.loadVault()
        repository.refreshStatuses()
        repository.refreshCleanerCategories()
    }
    LaunchedEffect(whatsappRootUri, whatsappBusinessRootUri) {
        repository.refreshStatuses()
        repository.refreshCleanerCategories()
    }

    // Real background auto-save: schedules (or cancels) a genuine WorkManager
    // periodic job the moment the Settings toggle changes — not just a
    // foreground-only refresh.
    LaunchedEffect(autoSaveEnabled) {
        if (autoSaveEnabled) AutoSaveWorker.schedulePeriodic(context) else AutoSaveWorker.cancelPeriodic(context)
    }

    // Real refresh-on-resume: coming back from WhatsApp (after viewing new
    // statuses) or from the system Notification Access settings screen
    // automatically re-syncs everything — no manual pull.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = WaStatusNotificationListener.hasNotificationAccess(context)
                scope.launch {
                    repository.refreshStatuses()
                    repository.refreshCleanerCategories()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Real SAF folder pickers — each grants the top-level "WhatsApp" (or
    // "WhatsApp Business") app folder, which auto-discovers every linked
    // account underneath it (legacy `Media/` and every `accounts/<id>/Media/`).
    val personalRootLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            SafUtils.takePersistablePermission(context, uri)
            scope.launch { repository.setWhatsAppRoot(uri) }
            Toast.makeText(context, "WhatsApp connected — scanning all linked accounts", Toast.LENGTH_SHORT).show()
        }
    }
    val businessRootLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            SafUtils.takePersistablePermission(context, uri)
            scope.launch { repository.setWhatsAppBusinessRoot(uri) }
            Toast.makeText(context, "WhatsApp Business connected", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = previewStatus != null || selectedIds.isNotEmpty() || isSearchActive || openCleanerCategory != null) {
        when {
            previewStatus != null -> previewStatusId = null
            selectedIds.isNotEmpty() -> selectedIds = emptySet()
            isSearchActive -> {
                isSearchActive = false
                searchQuery = ""
            }
            openCleanerCategory != null -> {
                openCleanerCategory = null
                repository.clearCleanerFiles()
            }
        }
    }

    val filteredStatuses = remember(allStatuses, vaultItems, currentStatusTab, searchQuery, activeSourceFilter) {
        val tabList = repository.getStatusesByTab(currentStatusTab)
        val sourceFiltered = if (activeSourceFilter == null) {
            tabList
        } else {
            tabList.filter { it.sourceLabel.substringBefore(" · ") == activeSourceFilter }
        }
        if (searchQuery.isBlank()) {
            sourceFiltered
        } else {
            sourceFiltered.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.caption.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (!onboardingComplete) {
        OnboardingScreen(onGetStarted = { scope.launch { repository.setOnboardingCompleted() } })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    WaStatusHeader(
                        onMenuClick = {
                            Toast.makeText(context, "WaStatus • Real status saver, no placeholders", Toast.LENGTH_SHORT).show()
                        },
                        onSearchClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                        onMoreClick = {
                            scope.launch {
                                repository.refreshStatuses()
                                repository.refreshCleanerCategories()
                            }
                            Toast.makeText(context, "Refreshed", Toast.LENGTH_SHORT).show()
                        }
                    )

                    AnimatedVisibility(visible = isSearchActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search statuses by title or caption...") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_input")
                            )
                        }
                    }
                }
            },
            bottomBar = {
                WaStatusBottomNav(
                    currentTab = currentNavTab,
                    onTabSelected = { tab ->
                        currentNavTab = tab
                        if (tab != BottomNavTab.STATUS) selectedIds = emptySet()
                        if (tab == BottomNavTab.STATUS && newStatusBadgeCount > 0) {
                            scope.launch { repository.clearNewStatusBadge() }
                        }
                    },
                    statusBadgeCount = newStatusBadgeCount
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentNavTab) {
                    BottomNavTab.STATUS -> {
                        StatusTabScreen(
                            statuses = filteredStatuses,
                            currentTab = currentStatusTab,
                            onTabSelected = { tab ->
                                currentStatusTab = tab
                                selectedIds = emptySet()
                            },
                            selectedIds = selectedIds,
                            onToggleSelect = { statusId ->
                                selectedIds = if (statusId in selectedIds) selectedIds - statusId else selectedIds + statusId
                            },
                            onSelectAll = {
                                selectedIds = if (selectedIds.size == filteredStatuses.size) {
                                    emptySet()
                                } else {
                                    filteredStatuses.map { it.id }.toSet()
                                }
                            },
                            onClearSelection = { selectedIds = emptySet() },
                            onStatusClick = { statusItem -> previewStatusId = statusItem.id },
                            onBatchDownload = {
                                if (selectedIds.isEmpty()) return@StatusTabScreen
                                val idsToSave = selectedIds
                                scope.launch {
                                    val summary = repository.saveStatusesBatch(idsToSave)
                                    Toast.makeText(
                                        context,
                                        "Saved ${summary.newlySavedCount} of ${summary.totalCount} status(es) to Gallery",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                selectedIds = emptySet()
                            },
                            hasFolderAccess = hasAnyFolderAccess,
                            isLoading = isLoadingStatuses,
                            showSourceBadges = showSourceBadges,
                            availableSources = availableTopSources,
                            activeSourceFilter = activeSourceFilter,
                            onSourceFilterChanged = { activeSourceFilter = it },
                            onRequestFolderAccess = {
                                personalRootLauncher.launch(SafUtils.buildInitialUri(SafUtils.WHATSAPP_APP_ROOT_PATH))
                            }
                        )
                    }

                    BottomNavTab.DIRECT_CHAT -> {
                        DirectChatScreen(
                            history = directChatHistory,
                            onSendMessage = { phone, msg ->
                                scope.launch { repository.addDirectChatMessage(phone, msg) }
                            }
                        )
                    }

                    BottomNavTab.CLEANER -> {
                        CleanerScreen(
                            categories = cleanerCategories,
                            hasFolderAccess = hasAnyFolderAccess,
                            isLoading = isLoadingCleaner,
                            onCleanCategory = { catId ->
                                scope.launch {
                                    val backup = repository.cleanCategory(catId)
                                    if (backup == null) {
                                        Toast.makeText(context, "Nothing to clean right now", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val fileWord = if (backup.entries.size == 1) "file" else "files"
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Cleared ${backup.entries.size} $fileWord from ${backup.categoryTitle}",
                                            actionLabel = "Undo",
                                            withDismissAction = true,
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            val restored = repository.undoClean(backup)
                                            Toast.makeText(
                                                context,
                                                if (restored) "Restored" else "Some files couldn't be restored",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            // Undo window passed without a tap — the backup copy is no longer needed.
                                            repository.commitCleanBackup(backup)
                                        }
                                    }
                                }
                            },
                            onRequestFolderAccess = {
                                personalRootLauncher.launch(SafUtils.buildInitialUri(SafUtils.WHATSAPP_APP_ROOT_PATH))
                            },
                            openCategory = openCleanerCategory,
                            categoryFiles = cleanerFiles,
                            isLoadingFiles = isLoadingCleanerFiles,
                            onOpenCategory = { category ->
                                openCleanerCategory = category
                                scope.launch { repository.listCategoryFiles(category.id) }
                            },
                            onCloseCategory = {
                                openCleanerCategory = null
                                repository.clearCleanerFiles()
                            },
                            onDeleteSelectedFiles = { catId, ids ->
                                scope.launch {
                                    val backup = repository.deleteCleanerFiles(catId, ids)
                                    if (backup == null) {
                                        Toast.makeText(context, "Nothing deleted", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val fileWord = if (backup.entries.size == 1) "file" else "files"
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Deleted ${backup.entries.size} $fileWord from ${backup.categoryTitle}",
                                            actionLabel = "Undo",
                                            withDismissAction = true,
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            val restored = repository.undoClean(backup)
                                            Toast.makeText(
                                                context,
                                                if (restored) "Restored" else "Some files couldn't be restored",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            repository.commitCleanBackup(backup)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    BottomNavTab.SETTINGS -> {
                        SettingsScreen(
                            autoSaveEnabled = autoSaveEnabled,
                            onAutoSaveChanged = { scope.launch { repository.setAutoSaveEnabled(it) } },
                            notificationsEnabled = notificationsEnabled,
                            onNotificationsChanged = { scope.launch { repository.setNotificationsEnabled(it) } },
                            autoDetectEnabled = autoDetectEnabled,
                            onAutoDetectChanged = { scope.launch { repository.setAutoDetectEnabled(it) } },
                            hasNotificationAccess = hasNotificationAccess,
                            onOpenNotificationAccessSettings = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                }.onFailure {
                                    Toast.makeText(context, "Couldn't open notification access settings", Toast.LENGTH_SHORT).show()
                                }
                            },
                            themeMode = themeMode,
                            onThemeModeChanged = { scope.launch { repository.setThemeMode(it) } },
                            dynamicColorEnabled = dynamicColorEnabled,
                            onDynamicColorChanged = { scope.launch { repository.setDynamicColorEnabled(it) } },
                            personalConnected = whatsappRootUri != null,
                            businessConnected = whatsappBusinessRootUri != null,
                            onConnectPersonal = {
                                personalRootLauncher.launch(SafUtils.buildInitialUri(SafUtils.WHATSAPP_APP_ROOT_PATH))
                            },
                            onConnectBusiness = {
                                businessRootLauncher.launch(SafUtils.buildInitialUri(SafUtils.WHATSAPP_BUSINESS_APP_ROOT_PATH))
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = previewStatus != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            previewStatus?.let { status ->
                StatusPreviewScreen(
                    status = status,
                    filmstripItems = filteredStatuses,
                    onBack = { previewStatusId = null },
                    onSelect = { item -> previewStatusId = item.id },
                    onSaveStatus = { id ->
                        scope.launch {
                            val saved = repository.saveStatus(id)
                            Toast.makeText(
                                context,
                                if (saved) "Saved to Gallery!" else "Already in Saved Gallery",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onShare = { item -> shareStatus(context, item, restrictToWhatsApp = false) },
                    onRepost = { item -> shareStatus(context, item, restrictToWhatsApp = true) },
                    onToggleVault = { item ->
                        scope.launch {
                            val nowVaulted = if (item.isVaulted) {
                                repository.removeFromVault(item.id)
                                false
                            } else {
                                repository.addToVault(item.id)
                                true
                            }
                            Toast.makeText(
                                context,
                                if (nowVaulted) "Added to Vault — kept forever" else "Removed from Vault",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Shares a real status file. When [restrictToWhatsApp] is true (the
 * "Repost to Status" action) this jumps straight into WhatsApp's own share
 * target — which includes "Status" as an option — instead of the generic
 * chooser, falling back to the chooser if that specific package can't
 * handle it (e.g. WhatsApp not installed).
 */
private fun shareStatus(context: android.content.Context, item: StatusItem, restrictToWhatsApp: Boolean) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType
        putExtra(Intent.EXTRA_STREAM, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    if (restrictToWhatsApp) {
        val targetPackage = if (item.sourceLabel.startsWith("Business")) "com.whatsapp.w4b" else "com.whatsapp"
        sendIntent.setPackage(targetPackage)
        try {
            context.startActivity(sendIntent)
            return
        } catch (_: ActivityNotFoundException) {
            sendIntent.setPackage(null)
        }
    }

    runCatching {
        context.startActivity(Intent.createChooser(sendIntent, "Share ${item.title}"))
    }.onFailure {
        Toast.makeText(context, "Couldn't open the share sheet", Toast.LENGTH_SHORT).show()
    }
}
