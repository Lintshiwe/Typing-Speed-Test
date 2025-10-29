package com.typingspeed.ui

import com.typingspeed.lesson.LessonLibrary
import com.typingspeed.logic.ProgressTracker
import com.typingspeed.logic.TypingSessionManager
import com.typingspeed.model.Lesson
import com.typingspeed.model.SessionResult
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.Stage
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class TypingTutorApp : Application() {
    private val lessonLibrary = LessonLibrary()
    private val progressTracker = ProgressTracker()
    private val sessionManager = TypingSessionManager(progressTracker)
    private val scoreboard = Label()
    private val backgroundPool = Executors.newSingleThreadScheduledExecutor()
    private val progressEntries = FXCollections.observableArrayList<SessionResult>()

    override fun start(stage: Stage) {
        val root = BorderPane()
        root.padding = Insets(16.0)
        root.top = buildScoreboardHeader()
        root.center = buildTabbedContent()

        stage.title = "Typing Speed Tutor"
        stage.scene = Scene(root, 1024.0, 720.0)
        stage.show()

        progressEntries.setAll(progressTracker.recentSessions(20))
        refreshScoreboard()
    }

    override fun stop() {
        backgroundPool.shutdownNow()
    }

    private fun buildScoreboardHeader(): VBox {
        scoreboard.font = Font.font(18.0)
        scoreboard.padding = Insets(12.0, 0.0, 12.0, 0.0)
        val intro = Label("Build accuracy first, then invite speed. Choose a module below to begin.")
        intro.style = "-fx-text-fill: #555555;"
        return VBox(scoreboard, intro)
    }

    private fun buildTabbedContent(): TabPane {
        val tabPane = TabPane()
        tabPane.tabs.addAll(
            createQuickTestTab(),
            createLessonPathTab(),
            createProgressTab()
        )
        tabPane.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        return tabPane
    }

    private fun createQuickTestTab(): Tab {
        val lessonTypeBox = ComboBox<String>()
        lessonTypeBox.items.addAll("Warmup", "Accuracy", "Fluency")
        lessonTypeBox.selectionModel.selectFirst()

        val targetArea = TextArea()
        targetArea.isEditable = false
    targetArea.isWrapText = true
        targetArea.promptText = "Start a session to load your practice text."

        val typingArea = TextArea()
        typingArea.promptText = "Type the text above here..."
    typingArea.isWrapText = true
        VBox.setVgrow(typingArea, Priority.ALWAYS)

        val resetButton = Button("Reset")
        val startButton = Button("Start Lesson")
        val completeButton = Button("Complete Session")
        completeButton.isDisable = true

        val statusLabel = Label("Choose a lesson type to begin.")
        statusLabel.style = "-fx-text-fill: #2a6f97;"

        startButton.setOnAction {
            val lesson = when (lessonTypeBox.value) {
                "Warmup" -> lessonLibrary.randomWarmup()
                "Accuracy" -> lessonLibrary.randomAccuracyBuilder()
                else -> lessonLibrary.randomFluencyRun()
            }
            beginSession(lesson, targetArea, typingArea, statusLabel, completeButton)
        }

        completeButton.setOnAction {
            val result = sessionManager.completeSession()
            handleSessionResult(result, typingArea, statusLabel)
            completeButton.isDisable = true
        }

        resetButton.setOnAction {
            sessionManager.currentLesson()?.let {
                beginSession(it, targetArea, typingArea, statusLabel, completeButton)
            }
        }

        typingArea.textProperty().addListener { _, _, newValue ->
            sessionManager.updateInput(newValue)
        }

        val controls = HBox(12.0, lessonTypeBox, startButton, completeButton, resetButton)
        controls.alignment = Pos.CENTER_LEFT

        val layout = VBox(12.0,
            controls,
            Label("Practice Text"),
            targetArea,
            Label("Your Typing"),
            typingArea,
            statusLabel
        )
        layout.padding = Insets(12.0)
        layout.prefHeight = 640.0

        return Tab("Quick Test", layout)
    }

    private fun createLessonPathTab(): Tab {
        val lessonList = ListView<Lesson>()
        lessonList.items = FXCollections.observableArrayList(lessonLibrary.allLessons)
        lessonList.setCellFactory {
            object : ListCell<Lesson>() {
                override fun updateItem(item: Lesson?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (item == null || empty) null else "${item.title} — ${item.focusArea}"
                }
            }
        }

        val coachingBox = TextArea()
        coachingBox.isEditable = false
        coachingBox.isWrapText = true

        lessonList.selectionModel.selectedItemProperty().addListener { _, _, lesson ->
            if (lesson != null) {
                coachingBox.text = buildString {
                    appendLine(lesson.passage)
                    appendLine()
                    append("Coach tip: ")
                    append(lesson.coachingTip)
                }
            }
        }

        val activateButton = Button("Start Selected Lesson")
        activateButton.setOnAction {
            val lesson = lessonList.selectionModel.selectedItem
            if (lesson != null) {
                beginSession(lesson, null, null, null, null)
                val alert = Alert(Alert.AlertType.INFORMATION)
                alert.title = "Lesson loaded"
                alert.headerText = lesson.title
                alert.contentText = "Switch to Quick Test to begin typing this lesson."
                alert.showAndWait()
            }
        }

        val pane = SplitPane(lessonList, VBox(12.0, Label("Lesson Content"), coachingBox, activateButton))
        pane.setDividerPositions(0.4)

        val content = VBox(12.0,
            Label("Choose a lesson to review guidance before you type."),
            pane
        )
        content.padding = Insets(12.0)

        return Tab("Lesson Paths", content)
    }

    private fun createProgressTab(): Tab {
        val table = TableView(progressEntries)

        val lessonCol = TableColumn<SessionResult, String>("Lesson")
        lessonCol.setCellValueFactory { SimpleStringProperty(it.value.lessonTitle) }
        lessonCol.prefWidth = 200.0

        val wpmCol = TableColumn<SessionResult, Number>("WPM")
        wpmCol.setCellValueFactory { SimpleDoubleProperty(it.value.wordsPerMinute.roundToInt().toDouble()) }

        val accuracyCol = TableColumn<SessionResult, String>("Accuracy")
        accuracyCol.setCellValueFactory {
            SimpleStringProperty("${it.value.accuracy.roundToInt()}%")
        }

        val durationCol = TableColumn<SessionResult, Number>("Seconds")
        durationCol.setCellValueFactory {
            SimpleLongProperty(it.value.duration.seconds)
        }

        val dateCol = TableColumn<SessionResult, String>("When")
        dateCol.setCellValueFactory {
            val formatter = DateTimeFormatter.ofPattern("MMM d HH:mm")
            SimpleStringProperty(it.value.startedAt.atZone(java.time.ZoneId.systemDefault()).format(formatter))
        }

        table.columns.setAll(lessonCol, wpmCol, accuracyCol, durationCol, dateCol)
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS

        val tipsArea = TextArea()
        tipsArea.isEditable = false
        tipsArea.isWrapText = true
        tipsArea.promptText = "Performance coaching will appear here after you complete a session."

        val layout = VBox(12.0, table, Label("Coaching Notes"), tipsArea)
        layout.padding = Insets(12.0)
        VBox.setVgrow(table, Priority.ALWAYS)

        backgroundPool.scheduleAtFixedRate({
            val last = progressTracker.lastSession()
            if (last != null) {
                val guidance = guidanceFor(last)
                Platform.runLater {
                    tipsArea.text = guidance
                }
            }
        }, 2, 5, TimeUnit.SECONDS)

        return Tab("Progress", layout)
    }

    private fun beginSession(
        lesson: Lesson,
        targetArea: TextArea?,
        typingArea: TextArea?,
        statusLabel: Label?,
        completeButton: Button?
    ) {
        sessionManager.beginSession(lesson)
        targetArea?.text = lesson.passage
        typingArea?.clear()
        typingArea?.requestFocus()
        statusLabel?.text = "Typing '${lesson.title}'. Focus: ${lesson.focusArea}."
        completeButton?.isDisable = false
    }

    private fun handleSessionResult(result: SessionResult, typingArea: TextArea, statusLabel: Label) {
        typingArea.isDisable = false
        val summary = "${result.wordsPerMinute.roundToInt()} WPM | ${result.accuracy.roundToInt()}% accuracy | ${result.errorCount} errors"
        statusLabel.text = "Session complete: $summary"
        progressEntries.setAll(progressTracker.recentSessions(20))
        refreshScoreboard()
    }

    private fun refreshScoreboard() {
        val avgWpm = progressTracker.averageWpm()
        val avgAccuracy = progressTracker.averageAccuracy()
        val streak = if (progressTracker.streakActiveWithin(24)) "Streak active" else "Start today’s streak"
        scoreboard.text = "Average WPM: ${avgWpm.roundToInt()} | Average Accuracy: ${avgAccuracy.roundToInt()}% | $streak"
    }

    private fun guidanceFor(result: SessionResult): String {
        val builder = StringBuilder()
        builder.appendLine("Lesson: ${result.lessonTitle}")
        builder.appendLine("Focus on ${result.accuracy.roundToInt()}% accuracy before chasing speed.")
        if (result.accuracy < 92) {
            builder.appendLine("Tip: Slow down slightly and breathe every other sentence.")
        } else if (result.wordsPerMinute < 40) {
            builder.appendLine("Tip: Practice warmups to build finger agility before longer runs.")
        } else {
            builder.appendLine("Great job! Try a fluency run to keep momentum.")
        }
        val durationSeconds = result.duration.seconds
        builder.append("Time on task: ${durationSeconds}s. Record at least three runs per day for steady growth.")
        return builder.toString()
    }
}

fun main() {
    Application.launch(TypingTutorApp::class.java)
}
