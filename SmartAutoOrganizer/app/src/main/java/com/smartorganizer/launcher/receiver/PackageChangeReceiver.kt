package com.smartorganizer.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.smartorganizer.launcher.worker.ClassifyAppWorker

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        val action = when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> ClassifyAppWorker.ACTION_ADD
            Intent.ACTION_PACKAGE_REMOVED -> ClassifyAppWorker.ACTION_REMOVE
            Intent.ACTION_PACKAGE_REPLACED -> ClassifyAppWorker.ACTION_REPLACE
            else -> return
        }

        val inputData = Data.Builder()
            .putString(ClassifyAppWorker.KEY_PACKAGE_NAME, packageName)
            .putString(ClassifyAppWorker.KEY_ACTION, action)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ClassifyAppWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
