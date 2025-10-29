# Typing Speed Tutor

Typing Speed Tutor is a JavaFX desktop application for Windows that blends fast typing drills with guided lessons. Kotlin drives the UI flow and session handling, while Java provides the shared domain model and lesson catalogue. The app emphasises accuracy-first habits so learners build repeatable muscle memory before accelerating their words per minute.

## Latest Release

- **Version:** `v0.1.0` (commit `90d7c3f`), published by @Lintshiwe
- **Key updates:** TypeFast-branded splash screen, guided onboarding tour, refreshed theme, multi-phase typing sessions with countdown alerts, and adaptive coaching tips.
- **Installer:** [Typing.Speed.Test-0.1.0.msi](https://github.com/Lintshiwe/Typing-Speed-Test/releases/download/v0.1.0/Typing.Speed.Test-0.1.0.msi) – 42.1 MB (SHA-256 `f05ed3c2699a86f00c66fa0996b4620a8c0276ad92ce444c181ce3acce38481c`)
- **Source archives:** [zip](https://github.com/Lintshiwe/Typing-Speed-Test/archive/refs/tags/v0.1.0.zip) · [tar.gz](https://github.com/Lintshiwe/Typing-Speed-Test/archive/refs/tags/v0.1.0.tar.gz)

## Table of Contents

- [Latest Release](#latest-release)
- [Highlights](#highlights)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Build & Test](#build--test)
- [Run the App](#run-the-app)
- [Package a Windows Installer](#package-a-windows-installer)
- [Educational Design](#educational-design)
- [Project Layout](#project-layout)
- [Roadmap](#roadmap)
- [Troubleshooting](#troubleshooting)

## Highlights

- **Quick Test tab** – run precision drills or assessments with instant WPM, accuracy, and error feedback.
- **Lesson Paths tab** – browse curated modules with coaching notes covering posture, rhythm, and deliberate pacing.
- **Progress tab** – review captured sessions in a sortable table and receive adaptive coaching prompts based on trends.
- **Cross-language codebase** – Kotlin powers the JavaFX UI and session logic; Java supplies reusable models and the seeded lesson library.
- **Unit-tested metrics** – WPM, accuracy, and error statistics are covered by JUnit tests for trustworthy feedback.

## Prerequisites

- Java 17 or newer (ensure `JAVA_HOME` points to this JDK)
- Maven 3.9+
- Windows 10/11 desktop (JavaFX dependencies are resolved with the `win` classifier)

Optional tooling:

- [WiX Toolset 3.x](https://wixtoolset.org/) for packaging `.exe` installers with `jpackage`
- VS Code tasks (already committed) for running Maven verify cycles from the Command Palette

## Quick Start

1. Clone the repository and open it in VS Code.
2. Ensure `JAVA_HOME` is set to your JDK 17+ installation.
3. Resolve dependencies and run the verification build:
   ```bash
   mvn -Djavafx.platform=win clean verify
   ```
4. Launch the JavaFX application:
   ```bash
   mvn -Djavafx.platform=win javafx:run
   ```

## Build & Test

The default build compiles Kotlin and Java sources, runs unit tests, and verifies JavaFX resources:

```bash
mvn -Djavafx.platform=win test
```

In VS Code you can trigger the preconfigured **Run Maven Tests** task for the same cycle.

## Run the App

Use the JavaFX Maven plugin to start the desktop UI:

```bash
mvn -Djavafx.platform=win javafx:run
```

The command launches the application with the correct JavaFX platform modules on Windows. If you encounter lingering JVM or JavaFX processes during development, use `scripts/clean_target_and_kill.bat` to terminate stray runs and clear the Maven `target` directory.

## Package a Windows Installer

Bundling distributable installers uses `jpackage` in conjunction with the WiX Toolset 3.x. Generate an `.exe` installer with:

```bash
mvn -Pinstaller -Djavafx.platform=win clean verify
```

Or build the `.msi` installer that ships with the GitHub release:

```bash
mvn -Pinstaller-msi -Djavafx.platform=win clean verify
```

Add `-Djpackage.verbose=true` if you need additional diagnostics. After a successful build, inspect `target/installer` for the signed application image and both installer formats.

## Educational Design

- **Warmups** focus on home-row muscle memory, finger reach comfort, and posture cues.
- **Accuracy builders** emphasise tricky spelling, punctuation timing, and deliberate tempo adjustments.
- **Fluency runs** introduce longer passages with mindfulness reminders that reinforce breathing and rhythm.

Every completed session captures statistics and contextual coaching tips, prompting learners to reflect on accuracy before pushing speed goals.

## Project Layout

```text
src/main/java/com/typingspeed/model      // Shared Java domain models
src/main/java/com/typingspeed/lesson     // Java-based lesson catalogue seed data
src/main/kotlin/com/typingspeed          // Kotlin JavaFX UI and session orchestration
src/test/kotlin/com/typingspeed          // JUnit tests covering metrics and helpers
scripts/clean_target_and_kill.bat        // Utility script to reset local dev state on Windows
```

## Roadmap

- Expand the lesson catalogue with age-specific or thematic content.
- Persist session histories (e.g., JSON or embedded database) to track long-term progress.
- Introduce adaptive difficulty that schedules lessons based on recent accuracy trends.
- Add export/share options so learners can review their progress outside the app.

## Troubleshooting

- **Build fails with missing JavaFX modules** – verify `JAVA_HOME` targets JDK 17+ and the `-Djavafx.platform=win` flag is present.
- **Installer build cannot find `candle.exe`/`light.exe`** – ensure the WiX Toolset `bin` directory is on your `PATH` before invoking the installer profile.
- **App relaunch fails after a crash** – run `scripts/clean_target_and_kill.bat` to stop orphaned processes and clean the build output.
