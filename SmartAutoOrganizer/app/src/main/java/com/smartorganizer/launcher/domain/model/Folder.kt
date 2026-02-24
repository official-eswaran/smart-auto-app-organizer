package com.smartorganizer.launcher.domain.model

data class Folder(
    val id: Long,
    val name: String,
    val colorHex: String,
    val isLocked: Boolean = false,
    val sortOrder: Int = 0,
    val apps: List<AppInfo> = emptyList()
)
