package com.smartorganizer.launcher.domain.usecase

import com.smartorganizer.launcher.engine.ClassificationResult
import com.smartorganizer.launcher.engine.RuleEngine
import javax.inject.Inject

class ClassifyAppUseCase @Inject constructor(
    private val ruleEngine: RuleEngine
) {
    /**
     * Classifies a single app by name and package name.
     * Returns a ClassificationResult with category and confidence score.
     */
    operator fun invoke(appName: String, packageName: String): ClassificationResult {
        return ruleEngine.classify(appName, packageName)
    }
}
