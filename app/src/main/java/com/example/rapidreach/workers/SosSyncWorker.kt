package com.example.rapidreach.workers

import android.content.Context
import androidx.work.*
import com.example.rapidreach.data.repository.SosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SosSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val repository = SosRepository(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val unsyncedLogs = repository.getUnsyncedLogs()
            if (unsyncedLogs.isEmpty()) return@withContext Result.success()

            unsyncedLogs.forEach { log ->
                repository.uploadAndMarkSynced(log)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SosSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
