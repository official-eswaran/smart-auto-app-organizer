package com.smartorganizer.launcher.engine

import android.content.Context
import android.util.Log

/**
 * TFLite classifier stub — MVP phase uses NaiveBayes + RuleEngine instead.
 *
 * To activate in Phase 2:
 *   1. Add TFLite dependencies to build.gradle.kts
 *   2. Place trained model at app/src/main/assets/app_classifier.tflite
 *   3. Replace this stub with the full TFLite implementation
 *
 * HybridClassifier checks isAvailable before calling classify(),
 * so returning null here safely falls through to NaiveBayes + RuleEngine.
 */
class TFLiteClassifier(private val context: Context) {

    companion object {
        private const val TAG = "TFLiteClassifier"
    }

    /** Always false in MVP — no TFLite model bundled yet. */
    val isAvailable: Boolean = false

    init {
        Log.i(TAG, "TFLite stub active — using NaiveBayes + RuleEngine fallback")
    }

    /** Returns null — HybridClassifier falls back to NaiveBayes + RuleEngine. */
    fun classify(appName: String, packageName: String): ClassificationResult? = null

    fun close() { /* no-op */ }
}
