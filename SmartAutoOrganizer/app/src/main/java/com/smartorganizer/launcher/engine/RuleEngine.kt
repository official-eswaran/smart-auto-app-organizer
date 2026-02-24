package com.smartorganizer.launcher.engine

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleEngine @Inject constructor() {

    companion object {
        // Minimum confidence to assign a category (below → "Others")
        private const val CONFIDENCE_THRESHOLD = 0.3f

        // Scoring weights
        private const val EXACT_NAME_MATCH = 3.0f      // token == keyword (e.g. "spotify" == "spotify")
        private const val SUBSTRING_NAME_MATCH = 1.0f  // token contains keyword or vice-versa
        private const val EXACT_PACKAGE_MATCH = 2.0f   // package token == keyword
        private const val SUBSTRING_PACKAGE_MATCH = 0.5f

        // Confidence cap denominator — 1 strong name hit → high confidence
        private const val NORMALIZATION_CAP = 4.0f
    }

    private val categoryKeywords: Map<String, List<String>> = mapOf(
        "Payments" to listOf(
            "pay", "upi", "bank", "wallet", "money", "gpay", "phonepe", "paytm",
            "finance", "credit", "debit", "transfer", "transaction", "cash", "bhim"
        ),
        "Games" to listOf(
            "game", "battle", "racing", "puzzle", "arena", "clash", "craft", "bgmi",
            "play", "quest", "hero", "war", "strike", "shooter", "runner", "adventure",
            "chess", "ludo", "rummy", "casino", "dice"
        ),
        "Social" to listOf(
            "chat", "message", "social", "whatsapp", "telegram", "instagram",
            "facebook", "twitter", "linkedin", "snapchat", "tiktok", "share",
            "connect", "meet", "discord", "viber", "signal"
        ),
        "Shopping" to listOf(
            "shop", "store", "buy", "amazon", "flipkart", "cart", "market",
            "deal", "sale", "order", "meesho", "myntra", "nykaa", "snapdeal",
            "commerce", "mall", "fashion"
        ),
        "Music" to listOf(
            "music", "song", "audio", "spotify", "gaana", "radio", "jio",
            "wynk", "saavn", "tune", "beat", "podcast", "fm", "sound",
            "player", "stream", "mp3"
        ),
        "Health" to listOf(
            "health", "fit", "fitness", "doctor", "med", "pharmacy", "workout",
            "yoga", "diet", "nutrition", "hospital", "clinic", "pharma",
            "wellness", "care", "calorie", "pulse", "heart"
        ),
        "Travel" to listOf(
            "travel", "cab", "ride", "flight", "hotel", "train", "ola", "uber",
            "rapido", "bus", "trip", "map", "navigation", "route", "irctc",
            "booking", "makemytrip", "goibibo", "yatra", "ixigo"
        ),
        "News" to listOf(
            "news", "times", "daily", "feed", "headlines", "media", "press",
            "live", "report", "update", "breaking", "inshorts", "flipboard",
            "journal", "digest", "current"
        )
    )

    /**
     * Classifies an app by its display name and package name using keyword scoring.
     *
     * Scoring per keyword hit:
     *   - Exact name token match   → +3.0
     *   - Substring name match     → +1.0
     *   - Exact package token match→ +2.0
     *   - Substring package match  → +0.5
     *
     * Confidence = min(1.0, totalScore / 4.0).
     * If best confidence >= threshold (0.3) → return that category; else "Others".
     */
    fun classify(appName: String, packageName: String): ClassificationResult {
        val nameTokens = tokenize(appName)
        val packageTokens = tokenizePackage(packageName)

        val scores = mutableMapOf<String, Float>()

        for ((category, keywords) in categoryKeywords) {
            var score = 0f

            for (keyword in keywords) {
                // Name scoring
                for (token in nameTokens) {
                    score += when {
                        token == keyword -> EXACT_NAME_MATCH
                        token.contains(keyword) -> SUBSTRING_NAME_MATCH
                        keyword.contains(token) && token.length >= 3 -> SUBSTRING_NAME_MATCH
                        else -> 0f
                    }
                }

                // Package scoring
                for (token in packageTokens) {
                    score += when {
                        token == keyword -> EXACT_PACKAGE_MATCH
                        token.contains(keyword) -> SUBSTRING_PACKAGE_MATCH
                        keyword.contains(token) && token.length >= 3 -> SUBSTRING_PACKAGE_MATCH
                        else -> 0f
                    }
                }
            }

            if (score > 0f) {
                scores[category] = minOf(1.0f, score / NORMALIZATION_CAP)
            }
        }

        if (scores.isEmpty()) return ClassificationResult.unclassified()

        val best = scores.maxByOrNull { it.value } ?: return ClassificationResult.unclassified()

        return if (best.value >= CONFIDENCE_THRESHOLD) {
            ClassificationResult(category = best.key, confidence = best.value)
        } else {
            ClassificationResult.unclassified()
        }
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase().split(Regex("[\\s_\\-.,]+")).filter { it.isNotBlank() }

    private fun tokenizePackage(packageName: String): List<String> =
        packageName.lowercase()
            .split(".")
            .filter { it.length > 2 && it !in setOf("com", "org", "net", "app", "android", "google") }
}
