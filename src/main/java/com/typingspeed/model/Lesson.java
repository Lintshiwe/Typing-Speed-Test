package com.typingspeed.model;

import java.util.Objects;

public final class Lesson {
    private final String id;
    private final String title;
    private final String focusArea;
    private final String passage;
    private final String coachingTip;

    public Lesson(String id, String title, String focusArea, String passage, String coachingTip) {
        this.id = id;
        this.title = title;
        this.focusArea = focusArea;
        this.passage = passage;
        this.coachingTip = coachingTip;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFocusArea() {
        return focusArea;
    }

    public String getPassage() {
        return passage;
    }

    public String getCoachingTip() {
        return coachingTip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lesson lesson = (Lesson) o;
        return Objects.equals(id, lesson.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title + " — " + focusArea;
    }
}
