package com.typingspeed.logic

import kotlin.math.max

/** Pure helpers for computing typing performance metrics. */
object TypingMetrics {
    fun wordsPerMinute(characterCount: Int, elapsedMillis: Long): Double {
        if (characterCount <= 0 || elapsedMillis <= 0) return 0.0
        val minutes = elapsedMillis / 60000.0
        val words = characterCount / 5.0
        return (words / minutes).coerceAtMost(350.0)
    }

    fun accuracyPercentage(target: String, typed: String): Pair<Double, Int> {
        if (target.isEmpty()) return 100.0 to 0
        val pairs = target.zip(typed)
        val correct = pairs.count { (expected, actual) -> expected == actual }
        val total = max(target.length, typed.length)
        val extraErrors = total - pairs.size
        val errors = (total - correct) + extraErrors
        val accuracy = if (total == 0) 100.0 else ((total - errors).toDouble() / total) * 100.0
        return accuracy.coerceIn(0.0, 100.0) to errors
    }
}
