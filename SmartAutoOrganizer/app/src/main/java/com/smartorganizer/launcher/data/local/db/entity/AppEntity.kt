package com.smartorganizer.launcher.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "apps",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val folderId: Long? = null,
    val category: String = "Others",
    val isManualOverride: Boolean = false,
    val installTime: Long = System.currentTimeMillis(),
    val confidenceScore: Float = 0f
)
