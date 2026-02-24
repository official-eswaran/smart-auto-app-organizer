package com.smartorganizer.launcher.data.repository

import com.smartorganizer.launcher.data.local.db.dao.AppDao
import com.smartorganizer.launcher.data.local.db.entity.AppEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val appDao: AppDao
) {
    fun getAllApps(): Flow<List<AppEntity>> = appDao.getAll()

    fun getAppsByFolder(folderId: Long): Flow<List<AppEntity>> = appDao.getByFolder(folderId)

    suspend fun insertApp(app: AppEntity) = appDao.insert(app)

    suspend fun insertApps(apps: List<AppEntity>) = appDao.insertAll(apps)

    suspend fun updateApp(app: AppEntity) = appDao.update(app)

    suspend fun deleteApp(app: AppEntity) = appDao.delete(app)

    suspend fun deleteByPackageName(packageName: String) = appDao.deleteByPackageName(packageName)

    suspend fun getByPackageName(packageName: String): AppEntity? = appDao.getByPackageName(packageName)

    suspend fun updateFolder(packageName: String, folderId: Long?) = appDao.updateFolder(packageName, folderId)

    suspend fun setManualOverride(packageName: String, folderId: Long) = appDao.setManualOverride(packageName, folderId)
}
