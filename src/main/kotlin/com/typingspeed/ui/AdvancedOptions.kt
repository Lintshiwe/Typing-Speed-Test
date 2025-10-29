package com.typingspeed.ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

class AdvancedOptions {
    val countdownEnabled = SimpleBooleanProperty(true)
    val focusMode = SimpleBooleanProperty(false)
    val targetWpm = SimpleIntegerProperty(45)
    val theme = SimpleStringProperty("Sky")
    val difficulty = SimpleStringProperty("Easy")
}
