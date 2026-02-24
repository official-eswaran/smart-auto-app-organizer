package com.smartorganizer.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

private const val PIN_LENGTH = 4

/** Mode: entering PIN to unlock, or setting a new PIN for a folder. */
enum class PinDialogMode { UNLOCK, SET_NEW }

@Composable
fun PinLockDialog(
    folderName: String,
    mode: PinDialogMode = PinDialogMode.UNLOCK,
    onPinEntered: (pin: String) -> Boolean, // return true = correct / accepted
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val errorShake by animateFloatAsState(
        targetValue = if (isError) 1f else 0f,
        animationSpec = tween(200),
        label = "shake"
    )

    // Auto-check when PIN reaches required length
    LaunchedEffect(pin) {
        if (pin.length == PIN_LENGTH) {
            val accepted = onPinEntered(pin)
            if (accepted) {
                isSuccess = true
                delay(300)
                onDismiss()
            } else {
                isError = true
                delay(600)
                isError = false
                pin = ""
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (mode == PinDialogMode.UNLOCK) "\uD83D\uDD12 $folderName" else "Set PIN — $folderName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (mode == PinDialogMode.UNLOCK) "Enter 4-digit PIN to unlock"
                           else "Enter a new 4-digit PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                // PIN indicator dots
                val dotColor by animateColorAsState(
                    targetValue = when {
                        isError -> MaterialTheme.colorScheme.error
                        isSuccess -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    label = "dot_color"
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.scale(1f + errorShake * 0.04f)
                ) {
                    repeat(PIN_LENGTH) { i ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (i < pin.length) dotColor else dotColor.copy(alpha = 0.2f))
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Number pad
                val digits = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫")
                )
                digits.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        row.forEach { label ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                when (label) {
                                    "" -> Spacer(Modifier.size(64.dp))
                                    "⌫" -> IconButton(
                                        onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    else -> FilledTonalButton(
                                        onClick = { if (pin.length < PIN_LENGTH) pin += label },
                                        modifier = Modifier.size(64.dp),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}
