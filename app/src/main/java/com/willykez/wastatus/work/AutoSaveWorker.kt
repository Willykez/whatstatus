package com.willykez.wastatus.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.willykez.wastatus.data.StatusRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Real background work — not just a foreground refresh. Re-scans every
 * granted WhatsApp folder from disk and, when "Auto-Save New Statuses" is
 * on in Settings, saves anything not already saved into the gallery. Runs
 * whether or not the app is open, via either the periodic schedule below or
 * a one-time trigger from [com.willykez.wastatus.notifications.WaStatusNotificationListener].
 */
class AutoSaveWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = StatusRepository(applicationContext)
            repository.refreshStatuses()

            val autoSaveOn = repository.autoSaveEnabled.first()
            var savedCount = 0
            val unsavedIds = repository.statuses.value.filter { !it.isSaved }.map { it.id }.toSet()
            if (autoSaveOn && unsavedIds.isNotEmpty()) {
                val summary = repository.saveStatusesBatch(unsavedIds)
                savedCount = summary.newlySavedCount
            }

            // Badge reflects "statuses waiting for you" (unsaved right now) rather than a
            // fragile cross-run diff — each worker run is a fresh, stateless instance.
            val triggeredByListener = inputData.getBoolean(KEY_FROM_LISTENER, false)
            if (triggeredByListener && !autoSaveOn) {
                repository.setNewStatusBadge(unsavedIds.size)
            }

            Result.success(workDataOf(KEY_SAVED_COUNT to savedCount))
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC_NAME = "wastatus_auto_save_periodic"
        private const val UNIQUE_ONE_TIME_NAME = "wastatus_auto_save_one_time"
        const val KEY_FROM_LISTENER = "from_listener"
        const val KEY_SAVED_COUNT = "saved_count"

        /** Schedules (or re-schedules) the ~15-minute background scan. WorkManager's minimum periodic interval is 15 minutes. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoSaveWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_NAME)
        }

        /** Fired immediately when the notification listener sees a new WhatsApp notification. */
        fun triggerOneTimeFromListener(context: Context) {
            val request = OneTimeWorkRequestBuilder<AutoSaveWorker>()
                .setInputData(workDataOf(KEY_FROM_LISTENER to true))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
