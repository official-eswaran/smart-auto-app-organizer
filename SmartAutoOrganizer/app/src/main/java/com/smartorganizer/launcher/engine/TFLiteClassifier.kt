package com.smartorganizer.launcher.engine

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.IOException

/**
 * TFLite-based classifier — loads a pre-trained .tflite model from assets/.
 *
 * Model contract:
 *   Input  → float[1][VOCAB_SIZE]  (TF-IDF bag-of-words vector)
 *   Output → float[1][NUM_CLASSES] (softmax probabilities)
 *
 * Label order (must match model training label order):
 *   0=Games, 1=Health, 2=Music, 3=News, 4=Others,
 *   5=Payments, 6=Shopping, 7=Social, 8=Travel
 *
 * If the model file is not present in assets/, this classifier returns null
 * and the HybridClassifier falls back to NaiveBayes + RuleEngine.
 *
 * To activate: place your trained model at app/src/main/assets/app_classifier.tflite
 */
class TFLiteClassifier(private val context: Context) {

    companion object {
        private const val TAG = "TFLiteClassifier"
        private const val MODEL_FILE = "app_classifier.tflite"
        private const val VOCAB_FILE = "vocab.txt"

        private val LABELS = listOf(
            "Games", "Health", "Music", "News", "Others",
            "Payments", "Shopping", "Social", "Travel"
        )
    }

    private var interpreter: Interpreter? = null
    private var vocab: Map<String, Int> = emptyMap()
    val isAvailable: Boolean get() = interpreter != null

    init {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)
            interpreter = Interpreter(modelBuffer)
            vocab = loadVocab()
            Log.i(TAG, "TFLite model loaded successfully")
        } catch (e: IOException) {
            Log.i(TAG, "No TFLite model found in assets/ — using fallback classifier")
        } catch (e: Exception) {
            Log.e(TAG, "TFLite init error: ${e.message}")
        }
    }

    /** Returns null if model is not loaded. */
    fun classify(appName: String, packageName: String): ClassificationResult? {
        val interp = interpreter ?: return null
        if (vocab.isEmpty()) return null

        val inputText = "$appName $packageName"
        val inputTensor = vectorize(inputText)
        val outputTensor = Array(1) { FloatArray(LABELS.size) }

        interp.run(inputTensor, outputTensor)

        val probs = outputTensor[0]
        val bestIdx = probs.indices.maxByOrNull { probs[it] } ?: return null
        val confidence = probs[bestIdx]

        return ClassificationResult(
            category = LABELS[bestIdx],
            confidence = confidence
        )
    }

    private fun vectorize(text: String): Array<FloatArray> {
        val vocabSize = vocab.size.coerceAtLeast(1)
        val vector = FloatArray(vocabSize)
        val tokens = TextVectorizer.tokenize(text)
        for (token in tokens) {
            val idx = vocab[token] ?: continue
            if (idx < vocabSize) vector[idx] += 1f
        }
        // L2 normalize
        val norm = Math.sqrt(vector.map { it * it }.sum().toDouble()).toFloat()
        if (norm > 0f) for (i in vector.indices) vector[i] /= norm
        return arrayOf(vector)
    }

    private fun loadVocab(): Map<String, Int> {
        return try {
            context.assets.open(VOCAB_FILE).bufferedReader().useLines { lines ->
                lines.mapIndexed { idx, line -> line.trim() to idx }
                    .filter { it.first.isNotBlank() }
                    .toMap()
            }
        } catch (e: IOException) {
            emptyMap()
        }
    }

    fun close() {
        interpreter?.close()
    }
}
