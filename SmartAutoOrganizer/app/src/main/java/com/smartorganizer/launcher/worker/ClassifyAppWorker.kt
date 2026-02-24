package com.smartorganizer.launcher.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartorganizer.launcher.data.local.db.entity.AppEntity
import com.smartorganizer.launcher.data.repository.AppRepository
import com.smartorganizer.launcher.domain.usecase.ClassifyAppUseCase
import com.smartorganizer.launcher.domain.usecase.OrganizeFoldersUseCase
import com.smartorganizer.launcher.scanner.AppScanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ClassifyAppWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appScanner: AppScanner,
    private val classifyAppUseCase: ClassifyAppUseCase,
    private val appRepository: AppRepository,
    private val organizeFoldersUseCase: OrganizeFoldersUseCase
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_ACTION = "action"
        const val ACTION_ADD = "add"
        const val ACTION_REMOVE = "remove"
        const val ACTION_REPLACE = "replace"
    }

    override suspend fun doWork(): Result {
        val packageName = inputData.getString(KEY_PACKAGE_NAME) ?: return Result.failure()
        val action = inputData.getString(KEY_ACTION) ?: return Result.failure()

        return try {
            when (action) {
                ACTION_ADD, ACTION_REPLACE -> handleAddOrReplace(packageName)
                ACTION_REMOVE -> handleRemove(packageName)
                else -> Result.failure()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun handleAddOrReplace(packageName: String): Result {
        val apps = appScanner.scanInstalledApps()
        val targetApp = apps.find { it.packageName == packageName } ?: return Result.success()

        val existing = appRepository.getByPackageName(packageName)
        val entity = AppEntity(
            packageName = targetApp.packageName,
            appName = targetApp.appName,
            folderId = existing?.folderId,
            category = if (existing?.isManualOverride == true) existing.category else targetApp.category,
            isManualOverride = existing?.isManualOverride ?: false,
            installTime = targetApp.installTime,
            confidenceScore = targetApp.confidenceScore
        )
        appRepository.insertApp(entity)

        // Re-organize folders if needed
        val allApps = mutableListOf<AppEntity>()
        allApps.add(entity)
        organizeFoldersUseCase(allApps)

        return Result.success()
    }

    private suspend fun handleRemove(packageName: String): Result {
        appRepository.deleteByPackageName(packageName)
        return Result.success()
    }
}
