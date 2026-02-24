package com.smartorganizer.launcher.engine

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HybridClassifier: combines three classifiers in priority order.
 *
 * Priority:
 *   1. TFLiteClassifier  — if model file is present in assets/ (highest accuracy)
 *   2. NaiveBayesClassifier — trained in-memory on first call (Kotlin ML)
 *   3. RuleEngine        — deterministic keyword fallback
 *
 * The highest-confidence result that meets its threshold wins.
 * Ties between NaiveBayes and RuleEngine are resolved by averaging.
 */
@Singleton
class HybridClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleEngine: RuleEngine
) {
    companion object {
        private const val TAG = "HybridClassifier"
        private const val NB_THRESHOLD = 0.20f   // NaiveBayes min confidence
        private const val RULE_THRESHOLD = 0.30f  // RuleEngine min confidence
        private const val TFLITE_THRESHOLD = 0.40f
    }

    private val naiveBayes = NaiveBayesClassifier()
    private val tflite: TFLiteClassifier by lazy { TFLiteClassifier(context) }
    private var nbTrained = false

    /** Classifies an app using the best available model. */
    fun classify(appName: String, packageName: String): ClassificationResult {
        ensureNaiveBayesTrained()

        // 1. Try TFLite first
        val tfliteResult = runCatching { tflite.classify(appName, packageName) }.getOrNull()
        if (tfliteResult != null && tfliteResult.confidence >= TFLITE_THRESHOLD) {
            Log.d(TAG, "TFLite: $appName → ${tfliteResult.category} (${tfliteResult.confidence})")
            return tfliteResult
        }

        // 2. Naive Bayes
        val nbResult = naiveBayes.predict(appName, packageName)

        // 3. Rule Engine
        val ruleResult = ruleEngine.classify(appName, packageName)

        // Combine: if both agree → use NB confidence (higher recall)
        return when {
            nbResult.category == ruleResult.category &&
                    nbResult.confidence >= NB_THRESHOLD -> {
                val blended = (nbResult.confidence * 0.6f + ruleResult.confidence * 0.4f)
                    .coerceIn(0f, 1f)
                ClassificationResult(nbResult.category, blended)
            }
            nbResult.confidence >= NB_THRESHOLD && nbResult.category != "Others" -> nbResult
            ruleResult.confidence >= RULE_THRESHOLD -> ruleResult
            // Pick whichever has higher confidence
            nbResult.confidence > ruleResult.confidence -> nbResult
            else -> ruleResult
        }
    }

    private fun ensureNaiveBayesTrained() {
        if (nbTrained) return
        synchronized(this) {
            if (nbTrained) return
            val corpus = TrainingCorpus.generate()
            naiveBayes.train(corpus)
            nbTrained = true
            Log.i(TAG, "NaiveBayes trained on ${corpus.size} samples")
        }
    }
}
