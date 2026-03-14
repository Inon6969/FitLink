package com.example.fitlink.models;

import java.io.Serializable;

public class Comment implements Serializable {

    private String id;
    private String eventId;
    private String userId;
    private String text;
    private long timestamp;

    // בנאי ריק חובה עבור Firebase
    public Comment() {
    }

    public Comment(String id, String eventId, String userId, String text, long timestamp) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}