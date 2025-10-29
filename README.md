# Typing Speed Tutor

Typing Speed Tutor is a Windows desktop application that blends an interactive typing speed test with guided lessons. The experience is built with JavaFX for the user interface, uses both Java and Kotlin for the application logic, and emphasises accuracy-first habits for developing keyboarding fluency.

## Features

- **Quick Test tab** – run short practice or assessment sessions with instant feedback on words per minute, accuracy, and error counts.
- **Lesson Paths tab** – browse curated learning modules with coaching notes that reinforce posture, accuracy, and pacing fundamentals.
- **Progress tab** – review recorded sessions in a sortable table and receive adaptive coaching prompts based on recent performance.
- **Cross-language codebase** – Kotlin powers the JavaFX UI and session logic, while Java provides the lesson library and shared model classes.
- **Unit-tested metrics** – typing metrics such as WPM and accuracy are covered by JUnit tests to ensure reliable feedback for learners.

## Requirements

- Java 17 or newer (set `JAVA_HOME` accordingly)
- Maven 3.9+
- Windows desktop (the bundled JavaFX binaries target `win` classifiers)

## Build and Test

```bash
mvn -Djavafx.platform=win test
```

This command compiles Kotlin and Java sources and executes the JUnit test suite.

## Launch the Application

Use the JavaFX Maven plugin to run the desktop app:

```bash
mvn -Djavafx.platform=win javafx:run
```

Alternatively, in VS Code you can run the **Run Maven Tests** task (created in `.vscode/tasks.json`) to re-run the verification build, then use the same command above to start the UI.

## Build a Windows Installer

Creating an `.exe` installer uses `jpackage` and requires the [WiX Toolset](https://wixtoolset.org/) (version 3.x). Install WiX and add its `bin` directory (containing `candle.exe` and `light.exe`) to your `PATH`, then run:

```bash
mvn -Pinstaller -Djavafx.platform=win clean verify
```

After a successful run, the signed application image and installer are placed under `target/installer`.

## Educational Design

- Warmups target foundational muscle memory (home row drills, number reach).
- Accuracy builders emphasise tricky spelling, punctuation timing, and deliberate tempo.
- Fluency runs provide longer passages with mindfulness prompts to reinforce breathing and rhythm.

Each completed session records statistics and coaching tips, encouraging learners to reflect on accuracy before increasing speed.

## Project Structure

```text
src/main/java/com/typingspeed/model      // Shared Java model classes
src/main/java/com/typingspeed/lesson     // Lesson catalogue seeded in Java
src/main/kotlin/com/typingspeed          // Kotlin UI and session logic
src/test/kotlin/com/typingspeed          // JUnit tests for metrics
```

## Next Steps

- Expand the lesson catalogue with age- or topic-specific modules.
- Persist session history (e.g., JSON or embedded database) to track long-term growth.
- Add adaptive difficulty that schedules lessons based on recent accuracy trends.
