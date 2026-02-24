package com.smartorganizer.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartorganizer.launcher.domain.model.Folder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailBottomSheet(
    folder: Folder,
    onDismiss: () -> Unit,
    onToggleLock: ((folderId: Long, pin: String) -> Unit)? = null,
    onRemoveLock: ((folderId: Long, pin: String) -> Boolean)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${folder.apps.size} apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Lock / Unlock toggle
                IconButton(
                    onClick = {
                        if (folder.isLocked) showRemovePinDialog = true
                        else showSetPinDialog = true
                    }
                ) {
                    Icon(
                        imageVector = if (folder.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (folder.isLocked) "Remove lock" else "Set PIN lock",
                        tint = if (folder.isLocked) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folder.apps, key = { it.packageName }) { app ->
                    AppItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        icon = app.icon
                    )
                }
            }
        }
    }

    // Set new PIN dialog
    if (showSetPinDialog) {
        var confirmPin by remember { mutableStateOf<String?>(null) }
        PinLockDialog(
            folderName = folder.name,
            mode = if (confirmPin == null) PinDialogMode.SET_NEW else PinDialogMode.SET_NEW,
            onPinEntered = { pin ->
                if (confirmPin == null) {
                    confirmPin = pin
                    false // ask to confirm
                } else if (pin == confirmPin) {
                    onToggleLock?.invoke(folder.id, pin)
                    showSetPinDialog = false
                    true
                } else {
                    confirmPin = null
                    false
                }
            },
            onDismiss = { showSetPinDialog = false }
        )
    }

    // Remove PIN dialog
    if (showRemovePinDialog) {
        PinLockDialog(
            folderName = folder.name,
            mode = PinDialogMode.UNLOCK,
            onPinEntered = { pin ->
                val removed = onRemoveLock?.invoke(folder.id, pin) ?: false
                if (removed) showRemovePinDialog = false
                removed
            },
            onDismiss = { showRemovePinDialog = false }
        )
    }
}
