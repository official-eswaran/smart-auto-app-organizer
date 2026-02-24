package com.smartorganizer.launcher.ui.home

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartorganizer.launcher.data.local.db.entity.AppEntity
import com.smartorganizer.launcher.data.repository.AppRepository
import com.smartorganizer.launcher.data.repository.FolderRepository
import com.smartorganizer.launcher.security.PinManager
import com.smartorganizer.launcher.domain.model.AppInfo
import com.smartorganizer.launcher.domain.model.Folder
import com.smartorganizer.launcher.domain.usecase.OrganizeFoldersUseCase
import com.smartorganizer.launcher.domain.usecase.ScanAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanAppsUseCase: ScanAppsUseCase,
    private val organizeFoldersUseCase: OrganizeFoldersUseCase,
    private val folderRepository: FolderRepository,
    private val appRepository: AppRepository,
    private val pinManager: PinManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        initializeLauncher()
    }

    private fun initializeLauncher() {
        viewModelScope.launch {
            try {
                _uiState.value = HomeUiState(isLoading = true)

                // Scan and classify apps
                val scannedApps = scanAppsUseCase()

                // Organize into folders
                organizeFoldersUseCase(scannedApps)

                // Observe folders and apps together
                observeFoldersWithApps()
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to initialize launcher"
                )
            }
        }
    }

    private fun observeFoldersWithApps() {
        viewModelScope.launch {
            combine(
                folderRepository.getAllFolders(),
                appRepository.getAllApps()
            ) { folderEntities, appEntities ->
                val pm = context.packageManager
                val appsByFolder = appEntities.groupBy { it.folderId }

                folderEntities.map { folderEntity ->
                    val apps = appsByFolder[folderEntity.id]?.map { appEntity ->
                        appEntity.toAppInfo(pm)
                    } ?: emptyList()

                    Folder(
                        id = folderEntity.id,
                        name = folderEntity.name,
                        colorHex = folderEntity.colorHex,
                        isLocked = folderEntity.isLocked,
                        sortOrder = folderEntity.sortOrder,
                        apps = apps
                    )
                }.filter { it.apps.isNotEmpty() }
            }
                .catch { e ->
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        error = e.message ?: "Error loading folders"
                    )
                }
                .collect { folders ->
                    _uiState.value = HomeUiState(
                        folders = folders,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun refresh() {
        initializeLauncher()
    }

    fun moveAppToFolder(packageName: String, folderId: Long) {
        viewModelScope.launch {
            appRepository.setManualOverride(packageName, folderId)
        }
    }

    /** Returns true if this folder is PIN-locked. */
    fun isFolderLocked(folderId: Long): Boolean = pinManager.isLocked(folderId)

    /** Verify PIN for a locked folder. */
    fun verifyPin(folderId: Long, pin: String): Boolean = pinManager.verifyPin(folderId, pin)

    /** Set a new PIN and mark the folder as locked in DB. */
    fun setFolderPin(folderId: Long, pin: String) {
        pinManager.setPin(folderId, pin)
        viewModelScope.launch {
            val folder = folderRepository.getById(folderId) ?: return@launch
            folderRepository.insert(folder.copy(isLocked = true))
        }
    }

    /** Remove PIN lock from a folder after verifying the current PIN. */
    fun removeFolderLock(folderId: Long, pin: String): Boolean {
        if (!pinManager.verifyPin(folderId, pin)) return false
        pinManager.clearPin(folderId)
        viewModelScope.launch {
            val folder = folderRepository.getById(folderId) ?: return@launch
            folderRepository.insert(folder.copy(isLocked = false))
        }
        return true
    }

    /** Called after a drag-drop reorder: persists the new sort order to DB. */
    fun reorderFolders(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.folders.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        // Optimistic UI update
        _uiState.value = _uiState.value.copy(folders = current)
        // Persist new sort order
        viewModelScope.launch {
            folderRepository.updateSortOrders(current.map { it.id })
        }
    }

    private fun AppEntity.toAppInfo(pm: PackageManager): AppInfo {
        val icon = try { pm.getApplicationIcon(packageName) } catch (e: Exception) { null }
        return AppInfo(
            packageName = packageName,
            appName = appName,
            icon = icon,
            category = category,
            installTime = installTime,
            confidenceScore = confidenceScore,
            isManualOverride = isManualOverride
        )
    }
}
