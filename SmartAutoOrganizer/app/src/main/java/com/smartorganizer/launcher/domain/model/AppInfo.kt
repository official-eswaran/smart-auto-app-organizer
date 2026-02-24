package com.smartorganizer.launcher.domain.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val category: String = "Others",
    val installTime: Long = System.currentTimeMillis(),
    val confidenceScore: Float = 0f,
    val isManualOverride: Boolean = false
)
