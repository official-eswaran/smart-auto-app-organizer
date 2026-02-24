package com.smartorganizer.launcher.data.repository

import com.smartorganizer.launcher.data.local.db.dao.FolderDao
import com.smartorganizer.launcher.data.local.db.entity.FolderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao
) {
    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAll()

    suspend fun insert(folder: FolderEntity): Long = folderDao.insert(folder)

    suspend fun insertAll(folders: List<FolderEntity>) = folderDao.insertAll(folders)

    suspend fun delete(folder: FolderEntity) = folderDao.delete(folder)

    suspend fun deleteById(folderId: Long) = folderDao.deleteById(folderId)

    suspend fun getByName(name: String): FolderEntity? = folderDao.getByName(name)

    suspend fun getById(id: Long): FolderEntity? = folderDao.getById(id)

    suspend fun deleteEmptyFolders() = folderDao.deleteEmptyFolders()
}
