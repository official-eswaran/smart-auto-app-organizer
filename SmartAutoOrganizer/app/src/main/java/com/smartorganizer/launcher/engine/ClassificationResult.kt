package com.smartorganizer.launcher.engine

data class ClassificationResult(
    val category: String,
    val confidence: Float
) {
    companion object {
        fun unclassified() = ClassificationResult(
            category = "Others",
            confidence = 0f
        )
    }
}
