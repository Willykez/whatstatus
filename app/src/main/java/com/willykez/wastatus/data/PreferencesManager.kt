package com.willykez.wastatus.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.willykez.wastatus.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wastatus_prefs")

/**
 * Durable, real app state — replaces every hardcoded/mock default in the
 * original build. Backed by Jetpack DataStore so choices (granted folders,
 * saved-status ledger, theme, toggles, direct-chat history) survive process
 * death and app restarts.
 */
class PreferencesManager(private val context: Context) {

    private object Keys {
        val WHATSAPP_ROOT_URI = stringPreferencesKey("whatsapp_root_uri")
        val WHATSAPP_BUSINESS_ROOT_URI = stringPreferencesKey("whatsapp_business_root_uri")
        val SAVED_STATUS_IDS = stringSetPreferencesKey("saved_status_ids")
        val AUTO_SAVE = booleanPreferencesKey("auto_save_enabled")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val CHAT_HISTORY_JSON = stringPreferencesKey("direct_chat_history_json")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val AUTO_DETECT_ENABLED = booleanPreferencesKey("auto_detect_enabled")
        val VAULT_ITEMS_JSON = stringPreferencesKey("vault_items_json")
        val NEW_STATUS_BADGE_COUNT = androidx.datastore.preferences.core.intPreferencesKey("new_status_badge_count")
    }

    /** Grant covering `Android/media/com.whatsapp/WhatsApp` — the primary app plus every linked account underneath it. */
    val whatsappRootUri: Flow<Uri?> = context.dataStore.data.map { prefs ->
        prefs[Keys.WHATSAPP_ROOT_URI]?.let { Uri.parse(it) }
    }

    /** Grant covering `Android/media/com.whatsapp.w4b/WhatsApp Business`. */
    val whatsappBusinessRootUri: Flow<Uri?> = context.dataStore.data.map { prefs ->
        prefs[Keys.WHATSAPP_BUSINESS_ROOT_URI]?.let { Uri.parse(it) }
    }

    val savedStatusIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.SAVED_STATUS_IDS] ?: emptySet()
    }

    val autoSaveEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_SAVE] ?: false
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS] ?: true
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { raw ->
            runCatching { AppThemeMode.valueOf(raw) }.getOrDefault(AppThemeMode.SYSTEM)
        } ?: AppThemeMode.SYSTEM
    }

    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLOR] ?: true
    }

    val chatHistoryJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.CHAT_HISTORY_JSON] ?: "[]"
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    /** Whether the user has opted in to notification-triggered background rescans (still requires the system-level notification-access grant to actually fire). */
    val autoDetectEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_DETECT_ENABLED] ?: false
    }

    val vaultItemsJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.VAULT_ITEMS_JSON] ?: "[]"
    }

    /** How many statuses were auto-detected since the user last opened the Status tab. */
    val newStatusBadgeCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.NEW_STATUS_BADGE_COUNT] ?: 0
    }

    suspend fun setWhatsAppRootUri(uri: Uri?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(Keys.WHATSAPP_ROOT_URI)
            else prefs[Keys.WHATSAPP_ROOT_URI] = uri.toString()
        }
    }

    suspend fun setWhatsAppBusinessRootUri(uri: Uri?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(Keys.WHATSAPP_BUSINESS_ROOT_URI)
            else prefs[Keys.WHATSAPP_BUSINESS_ROOT_URI] = uri.toString()
        }
    }

    suspend fun markSaved(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAVED_STATUS_IDS] ?: emptySet()
            prefs[Keys.SAVED_STATUS_IDS] = current + id
        }
    }

    suspend fun markSavedBatch(ids: Collection<String>) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAVED_STATUS_IDS] ?: emptySet()
            prefs[Keys.SAVED_STATUS_IDS] = current + ids
        }
    }

    suspend fun setAutoSaveEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_SAVE] = value }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.NOTIFICATIONS] = value }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColorEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.DYNAMIC_COLOR] = value }
    }

    suspend fun setChatHistoryJson(json: String) {
        context.dataStore.edit { prefs -> prefs[Keys.CHAT_HISTORY_JSON] = json }
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.ONBOARDING_COMPLETE] = value }
    }

    suspend fun setAutoDetectEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_DETECT_ENABLED] = value }
    }

    suspend fun setVaultItemsJson(json: String) {
        context.dataStore.edit { prefs -> prefs[Keys.VAULT_ITEMS_JSON] = json }
    }

    suspend fun setNewStatusBadgeCount(count: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.NEW_STATUS_BADGE_COUNT] = count }
    }

    suspend fun incrementNewStatusBadgeCount(by: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.NEW_STATUS_BADGE_COUNT] ?: 0
            prefs[Keys.NEW_STATUS_BADGE_COUNT] = current + by
        }
    }

    suspend fun currentSavedStatusIds(): Set<String> = savedStatusIds.first()
}
