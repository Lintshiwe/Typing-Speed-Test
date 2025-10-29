package com.typingspeed.model

import java.time.Duration
import java.time.Instant

/** Stores the outcome of a completed typing session for reporting. */
data class SessionResult(
    val lessonId: String,
    val lessonTitle: String,
    val startedAt: Instant,
    val duration: Duration,
    val wordsPerMinute: Double,
    val accuracy: Double,
    val errorCount: Int
)
