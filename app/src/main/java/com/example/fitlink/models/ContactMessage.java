package com.example.fitlink.models;

public class ContactMessage {
    private String id;
    private String userId; // השדה החדש
    private String name;
    private String email;
    private String phone;
    private String message;
    private long timestamp;

    public ContactMessage() {
        // קונסטרקטור ריק חובה עבור Firebase
    }

    public ContactMessage(String id, String userId, String name, String email, String phone, String message, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    } // Getter חדש

    public void setUserId(String userId) {
        this.userId = userId;
    } // Setter חדש

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}