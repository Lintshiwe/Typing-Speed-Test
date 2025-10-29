package com.typingspeed.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TypingMetricsTest {
    @Test
    fun `words per minute uses standard five character words`() {
        val wpm = TypingMetrics.wordsPerMinute(characterCount = 250, elapsedMillis = 60000)
        assertEquals(50.0, wpm, 0.1)
    }

    @Test
    fun `accuracy identifies substitutions and omissions`() {
        val (accuracy, errors) = TypingMetrics.accuracyPercentage("hello world", "hxllo wurld")
    assertEquals(81.8, accuracy.roundToOneDecimal(), 0.1)
        assertEquals(2, errors)
    }

    private fun Double.roundToOneDecimal(): Double = kotlin.math.round(this * 10.0) / 10.0
}
