package com.smartorganizer.launcher.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.smartorganizer.launcher.domain.model.AppInfo
import com.smartorganizer.launcher.engine.RuleEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleEngine: RuleEngine
) {
    private val pm: PackageManager = context.packageManager

    /**
     * Scans for all launchable, non-system apps installed on the device.
     * Uses MAIN + LAUNCHER intent filter to identify launchable apps.
     * Excludes system apps (FLAG_SYSTEM) unless they are also updated (FLAG_UPDATED_SYSTEM_APP).
     */
    fun scanInstalledApps(): List<AppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val launchablePackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, PackageManager.GET_META_DATA)
        }.map { it.activityInfo.packageName }.toSet()

        return launchablePackages.mapNotNull { packageName ->
            try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(packageName, 0)
                }

                // Skip pure system apps (allow updated system apps)
                val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                val isUpdatedSystemApp = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                if (isSystemApp && !isUpdatedSystemApp) return@mapNotNull null

                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = try { pm.getApplicationIcon(packageName) } catch (e: Exception) { null }
                val installTime = try {
                    pm.getPackageInfo(packageName, 0).firstInstallTime
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                // Try system category first (API 26+), fallback to RuleEngine
                val category = mapSystemCategory(appInfo.category, appName, packageName)

                AppInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = icon,
                    category = category.first,
                    installTime = installTime,
                    confidenceScore = category.second
                )
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }.sortedBy { it.appName }
    }

    private fun mapSystemCategory(
        systemCategory: Int,
        appName: String,
        packageName: String
    ): Pair<String, Float> {
        val systemMapped = when (systemCategory) {
            ApplicationInfo.CATEGORY_GAME -> "Games" to 1.0f
            ApplicationInfo.CATEGORY_AUDIO -> "Music" to 1.0f
            ApplicationInfo.CATEGORY_VIDEO -> "Entertainment" to 1.0f
            ApplicationInfo.CATEGORY_IMAGE -> "Photos" to 1.0f
            ApplicationInfo.CATEGORY_SOCIAL -> "Social" to 1.0f
            ApplicationInfo.CATEGORY_NEWS -> "News" to 1.0f
            ApplicationInfo.CATEGORY_MAPS -> "Travel" to 1.0f
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> null // let RuleEngine decide
            else -> null
        }

        return systemMapped ?: run {
            val result = ruleEngine.classify(appName, packageName)
            result.category to result.confidence
        }
    }
}
