package com.typingspeed.ui

import com.typingspeed.lesson.LessonLibrary
import com.typingspeed.logic.ProgressTracker
import com.typingspeed.logic.TypingSessionManager
import com.typingspeed.model.Lesson
import com.typingspeed.model.SessionResult
import javafx.animation.FadeTransition
import javafx.animation.KeyFrame
import javafx.animation.PauseTransition
import javafx.animation.Timeline
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Effect
import javafx.scene.input.MouseEvent
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.stage.Stage
import javafx.event.EventHandler
import javafx.util.Callback
import javafx.util.Duration as FxDuration
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class TypingTutorApp : Application() {
    private val lessonLibrary = LessonLibrary()
    private val progressTracker = ProgressTracker()
    private val sessionManager = TypingSessionManager(progressTracker)
    private val scoreboard = Label()
    private val backgroundPool = Executors.newSingleThreadScheduledExecutor()
    private val progressEntries = FXCollections.observableArrayList<SessionResult>()
    private val statusMessage = SimpleStringProperty("Welcome! Launch the guided tour to get oriented.")
    private val options = AdvancedOptions()
    private val preferences = Preferences.userNodeForPackage(TypingTutorApp::class.java)

    private lateinit var rootStack: StackPane
    private lateinit var chrome: BorderPane
    private lateinit var navigationCoach: VBox
    private lateinit var advancedPane: VBox
    private lateinit var heroBanner: VBox
    private lateinit var mainTabPane: TabPane
    private lateinit var statusBar: HBox
    private lateinit var timerLabel: Label
    private lateinit var timerProgress: ProgressBar
    private lateinit var phaseLabel: Label
    private var sessionCountdownTimeline: Timeline? = null
    private var sessionDurationSeconds: Int = 0
    private var sessionWarningIssued = false
    private var appLogo: Image? = null
    private var tourOverlay: StackPane? = null
    private val originalEffects = mutableMapOf<Node, Effect?>()

    private val themeClasses = mapOf(
        "Sky" to "theme-sky",
        "Midnight" to "theme-midnight",
        "Contrast" to "theme-contrast"
    )

    override fun start(stage: Stage) {
        appLogo = loadAppLogo()
        stage.icons.clear()
        appLogo?.let { stage.icons.add(it) }
        stage.title = "Typing Speed Test"

        showSplash(stage) {
            initializeMainScene(stage)
        }
    }

    private fun showSplash(stage: Stage, onReady: () -> Unit) {
        val splashRoot = StackPane().apply {
            styleClass.add("splash-root")
            padding = Insets(32.0)
        }

        val logoView = appLogo?.let {
            ImageView(it).apply {
                fitWidth = 200.0
                isPreserveRatio = true
                styleClass.add("splash-logo")
            }
        }

        val loadingLabel = Label("Loading Typing Speed Test...").apply {
            styleClass.add("splash-text")
        }

        val progress = ProgressIndicator().apply {
            progress = ProgressIndicator.INDETERMINATE_PROGRESS
            styleClass.add("splash-indicator")
        }

        val content = VBox(18.0).apply {
            alignment = Pos.CENTER
            styleClass.add("splash-card")
            children += listOfNotNull(logoView, loadingLabel, progress)
        }

        splashRoot.children += content

        val scene = Scene(splashRoot, 640.0, 420.0)
        scene.stylesheets += javaClass.getResource("/styles/app.css").toExternalForm()
        stage.scene = scene
        stage.centerOnScreen()
        stage.show()

        PauseTransition(FxDuration.seconds(1.8)).apply {
            setOnFinished { onReady() }
        }.play()
    }

    private fun initializeMainScene(stage: Stage) {
        rootStack = StackPane().apply {
            styleClass.addAll("app-root", themeClasses.getValue(options.theme.value))
            padding = Insets(18.0)
        }

        chrome = buildChrome(stage)
        rootStack.children += chrome

        val scene = Scene(rootStack, 1180.0, 760.0)
        scene.stylesheets += javaClass.getResource("/styles/app.css").toExternalForm()

        rootStack.opacity = 0.0
        stage.scene = scene
        stage.centerOnScreen()

        FadeTransition(FxDuration.millis(450.0), rootStack).apply {
            fromValue = 0.0
            toValue = 1.0
        }.play()

        progressEntries.setAll(progressTracker.recentSessions(20))
        refreshScoreboard()
        hookOptionListeners()
        updateStatusBar(statusMessage.value)
        showWelcomeTourIfNeeded()
    }

    private fun loadAppLogo(): Image? {
        val resource = javaClass.getResource("/images/app-logo.png") ?: return null
        return Image(resource.toExternalForm())
    }

    override fun stop() {
        backgroundPool.shutdownNow()
    }

    private fun buildChrome(stage: Stage): BorderPane {
        val pane = BorderPane()
        pane.padding = Insets(12.0)
        heroBanner = buildHeroBanner()
        pane.top = VBox(16.0, buildMenuBar(stage), heroBanner)

        navigationCoach = buildNavigationCoachPanel()
        advancedPane = createAdvancedOptionsPane()

        pane.left = navigationCoach
        pane.right = advancedPane
        pane.center = buildContentCard()
        statusBar = buildStatusBar()
        pane.bottom = statusBar

        return pane
    }

    private fun buildMenuBar(stage: Stage): MenuBar {
        val fileMenu = Menu("File").apply {
            items.add(MenuItem("Exit").apply { setOnAction { stage.close() } })
        }

        val viewMenu = Menu("View").apply {
            val focusItem = CheckMenuItem("Focus Mode").apply {
                selectedProperty().bindBidirectional(options.focusMode)
            }
            items.add(focusItem)
        }

        val helpMenu = Menu("Help").apply {
            items.addAll(
                MenuItem("Guided Tour").apply { setOnAction { showGuidedTour(false) } },
                SeparatorMenuItem(),
                MenuItem("About").apply {
                    setOnAction {
                        Alert(Alert.AlertType.INFORMATION).apply {
                            title = "About Typing Speed Tutor"
                            headerText = "Typing Speed Tutor"
                            contentText = "Blend warmups, accuracy drills, and fluency runs while the app coaches you on posture and pacing."
                        }.showAndWait()
                    }
                }
            )
        }

        return MenuBar(fileMenu, viewMenu, helpMenu)
    }

    private fun buildHeroBanner(): VBox {
        scoreboard.font = Font.font(20.0)
        val logoView = appLogo?.let {
            ImageView(it).apply {
                fitWidth = 48.0
                isPreserveRatio = true
                styleClass.add("hero-logo")
            }
        }

        val title = Label("Typing Speed Test").apply {
            styleClass.add("headline")
        }
        val tagline = Label("Build confident typing habits with guided practice, live metrics, and actionable coaching.").apply {
            styleClass.add("subheadline")
            isWrapText = true
        }

        val titleRow = HBox(12.0).apply {
            alignment = Pos.CENTER_LEFT
            children.addAll(listOfNotNull(logoView, title))
        }

        return VBox(10.0, titleRow, scoreboard, tagline).apply {
            styleClass.add("hero-banner")
        }
    }

    private fun buildContentCard(): VBox {
        mainTabPane = buildTabbedContent()
        return VBox(mainTabPane).apply {
            styleClass.add("card-surface")
            padding = Insets(12.0)
        }
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

        val phaseSelector = ComboBox<String>(FXCollections.observableArrayList("Easy", "Medium", "Hard")).apply {
            value = options.difficulty.value
            valueProperty().bindBidirectional(options.difficulty)
            tooltip = Tooltip("Choose the coaching intensity for this run.")
        }

        phaseLabel = Label().apply {
            styleClass.add("phase-label")
            isWrapText = true
        }
        updatePhaseSummary()
        options.difficulty.addListener { _, _, _ -> updatePhaseSummary() }

        timerLabel = Label("Timer: --").apply {
            styleClass.add("timer-label")
        }
        timerProgress = ProgressBar(1.0).apply {
            styleClass.add("timer-progress")
            prefWidth = 220.0
        }
        val timerBox = HBox(12.0, timerLabel, timerProgress).apply {
            alignment = Pos.CENTER_LEFT
        }

        val targetArea = TextArea()
        targetArea.isEditable = false
        targetArea.isWrapText = true
        targetArea.promptText = "Start a session to load your practice text."

        val typingArea = TextArea()
        typingArea.promptText = "Type the text above here..."
        typingArea.isWrapText = true
        VBox.setVgrow(typingArea, Priority.ALWAYS)

        val resetButton = Button("Reset")
        val startButton = Button("Start Lesson").apply { styleClass.add("cta") }
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
            beginSession(lesson, targetArea, typingArea, statusLabel, completeButton, timerLabel, timerProgress)
        }

        completeButton.setOnAction {
            val result = sessionManager.completeSession()
            handleSessionResult(result, typingArea, statusLabel)
            completeButton.isDisable = true
        }

        resetButton.setOnAction {
            sessionManager.currentLesson()?.let {
                beginSession(it, targetArea, typingArea, statusLabel, completeButton, timerLabel, timerProgress)
            }
        }

        typingArea.textProperty().addListener { _, _, newValue ->
            sessionManager.updateInput(newValue)
        }

        val controls = HBox(12.0, lessonTypeBox, phaseSelector, startButton, completeButton, resetButton)
        controls.alignment = Pos.CENTER_LEFT

        val layout = VBox(12.0,
            controls,
            phaseLabel,
            timerBox,
            Label("Practice Text"),
            targetArea,
            Label("Your Typing"),
            typingArea,
            statusLabel
        )
        layout.padding = Insets(12.0)
        layout.prefHeight = 640.0
        layout.styleClass.add("card-surface")

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
                beginSession(lesson, null, null, null, null, timerLabel, timerProgress)
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
        content.styleClass.add("card-surface")

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
        layout.styleClass.add("card-surface")

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
        completeButton: Button?,
        timerLabel: Label?,
        timerBar: ProgressBar?
    ) {
        stopSessionTimer("Timer: --")
        sessionManager.beginSession(lesson)
        sessionDurationSeconds = determineSessionDuration()
        prepareTimerDisplay(timerLabel, timerBar, sessionDurationSeconds)
        targetArea?.text = lesson.passage
        typingArea?.clear()
        typingArea?.isDisable = options.countdownEnabled.get()
        val difficulty = options.difficulty.value
        statusLabel?.text = "Typing '${lesson.title}'. Focus: ${lesson.focusArea}. Phase: $difficulty."
        completeButton?.isDisable = false
        updateStatusBar("Session primed: ${lesson.title} ($difficulty phase). Aim for ${options.targetWpm.value} WPM with ${lesson.focusArea} awareness.")

        if (options.countdownEnabled.get() && typingArea != null) {
            showCountdownOverlay {
                typingArea.isDisable = false
                typingArea.requestFocus()
                updateStatusBar("Go! Keep accuracy above ${options.targetWpm.value}% to unlock speed gains.")
                startSessionTimer(timerLabel, timerBar, typingArea, completeButton)
            }
        } else {
            typingArea?.isDisable = false
            typingArea?.requestFocus()
            startSessionTimer(timerLabel, timerBar, typingArea, completeButton)
        }
    }

    private fun handleSessionResult(result: SessionResult, typingArea: TextArea, statusLabel: Label) {
        stopSessionTimer("Timer: 00:00")
        typingArea.isDisable = false
        val summary = "${result.wordsPerMinute.roundToInt()} WPM | ${result.accuracy.roundToInt()}% accuracy | ${result.errorCount} errors"
        statusLabel.text = "Session complete: $summary"
        progressEntries.setAll(progressTracker.recentSessions(20))
        refreshScoreboard()
        updateStatusBar("Great work! ${result.lessonTitle} logged at ${result.wordsPerMinute.roundToInt()} WPM.")
    }

    private fun refreshScoreboard() {
        val avgWpm = progressTracker.averageWpm()
        val avgAccuracy = progressTracker.averageAccuracy()
        val streak = if (progressTracker.streakActiveWithin(24)) "Streak active" else "Start today’s streak"
        val target = options.targetWpm.value
        scoreboard.text = "Averages • WPM ${avgWpm.roundToInt()} | Accuracy ${avgAccuracy.roundToInt()}% | Target $target WPM | $streak"
    }

    private fun determineSessionDuration(): Int = when (options.difficulty.value) {
        "Easy" -> 60
        "Medium" -> 90
        "Hard" -> 120
        else -> 75
    }

    private fun prepareTimerDisplay(timerLabel: Label?, timerBar: ProgressBar?, durationSeconds: Int) {
        timerLabel?.styleClass?.remove("timer-warning")
        timerLabel?.text = "Timer: ${formatDuration(durationSeconds)}"
        timerBar?.progress = 1.0
        sessionWarningIssued = false
    }

    private fun startSessionTimer(
        timerLabel: Label?,
        timerBar: ProgressBar?,
        typingArea: TextArea?,
        completeButton: Button?
    ) {
        sessionCountdownTimeline?.stop()
        if (sessionDurationSeconds <= 0) {
            timerLabel?.text = "Timer: --"
            timerBar?.progress = 0.0
            return
        }
        var remaining = sessionDurationSeconds
        timerLabel?.text = "Timer: ${formatDuration(remaining)}"
        timerBar?.progress = 1.0
        sessionWarningIssued = false

        sessionCountdownTimeline = Timeline(KeyFrame(FxDuration.seconds(1.0), EventHandler {
            remaining = max(remaining - 1, 0)
            timerLabel?.text = "Timer: ${formatDuration(remaining)}"
            timerBar?.progress = if (sessionDurationSeconds == 0) 0.0 else remaining.toDouble() / sessionDurationSeconds

            if (!sessionWarningIssued && remaining in 1..TIMER_WARNING_THRESHOLD) {
                sessionWarningIssued = true
                timerLabel?.styleClass?.add("timer-warning")
                updateStatusBar("Heads up: ${remaining}s remain in this run. Hold accuracy steady!")
            }

            if (remaining == 0) {
                stopSessionTimer("Timer: 00:00")
                typingArea?.isDisable = true
                if (completeButton != null && !completeButton.isDisable) {
                    completeButton.fire()
                }
            }
        })).apply {
            cycleCount = sessionDurationSeconds
        }
        sessionCountdownTimeline?.playFromStart()
    }

    private fun stopSessionTimer(finalLabel: String) {
        sessionCountdownTimeline?.stop()
        sessionCountdownTimeline = null
        sessionWarningIssued = false
        if (::timerLabel.isInitialized) {
            timerLabel.styleClass.remove("timer-warning")
            timerLabel.text = finalLabel
        }
        if (::timerProgress.isInitialized) {
            timerProgress.progress = 0.0
        }
    }

    private fun formatDuration(totalSeconds: Int): String {
        val seconds = max(totalSeconds, 0)
        val minutesPart = seconds / 60
        val secondsPart = seconds % 60
        return String.format("%02d:%02d", minutesPart, secondsPart)
    }

    private fun updatePhaseSummary() {
        if (!::phaseLabel.isInitialized) return
        val difficulty = options.difficulty.value
        val (duration, guidance) = when (difficulty) {
            "Easy" -> 60 to "Gentle pace to build accuracy and rhythm."
            "Medium" -> 90 to "Balanced challenge—maintain posture and precision."
            "Hard" -> 120 to "Extended push for endurance and speed bursts."
            else -> 75 to "Custom pacing—breathe, focus, and stay accurate."
        }
        phaseLabel.text = "Phase: $difficulty • ${duration}s focus interval. $guidance"
        if (::timerLabel.isInitialized && sessionCountdownTimeline == null) {
            timerLabel.styleClass.remove("timer-warning")
            timerLabel.text = "Timer: ${formatDuration(duration)}"
        }
        if (::timerProgress.isInitialized && sessionCountdownTimeline == null) {
            timerProgress.progress = 1.0
        }
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

    private fun buildNavigationCoachPanel(): VBox {
        val header = Label("Navigation Coach").apply {
            styleClass.add("section-title")
        }

        val steps = listOf(
            "Quick Test" to "Launch a focused drill with live WPM and accuracy feedback. Hit Start to load text, then Complete Session when done.",
            "Lesson Paths" to "Browse curated warmups, accuracy builders, and fluency runs. Activate one, then return to Quick Test to type it.",
            "Progress" to "Review your history, compare runs, and read coaching notes that adapt to your latest performance."
        )

        val accordion = Accordion().apply {
            panes.addAll(steps.map { (title, body) ->
                val label = Label(body).apply {
                    isWrapText = true
                    style = "-fx-text-fill: #1f3a4d;"
                }
                TitledPane(title, VBox(label).apply { spacing = 8.0 })
            })
            expandedPane = panes.firstOrNull()
        }

        val tourButton = Button("Start Guided Tour").apply {
            styleClass.add("cta")
            setOnAction { showGuidedTour(false) }
        }

        return VBox(16.0, header, accordion, tourButton).apply {
            styleClass.addAll("card-surface", "nav-coach-panel")
            padding = Insets(18.0)
            prefWidth = 260.0
        }
    }

    private fun createAdvancedOptionsPane(): VBox {
        val countdownToggle = CheckBox("3-second countdown before typing").apply {
            selectedProperty().bindBidirectional(options.countdownEnabled)
            tooltip = Tooltip("Give your hands time to settle before a run starts.")
        }

        val focusToggle = CheckBox("Focus mode (hide side panels)").apply {
            selectedProperty().bindBidirectional(options.focusMode)
        }

        val targetLabel = Label().apply {
            text = "Target WPM: ${options.targetWpm.value}"
        }
        val targetSlider = Slider(20.0, 120.0, options.targetWpm.value.toDouble()).apply {
            isShowTickLabels = true
            isShowTickMarks = true
            majorTickUnit = 20.0
            minorTickCount = 1
            blockIncrement = 5.0
            valueProperty().addListener { _, _, newValue ->
                options.targetWpm.set(newValue.toInt())
                targetLabel.text = "Target WPM: ${options.targetWpm.value}"
                refreshScoreboard()
            }
        }

        val themeChoice = ChoiceBox<String>(FXCollections.observableArrayList(themeClasses.keys)).apply {
            selectionModel.select(options.theme.value)
            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                if (newValue != null) options.theme.set(newValue)
            }
        }

        options.theme.addListener { _, _, newValue ->
            if (themeChoice.selectionModel.selectedItem != newValue) {
                themeChoice.selectionModel.select(newValue)
            }
        }

        val comfortPane = TitledPane("Session Comfort", VBox(10.0, countdownToggle, focusToggle))
        val displayPane = TitledPane("Display & Goals", VBox(12.0, targetLabel, targetSlider, Label("Theme"), themeChoice))

        return VBox(16.0, comfortPane, displayPane).apply {
            styleClass.addAll("card-surface", "advanced-pane")
            padding = Insets(18.0)
            prefWidth = 280.0
        }
    }

    private fun buildStatusBar(): HBox {
        val messageLabel = Label().apply {
            textProperty().bind(statusMessage)
            isWrapText = true
            maxWidth = Double.MAX_VALUE
        }
        val spacer = Region()
        HBox.setHgrow(messageLabel, Priority.ALWAYS)
        HBox.setHgrow(spacer, Priority.ALWAYS)

        val tourButton = Button("Guided Tour").apply {
            setOnAction { showGuidedTour(false) }
        }

        return HBox(12.0, messageLabel, spacer, tourButton).apply {
            styleClass.add("status-bar")
        }
    }

    private fun showCountdownOverlay(onComplete: () -> Unit) {
        val overlay = StackPane().apply { styleClass.add("countdown-overlay") }
        val counter = Label("3").apply { styleClass.add("countdown-label") }
        overlay.children += counter
        rootStack.children += overlay

        val timeline = Timeline().apply {
            keyFrames.setAll(
                listOf(
                    KeyFrame(FxDuration.ZERO, EventHandler { counter.text = "3" }),
                    KeyFrame(FxDuration.seconds(1.0), EventHandler { counter.text = "2" }),
                    KeyFrame(FxDuration.seconds(2.0), EventHandler { counter.text = "1" }),
                    KeyFrame(
                        FxDuration.seconds(3.0),
                        EventHandler {
                            rootStack.children.remove(overlay)
                            onComplete()
                        }
                    )
                )
            )
        }
        timeline.play()
    }

    private fun hookOptionListeners() {
        options.focusMode.addListener { _, _, enabled -> toggleFocusMode(enabled) }
        options.theme.addListener { _, oldValue, newValue -> applyTheme(oldValue, newValue) }
    }

    private fun toggleFocusMode(enabled: Boolean) {
        val panels = listOf(navigationCoach, advancedPane)
        panels.forEach {
            it.isVisible = !enabled
            it.isManaged = !enabled
        }
        if (enabled) {
            if (!rootStack.styleClass.contains("focus-mode")) rootStack.styleClass.add("focus-mode")
            updateStatusBar("Focus mode enabled. Side panels are hidden for distraction-free practice.")
        } else {
            rootStack.styleClass.remove("focus-mode")
            updateStatusBar("Focus mode cleared. Explore the navigation coach or advanced options anytime.")
        }
    }

    private fun applyTheme(oldValue: String?, newValue: String?) {
        val oldClass = themeClasses[oldValue]
        val newClass = themeClasses[newValue] ?: themeClasses.getValue("Sky")
        if (oldClass != null) {
            rootStack.styleClass.remove(oldClass)
        }
        if (!rootStack.styleClass.contains(newClass)) {
            rootStack.styleClass.add(newClass)
        }
        updateStatusBar("Theme switched to ${newValue ?: "Sky"}.")
    }

    private fun updateStatusBar(message: String) {
        statusMessage.set(message)
    }

    private data class GuidedTourStep(val title: String, val description: String, val targetSupplier: () -> Node?)

    private fun showGuidedTour(allowSkip: Boolean) {
        if (!::heroBanner.isInitialized || !::mainTabPane.isInitialized || !::navigationCoach.isInitialized) {
            return
        }
        if (tourOverlay != null) {
            return
        }

        val steps = listOf(
            GuidedTourStep("Hero Banner", "Track your averages, streaks, and motivational cues.") { heroBanner },
            GuidedTourStep("Quick Test", "Launch drills, adjust difficulty, and monitor live metrics.") {
                mainTabPane.selectionModel.select(0)
                mainTabPane.selectionModel.selectedItem?.content ?: mainTabPane
            },
            GuidedTourStep("Navigation Coach", "Follow the coach’s recommendations for guided lessons.") { navigationCoach },
            GuidedTourStep("Lesson Paths", "Browse curated passages with coaching insights before you type.") {
                mainTabPane.selectionModel.select(1)
                mainTabPane.selectionModel.selectedItem?.content ?: mainTabPane
            },
            GuidedTourStep("Progress Dashboard", "Review history, coaching notes, and celebrate streaks.") {
                mainTabPane.selectionModel.select(2)
                mainTabPane.selectionModel.selectedItem?.content ?: mainTabPane
            },
            GuidedTourStep("Advanced Options", "Customize countdowns, focus mode, themes, and targets.") { advancedPane },
            GuidedTourStep("Status Bar", "Watch for live tutor cues and relaunch the tour anytime.") { statusBar }
        )

        if (steps.isEmpty()) {
            return
        }

        val overlay = StackPane().apply {
            styleClass.add("tour-overlay")
            isPickOnBounds = true
        }
        val scrim = Rectangle().apply {
            styleClass.add("tour-scrim")
            widthProperty().bind(rootStack.widthProperty())
            heightProperty().bind(rootStack.heightProperty())
            addEventFilter(MouseEvent.MOUSE_PRESSED) { it.consume() }
        }
        overlay.children += scrim

        val titleLabel = Label().apply { styleClass.add("tour-card-title") }
        val descriptionLabel = Label().apply {
            isWrapText = true
            styleClass.add("tour-card-body")
        }
        val counterLabel = Label().apply { styleClass.add("tour-counter") }

        val backButton = Button("Back")
        val nextButton = Button("Next")
        val finishButton = Button("Finish")
        val closeButton = Button(if (allowSkip) "Skip" else "Close")

        val controls = HBox(10.0, backButton, closeButton, nextButton, finishButton).apply {
            alignment = Pos.CENTER_RIGHT
        }

        val skipCheckbox = if (allowSkip) CheckBox("Don't show this guide automatically") else null

        val card = VBox(12.0).apply {
            styleClass.add("tour-card")
            prefWidth = 360.0
            children += listOf(titleLabel, descriptionLabel, counterLabel, controls)
            skipCheckbox?.let { children.add(children.size - 1, it) }
        }

        overlay.children += card
        rootStack.children += overlay
        tourOverlay = overlay

        var index = 0

        fun highlightNode(node: Node, description: String) {
            clearTourHighlights()
            originalEffects[node] = node.effect
            if (!node.styleClass.contains("tour-highlight")) {
                node.styleClass.add("tour-highlight")
            }
            node.effect = DropShadow(28.0, Color.web("#40b5ad", 0.75))
            updateStatusBar("Tutor tip: $description")
        }

        fun repositionCard(node: Node) {
            if (node.scene == null) {
                return
            }
            val sceneBounds = node.localToScene(node.boundsInLocal)
            val rootBounds = rootStack.sceneToLocal(sceneBounds)
            val cardWidth = card.prefWidth
            val availableWidth = rootStack.width
            val left = rootBounds.minX.coerceIn(24.0, availableWidth - cardWidth - 24.0)
            val belowSpace = rootStack.height - (rootBounds.maxY + 24.0)
            val aboveSpace = rootBounds.minY - 24.0
            val cardHeight = if (card.height > 0) card.height else 220.0
            val top = if (belowSpace >= cardHeight || belowSpace >= aboveSpace) {
                (rootBounds.maxY + 24.0).coerceAtMost(rootStack.height - cardHeight - 24.0)
            } else {
                (rootBounds.minY - cardHeight - 24.0).coerceAtLeast(24.0)
            }
            StackPane.setAlignment(card, Pos.TOP_LEFT)
            StackPane.setMargin(card, Insets(top, 24.0, 24.0, left))
        }

        fun concludeTour() {
            skipCheckbox?.let {
                if (it.isSelected) {
                    preferences.putBoolean(TOUR_PREF_KEY, true)
                }
            }
            clearTourHighlights()
            rootStack.children.remove(overlay)
            tourOverlay = null
            updateStatusBar("Tour complete. You're ready to explore!")
        }

        fun updateStep() {
            val step = steps[index]
            val target = step.targetSupplier()
            if (target == null) {
                concludeTour()
                return
            }
            titleLabel.text = step.title
            descriptionLabel.text = step.description
            counterLabel.text = "${index + 1} of ${steps.size}"
            highlightNode(target, step.description)
            repositionCard(target)
            Platform.runLater { repositionCard(target) }

            backButton.isDisable = index == 0
            nextButton.isDisable = index >= steps.lastIndex
            nextButton.isVisible = index < steps.lastIndex
            nextButton.isManaged = nextButton.isVisible
            finishButton.isVisible = index == steps.lastIndex
            finishButton.isManaged = finishButton.isVisible
        }

        backButton.setOnAction {
            if (index > 0) {
                index -= 1
                updateStep()
            }
        }

        nextButton.setOnAction {
            if (index < steps.lastIndex) {
                index += 1
                updateStep()
            }
        }

        finishButton.setOnAction { concludeTour() }
        closeButton.setOnAction { concludeTour() }

        updateStep()
    }

    private fun clearTourHighlights() {
        originalEffects.forEach { (node, effect) ->
            node.styleClass.remove("tour-highlight")
            node.effect = effect
        }
        originalEffects.clear()
    }

    private fun showWelcomeTourIfNeeded() {
        val dismissed = preferences.getBoolean(TOUR_PREF_KEY, false)
        if (!dismissed) {
            Platform.runLater {
                showGuidedTour(true)
            }
        }
    }

    companion object {
        private const val TOUR_PREF_KEY = "onboarding.tourDismissed"
        private const val TIMER_WARNING_THRESHOLD = 10
    }
}

fun main() {
    Application.launch(TypingTutorApp::class.java)
}
