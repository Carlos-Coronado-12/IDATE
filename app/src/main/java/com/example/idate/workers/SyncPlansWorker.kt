package com.example.idate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.idate.data.repository.IDateRepository
import com.example.idate.notifications.NotificationHelper
import java.util.concurrent.TimeUnit

class SyncPlansWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = IDateRepository(applicationContext)
            repository.initializeDefaultDataIfNeeded()

            val notificationHelper = NotificationHelper(applicationContext)
            // Optionally notify user of sync success if needed
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val SYNC_WORK_NAME = "IDatePeriodicSyncWork"

        fun schedulePeriodicSync(context: Context) {
            val syncRequest = PeriodicWorkRequestBuilder<SyncPlansWorker>(
                12, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
