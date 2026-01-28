package com.example.fitlink.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a sports group in the FitLink app.
 */
public class Group implements Serializable {

    private String id;              // Unique ID from Firebase
    private String name;            // Group Name
    private String description;     // Group Description
    private SportType sportType;    // Restricted to RUNNING, CYCLING, SWIMMING
    private String level;           // e.g., Beginner, Intermediate, Advanced
    private String location;        // Meeting point or city
    private String adminId;         // The UID of the user who created the group

    // שינוי ל-Map למניעת כפילויות ושיפור ביצועים ב-Firebase
    private Map<String, Boolean> members;

    // Required empty constructor for Firebase
    public Group() {
        this.members = new HashMap<>();
    }

    public Group(String id, String name, String description, SportType sportType, String level, String location, String adminId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sportType = sportType;
        this.level = level;
        this.location = location;
        this.adminId = adminId;
        this.members = new HashMap<>();
        // The creator is the first member
        this.members.put(adminId, true);
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SportType getSportType() {
        return sportType;
    }

    public void setSportType(SportType sportType) {
        this.sportType = sportType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public Map<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Boolean> members) {
        this.members = members;
    }

    /**
     * Helper method to add a new member to the group
     */
    public void addMember(String userId) {
        if (this.members == null) {
            this.members = new HashMap<>();
        }
        // במפה, put פשוט יחליף את הערך הקיים אם ה-ID כבר קיים, מה שמונע כפילויות באופן טבעי
        this.members.put(userId, true);
    }
}