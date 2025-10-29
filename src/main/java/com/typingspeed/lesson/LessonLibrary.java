package com.typingspeed.lesson;

import com.typingspeed.model.Lesson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class LessonLibrary {
    private final List<Lesson> warmups = new ArrayList<>();
    private final List<Lesson> accuracyBuilders = new ArrayList<>();
    private final List<Lesson> fluencyRuns = new ArrayList<>();
    private final Random random = new Random();

    public LessonLibrary() {
        seedWarmups();
        seedAccuracy();
        seedFluency();
    }

    public List<Lesson> getWarmups() {
        return Collections.unmodifiableList(warmups);
    }

    public List<Lesson> getAccuracyBuilders() {
        return Collections.unmodifiableList(accuracyBuilders);
    }

    public List<Lesson> getFluencyRuns() {
        return Collections.unmodifiableList(fluencyRuns);
    }

    public List<Lesson> getAllLessons() {
        List<Lesson> all = new ArrayList<>(warmups);
        all.addAll(accuracyBuilders);
        all.addAll(fluencyRuns);
        return Collections.unmodifiableList(all);
    }

    public Lesson randomWarmup() {
        return warmups.get(random.nextInt(warmups.size()));
    }

    public Lesson randomAccuracyBuilder() {
        return accuracyBuilders.get(random.nextInt(accuracyBuilders.size()));
    }

    public Lesson randomFluencyRun() {
        return fluencyRuns.get(random.nextInt(fluencyRuns.size()));
    }

    private void seedWarmups() {
        warmups.add(new Lesson(
                "warmup_home_row",
                "Home Row Flow",
                "Home row keys",
                "asdf jkl; asdf jkl; keep a gentle curve in your fingers as you glide across the home row",
                "Keep your wrists lifted and strike the keys with light taps. Say the letters softly to reinforce muscle memory."
        ));
        warmups.add(new Lesson(
                "warmup_numbers",
                "Number Pad Rhythm",
                "Number reach",
                "123 789 456 012 practice stretching from the home row while staying relaxed",
                "Glance only with your eyes. Try to keep your palms centered over F and J while reaching for numbers."
        ));
    }

    private void seedAccuracy() {
        accuracyBuilders.add(new Lesson(
                "accuracy_tricky_pairs",
                "Tricky Letter Pairs",
                "Common reversals",
                "receive believe achieve perceive relieve conceive deceive",
                "Focus on the ie/ei pattern. Slow down intentionally and keep accuracy above 95% before adding speed."
        ));
        accuracyBuilders.add(new Lesson(
                "accuracy_punctuation",
                "Punctuation Patrol",
                "Symbols and rhythm",
                "Where does the question mark go? Does the exclamation point shout or sing? Practice makes punctuation pleasant!",
                "Say the punctuation names as you type them. Build a habit of pressing shift with the opposite hand."
        ));
    }

    private void seedFluency() {
        fluencyRuns.add(new Lesson(
                "fluency_story",
                "Guided Story Sprint",
                "Storytelling",
                "The quick brown fox thanked the patient hound for sharing mindful breathing with the bustling forest.",
                "Keep a calm pace. Smooth breathing supports consistent keystrokes; inhale every two lines."
        ));
        fluencyRuns.add(new Lesson(
                "fluency_fact",
                "STEM Fact Blast",
                "Scientific vocabulary",
                "Photosynthesis powers plants with sunlight, water, and carbon dioxide, producing energy that fuels entire ecosystems.",
                "Break longer words into syllables as you type: pho-to-syn-the-sis. Accuracy first, speed follows."
        ));
    }
}
