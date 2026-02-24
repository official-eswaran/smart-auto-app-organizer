package com.smartorganizer.launcher.engine

import kotlin.math.ln

/**
 * Multinomial Naive Bayes text classifier — pure Kotlin, no external ML framework.
 *
 * Training is done in-memory at first launch using a synthetic corpus built from
 * the category keyword maps + augmented app name templates.
 *
 * P(category | text) ∝ P(category) × Π P(token | category)
 *
 * With Laplace (add-1) smoothing to handle unseen tokens.
 */
class NaiveBayesClassifier {

    // log P(category)
    private val logPrior = mutableMapOf<String, Double>()

    // log P(token_idx | category)  — stored as sparse map
    private val logLikelihood = mutableMapOf<String, MutableMap<Int, Double>>()

    // vocabulary size (needed for Laplace smoothing denominator)
    private var vocabSize: Int = 0

    private var vectorizer: TextVectorizer? = null

    val isTrained: Boolean get() = logPrior.isNotEmpty()

    /**
     * Train on a list of (featureText, label) pairs.
     * featureText = "$appName $packageName"
     */
    fun train(corpus: List<Pair<String, String>>) {
        val fitted = TextVectorizer.fit(corpus, maxVocabSize = 3000)
        vectorizer = fitted
        vocabSize = fitted.vocabSize

        val categories = corpus.map { it.second }.distinct()
        val totalDocs = corpus.size.toDouble()

        // Count documents per category
        val docCounts = mutableMapOf<String, Int>()
        // Count token occurrences per category
        val tokenCounts = mutableMapOf<String, MutableMap<Int, Int>>()
        val categoryCounts = mutableMapOf<String, Int>()

        for (cat in categories) {
            docCounts[cat] = 0
            tokenCounts[cat] = mutableMapOf()
            categoryCounts[cat] = 0
        }

        for ((text, label) in corpus) {
            docCounts[label] = (docCounts[label] ?: 0) + 1
            val features = fitted.transform(text)
            for ((idx, count) in features) {
                tokenCounts[label]!![idx] = (tokenCounts[label]!![idx] ?: 0) + count
                categoryCounts[label] = (categoryCounts[label] ?: 0) + count
            }
        }

        // Compute log priors
        for (cat in categories) {
            logPrior[cat] = ln((docCounts[cat] ?: 1).toDouble() / totalDocs)
        }

        // Compute log likelihoods with Laplace smoothing
        for (cat in categories) {
            val catTotal = (categoryCounts[cat] ?: 0) + vocabSize
            val ll = mutableMapOf<Int, Double>()
            val catTokens = tokenCounts[cat] ?: emptyMap()
            for ((idx, count) in catTokens) {
                ll[idx] = ln((count + 1).toDouble() / catTotal)
            }
            // Store the default log-likelihood for unseen tokens (smoothed)
            ll[-1] = ln(1.0 / catTotal)  // sentinel for unseen tokens
            logLikelihood[cat] = ll
        }
    }

    /**
     * Predict the most likely category and its softmax-normalized confidence.
     */
    fun predict(appName: String, packageName: String): ClassificationResult {
        val vec = vectorizer ?: return ClassificationResult.unclassified()
        val features = vec.transform("$appName $packageName")

        val scores = mutableMapOf<String, Double>()
        for ((cat, prior) in logPrior) {
            var score = prior
            val ll = logLikelihood[cat] ?: continue
            val defaultLL = ll[-1] ?: ln(1.0 / (vocabSize + 1))
            for ((idx, count) in features) {
                val tokenLL = ll[idx] ?: defaultLL
                score += count * tokenLL
            }
            scores[cat] = score
        }

        if (scores.isEmpty()) return ClassificationResult.unclassified()

        // Softmax over log-scores to get probabilities
        val maxScore = scores.values.max()
        val expScores = scores.mapValues { Math.exp(it.value - maxScore) }
        val sumExp = expScores.values.sum()
        val probabilities = expScores.mapValues { (it.value / sumExp).toFloat() }

        val best = probabilities.maxByOrNull { it.value }
            ?: return ClassificationResult.unclassified()

        return ClassificationResult(
            category = best.key,
            confidence = best.value
        )
    }
}
