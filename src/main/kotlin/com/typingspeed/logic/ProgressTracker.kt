package com.typingspeed.logic

import com.typingspeed.model.SessionResult
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

class ProgressTracker {
    private val sessions = CopyOnWriteArrayList<SessionResult>()

    fun record(result: SessionResult) {
        sessions += result
    }

    fun history(): List<SessionResult> = sessions.sortedByDescending { it.startedAt }

    fun averageWpm(): Double = sessions.map { it.wordsPerMinute }.averageOrZero()

    fun averageAccuracy(): Double = sessions.map { it.accuracy }.averageOrZero()

    fun recentSessions(limit: Int = 10): List<SessionResult> = history().take(limit)

    fun lastSession(): SessionResult? = sessions.maxByOrNull(SessionResult::startedAt)

    fun streakActiveWithin(hours: Long): Boolean {
        val cutoff = Instant.now().minusSeconds(hours * 3600)
        return sessions.any { it.startedAt.isAfter(cutoff) }
    }

    private fun Iterable<Double>.averageOrZero(): Double = if (this.none()) 0.0 else this.average()
}
