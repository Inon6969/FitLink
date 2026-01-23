package com.example.fitlink.models;

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
}