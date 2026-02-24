package com.smartorganizer.launcher.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartorganizer.launcher.data.local.db.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppEntity>)

    @Update
    suspend fun update(app: AppEntity)

    @Delete
    suspend fun delete(app: AppEntity)

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT * FROM apps WHERE folderId = :folderId ORDER BY appName ASC")
    fun getByFolder(folderId: Long): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps ORDER BY appName ASC")
    fun getAll(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): AppEntity?

    @Query("UPDATE apps SET folderId = :folderId WHERE packageName = :packageName")
    suspend fun updateFolder(packageName: String, folderId: Long?)

    @Query("UPDATE apps SET isManualOverride = 1, folderId = :folderId WHERE packageName = :packageName")
    suspend fun setManualOverride(packageName: String, folderId: Long)

    @Query("SELECT * FROM apps WHERE category = :category ORDER BY appName ASC")
    fun getByCategory(category: String): Flow<List<AppEntity>>
}
