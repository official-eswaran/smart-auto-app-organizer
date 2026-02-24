package com.smartorganizer.launcher.engine

/**
 * Converts app name + package name into a bag-of-unigrams + bigrams feature vector.
 * Used by both NaiveBayesClassifier (training + inference) and TFLiteClassifier (pre-processing).
 */
class TextVectorizer(private val vocabulary: Map<String, Int>) {

    companion object {
        // Stop-words that add no signal
        private val STOP_WORDS = setOf(
            "com", "org", "net", "app", "android", "google", "the", "and",
            "for", "with", "from", "this", "that", "are", "have", "has"
        )

        /**
         * Builds a vocabulary from a corpus of (text, label) pairs.
         * Returns a TextVectorizer fitted on that corpus.
         */
        fun fit(corpus: List<Pair<String, String>>, maxVocabSize: Int = 2000): TextVectorizer {
            val termFreq = mutableMapOf<String, Int>()
            for ((text, _) in corpus) {
                for (token in tokenize(text)) {
                    termFreq[token] = (termFreq[token] ?: 0) + 1
                }
            }
            // Keep top-N most frequent terms (excluding stop-words)
            val vocab = termFreq.entries
                .sortedByDescending { it.value }
                .filter { it.key !in STOP_WORDS && it.key.length > 1 }
                .take(maxVocabSize)
                .mapIndexed { idx, entry -> entry.key to idx }
                .toMap()
            return TextVectorizer(vocab)
        }

        fun tokenize(text: String): List<String> {
            val tokens = text.lowercase()
                .split(Regex("[.\\s_\\-,/]+"))
                .filter { it.isNotBlank() && it.length > 1 }

            // Unigrams + bigrams
            val result = tokens.toMutableList()
            for (i in 0 until tokens.size - 1) {
                result.add("${tokens[i]}_${tokens[i + 1]}")
            }
            return result
        }
    }

    val vocabSize: Int get() = vocabulary.size

    /** Returns a sparse term-frequency map (token index → count) for the given text. */
    fun transform(text: String): Map<Int, Int> {
        val counts = mutableMapOf<Int, Int>()
        for (token in tokenize(text)) {
            val idx = vocabulary[token] ?: continue
            counts[idx] = (counts[idx] ?: 0) + 1
        }
        return counts
    }

    fun getVocabulary(): Map<String, Int> = vocabulary
}
