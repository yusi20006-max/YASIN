package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.di.AppContainer
import java.util.concurrent.TimeUnit

class YasinPressAutoSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer(applicationContext)
        return try {
            container.pressPipeline.runCollectionAndPublish("زمان‌بندی خودکار ساعتی")
            Result.success()
        } catch (e: Exception) {
            container.logRepository.log(
                module = "Yasin Press Auto",
                level = "ERROR",
                title = "خطا در اجرای زمان‌بند ساعتی",
                details = e.localizedMessage ?: "خطای غیرمنتظره در Worker"
            )
            Result.retry()
        }
    }
}

object YasinPressScheduler {
    private const val UNIQUE_WORK_NAME = "yasin_press_hourly_sync"

    fun syncSchedule(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<YasinPressAutoSyncWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
