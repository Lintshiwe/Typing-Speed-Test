package com.typingspeed.logic

import com.typingspeed.model.Lesson
import com.typingspeed.model.SessionResult
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

class TypingSessionManager(private val progressTracker: ProgressTracker) {
    private val activeLesson = AtomicReference<Lesson?>()
    private val sessionStart = AtomicReference<Instant?>()
    private val lastInput = AtomicReference("")

    fun beginSession(lesson: Lesson) {
        activeLesson.set(lesson)
        sessionStart.set(Instant.now())
        lastInput.set("")
    }

    fun updateInput(input: String) {
        lastInput.set(input)
    }

    fun completeSession(): SessionResult {
        val lesson = activeLesson.get() ?: error("No active lesson to complete.")
        val started = sessionStart.get() ?: error("Session start time missing.")
        val input = lastInput.get()
        val elapsed = Duration.between(started, Instant.now())
        val (accuracy, errors) = TypingMetrics.accuracyPercentage(lesson.passage, input)
        val wpm = TypingMetrics.wordsPerMinute(input.length, elapsed.toMillis())
        return SessionResult(
            lessonId = lesson.id,
            lessonTitle = lesson.title,
            startedAt = started,
            duration = elapsed,
            wordsPerMinute = wpm,
            accuracy = accuracy,
            errorCount = errors
        ).also { progressTracker.record(it) }
    }

    fun currentLesson(): Lesson? = activeLesson.get()

    fun currentInput(): String = lastInput.get()
}
