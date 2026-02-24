package com.smartorganizer.launcher.ui.home

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartorganizer.launcher.R
import com.smartorganizer.launcher.domain.model.Folder
import com.smartorganizer.launcher.ui.components.FolderCard
import com.smartorganizer.launcher.ui.components.FolderDetailBottomSheet
import com.smartorganizer.launcher.ui.components.PinDialogMode
import com.smartorganizer.launcher.ui.components.PinLockDialog
import com.smartorganizer.launcher.ui.components.rememberDragDropState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState: HomeUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // The folder whose detail bottom-sheet is open (after unlock if needed)
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }
    // The folder waiting for PIN unlock before opening
    var pendingLockedFolder by remember { mutableStateOf<Folder?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(message = it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.folders.isEmpty() -> Text(
                    text = stringResource(R.string.no_apps_found),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> {
                    val folders: List<Folder> = uiState.folders
                    val gridState = rememberLazyGridState()
                    val dragDropState = rememberDragDropState(gridState) { from, to ->
                        viewModel.reorderFolders(from, to)
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(dragDropState) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { dragDropState.onDragStart(it) },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        dragDropState.onDrag(delta)
                                    },
                                    onDragEnd = { dragDropState.onDragEnd() },
                                    onDragCancel = { dragDropState.onDragCancel() }
                                )
                            }
                    ) {
                        itemsIndexed(folders, key = { _, f: Folder -> f.id }) { index, folder: Folder ->
                            FolderCard(
                                folder = folder,
                                isDragging = index == dragDropState.draggingItemIndex,
                                onClick = {
                                    if (folder.isLocked) pendingLockedFolder = folder
                                    else selectedFolder = folder
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // PIN unlock dialog for locked folders
    pendingLockedFolder?.let { locked ->
        PinLockDialog(
            folderName = locked.name,
            mode = PinDialogMode.UNLOCK,
            onPinEntered = { pin ->
                val ok = viewModel.verifyPin(locked.id, pin)
                if (ok) {
                    selectedFolder = locked
                    pendingLockedFolder = null
                }
                ok
            },
            onDismiss = { pendingLockedFolder = null }
        )
    }

    // Folder detail bottom sheet (only shown after unlock)
    selectedFolder?.let { folder ->
        FolderDetailBottomSheet(
            folder = folder,
            onDismiss = { selectedFolder = null },
            onToggleLock = { folderId, pin -> viewModel.setFolderPin(folderId, pin) },
            onRemoveLock = { folderId, pin -> viewModel.removeFolderLock(folderId, pin) }
        )
    }
}
