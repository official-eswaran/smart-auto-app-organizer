package com.smartorganizer.launcher.domain.usecase

import com.smartorganizer.launcher.data.local.db.entity.AppEntity
import com.smartorganizer.launcher.data.local.db.entity.FolderEntity
import com.smartorganizer.launcher.data.repository.AppRepository
import com.smartorganizer.launcher.data.repository.FolderRepository
import javax.inject.Inject

class OrganizeFoldersUseCase @Inject constructor(
    private val folderRepository: FolderRepository,
    private val appRepository: AppRepository
) {
    // Folder color presets corresponding to categories
    private val categoryColors = mapOf(
        "Payments" to "#4CAF50",
        "Games" to "#F44336",
        "Social" to "#2196F3",
        "Shopping" to "#FF9800",
        "Music" to "#9C27B0",
        "Health" to "#00BCD4",
        "Travel" to "#3F51B5",
        "News" to "#607D8B",
        "Others" to "#9E9E9E"
    )

    /**
     * Groups apps by category, creates/reuses folders for each category,
     * and assigns each app to its corresponding folder.
     * Apps with manual overrides are skipped (their folderId is preserved).
     */
    suspend operator fun invoke(apps: List<AppEntity>) {
        // Group non-override apps by category
        val categoryGroups = apps
            .filter { !it.isManualOverride }
            .groupBy { it.category }

        for ((category, appsInCategory) in categoryGroups) {
            if (appsInCategory.isEmpty()) continue

            // Find or create folder for this category
            val folder = folderRepository.getByName(category) ?: run {
                val colorHex = categoryColors[category] ?: "#9E9E9E"
                val sortOrder = categoryColors.keys.indexOf(category).coerceAtLeast(0)
                val newFolder = FolderEntity(
                    name = category,
                    colorHex = colorHex,
                    sortOrder = sortOrder
                )
                val folderId = folderRepository.insert(newFolder)
                FolderEntity(
                    id = folderId,
                    name = category,
                    colorHex = colorHex,
                    sortOrder = sortOrder
                )
            }

            // Assign apps to folder
            for (app in appsInCategory) {
                appRepository.updateFolder(app.packageName, folder.id)
            }
        }

        // Clean up empty folders
        folderRepository.deleteEmptyFolders()
    }
}
