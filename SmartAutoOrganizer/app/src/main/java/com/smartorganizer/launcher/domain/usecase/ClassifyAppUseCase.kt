package com.smartorganizer.launcher.domain.usecase

import com.smartorganizer.launcher.engine.ClassificationResult
import com.smartorganizer.launcher.engine.HybridClassifier
import javax.inject.Inject

class ClassifyAppUseCase @Inject constructor(
    private val hybridClassifier: HybridClassifier
) {
    /**
     * Classifies a single app using the HybridClassifier
     * (TFLite → NaiveBayes → RuleEngine priority chain).
     */
    operator fun invoke(appName: String, packageName: String): ClassificationResult {
        return hybridClassifier.classify(appName, packageName)
    }
}
