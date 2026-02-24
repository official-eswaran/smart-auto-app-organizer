package com.smartorganizer.launcher.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.smartorganizer.launcher.domain.model.AppInfo
import com.smartorganizer.launcher.engine.HybridClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hybridClassifier: HybridClassifier
) {
    private val pm: PackageManager = context.packageManager

    /**
     * Scans for all launchable, non-system apps installed on the device.
     * Uses MAIN + LAUNCHER intent filter to identify launchable apps.
     * Excludes pure system apps (FLAG_SYSTEM) unless updated (FLAG_UPDATED_SYSTEM_APP).
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
                } catch (e: Exception) { System.currentTimeMillis() }

                // Use system category if available, otherwise HybridClassifier
                val (category, confidence) = resolveCategory(appInfo, appName, packageName)

                AppInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = icon,
                    category = category,
                    installTime = installTime,
                    confidenceScore = confidence
                )
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }.sortedBy { it.appName }
    }

    private fun resolveCategory(
        appInfo: ApplicationInfo,
        appName: String,
        packageName: String
    ): Pair<String, Float> {
        // API-level system category hints (high confidence)
        val systemHint: Pair<String, Float>? = when (appInfo.category) {
            ApplicationInfo.CATEGORY_GAME -> "Games" to 1.0f
            ApplicationInfo.CATEGORY_AUDIO -> "Music" to 1.0f
            ApplicationInfo.CATEGORY_VIDEO -> "Entertainment" to 0.9f
            ApplicationInfo.CATEGORY_IMAGE -> "Photos" to 0.9f
            ApplicationInfo.CATEGORY_SOCIAL -> "Social" to 1.0f
            ApplicationInfo.CATEGORY_NEWS -> "News" to 1.0f
            ApplicationInfo.CATEGORY_MAPS -> "Travel" to 1.0f
            else -> null
        }
        if (systemHint != null) return systemHint

        // Hybrid ML classifier
        val result = hybridClassifier.classify(appName, packageName)
        return result.category to result.confidence
    }
}
