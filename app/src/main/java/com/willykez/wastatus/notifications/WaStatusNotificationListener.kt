package com.willykez.wastatus.notifications

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.willykez.wastatus.data.PreferencesManager
import com.willykez.wastatus.work.AutoSaveWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Opt-in "new status" detector. This intentionally does NOT read any
 * notification text/content — it only checks which app a notification came
 * from ([StatusBarNotification.packageName]) and, if that's WhatsApp or
 * WhatsApp Business, triggers a real background re-scan via [AutoSaveWorker].
 * No message content is inspected, stored, or transmitted; this exists
 * purely as a "something happened over there, go look" trigger, and only
 * runs at all once the user has both flipped "Auto-Detect New Statuses" on
 * in Settings AND granted system-level Notification Access to this app.
 */
class WaStatusNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (sbn.packageName != PACKAGE_WHATSAPP && sbn.packageName != PACKAGE_WHATSAPP_BUSINESS) return

        val appContext = applicationContext
        serviceScope.launch {
            val prefs = PreferencesManager(appContext)
            val enabled = prefs.autoDetectEnabled.first()
            if (enabled) {
                AutoSaveWorker.triggerOneTimeFromListener(appContext)
            }
        }
    }

    companion object {
        private const val PACKAGE_WHATSAPP = "com.whatsapp"
        private const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

        /** Whether this app currently holds the system-level notification-access grant. */
        fun hasNotificationAccess(context: Context): Boolean {
            return NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }
    }
}
