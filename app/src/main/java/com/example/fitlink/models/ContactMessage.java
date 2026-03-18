package com.example.fitlink.models;

public class ContactMessage {
    private String id;
    private String name;
    private String email;
    private String phone; // השדה החדש
    private String message;
    private long timestamp;

    public ContactMessage() {
        // קונסטרקטור ריק חובה עבור Firebase
    }

    public ContactMessage(String id, String name, String email, String phone, String message, long timestamp) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone; // אתחול
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; } // Getter
    public void setPhone(String phone) { this.phone = phone; } // Setter

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}