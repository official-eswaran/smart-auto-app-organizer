package com.smartorganizer.launcher.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartorganizer.launcher.data.local.db.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<FolderEntity>)

    @Delete
    suspend fun delete(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteById(folderId: Long)

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, name ASC")
    fun getAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FolderEntity?

    @Query("DELETE FROM folders WHERE id NOT IN (SELECT DISTINCT folderId FROM apps WHERE folderId IS NOT NULL)")
    suspend fun deleteEmptyFolders()
}
