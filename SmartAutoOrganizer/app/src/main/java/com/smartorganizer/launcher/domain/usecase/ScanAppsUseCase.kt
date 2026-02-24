package com.smartorganizer.launcher.domain.usecase

import com.smartorganizer.launcher.data.local.db.entity.AppEntity
import com.smartorganizer.launcher.data.repository.AppRepository
import com.smartorganizer.launcher.scanner.AppScanner
import javax.inject.Inject

class ScanAppsUseCase @Inject constructor(
    private val appScanner: AppScanner,
    private val appRepository: AppRepository
) {
    /**
     * Scans all installed apps, persists them to Room, and returns the entity list.
     * Existing manual overrides are preserved.
     */
    suspend operator fun invoke(): List<AppEntity> {
        val scannedApps = appScanner.scanInstalledApps()

        val entities = scannedApps.map { appInfo ->
            // Preserve manual overrides if app already exists in DB
            val existing = appRepository.getByPackageName(appInfo.packageName)
            AppEntity(
                packageName = appInfo.packageName,
                appName = appInfo.appName,
                folderId = existing?.folderId,
                category = if (existing?.isManualOverride == true) existing.category else appInfo.category,
                isManualOverride = existing?.isManualOverride ?: false,
                installTime = appInfo.installTime,
                confidenceScore = appInfo.confidenceScore
            )
        }

        appRepository.insertApps(entities)
        return entities
    }
}
