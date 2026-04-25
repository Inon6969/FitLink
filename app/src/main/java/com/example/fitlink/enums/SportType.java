package com.example.fitlink.enums;

public enum SportType {
    RUNNING("Running"),
    CYCLING("Cycling"),
    SWIMMING("Swimming");

    private final String displayName;

    SportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // הוספנו את הפונקציה הזו כדי שהספינר יציג את הטקסט היפה
    @Override
    public String toString() {
        return displayName;
    }
}