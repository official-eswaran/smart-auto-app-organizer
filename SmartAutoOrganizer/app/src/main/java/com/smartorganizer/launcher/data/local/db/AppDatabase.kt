package com.smartorganizer.launcher.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartorganizer.launcher.data.local.db.dao.AppDao
import com.smartorganizer.launcher.data.local.db.dao.FolderDao
import com.smartorganizer.launcher.data.local.db.entity.AppEntity
import com.smartorganizer.launcher.data.local.db.entity.FolderEntity

@Database(
    entities = [AppEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun folderDao(): FolderDao

    companion object {
        const val DATABASE_NAME = "smart_organizer.db"
    }
}
