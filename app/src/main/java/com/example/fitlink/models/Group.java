package com.example.fitlink.models;

import com.example.fitlink.enums.DifficultyLevel;
import com.example.fitlink.enums.SportType;

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
    private DifficultyLevel level;  // Difficulty Level using the new Enum
    private Location location;      // Meeting point or city
    private String creatorId;       // The UID of the user who created the group (formerly adminId)

    private String groupImage;      // תמונת הקבוצה בפורמט Base64

    private Map<String, Boolean> members;
    private Map<String, Boolean> managers;
    private Map<String, Boolean> pendingRequests;

    // Required empty constructor for Firebase
    public Group() {
        this.members = new HashMap<>();
        this.managers = new HashMap<>();
        this.pendingRequests = new HashMap<>();
    }

    public Group(String id, String name, String description, SportType sportType, DifficultyLevel level, Location location, String creatorId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sportType = sportType;
        this.level = level;
        this.location = location;
        this.creatorId = creatorId;

        this.members = new HashMap<>();
        // The creator is the first member
        this.members.put(creatorId, true);

        this.managers = new HashMap<>();
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

    public DifficultyLevel getLevel() {
        return level;
    }

    public void setLevel(DifficultyLevel level) {
        this.level = level;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getGroupImage() {
        return groupImage;
    }

    public void setGroupImage(String groupImage) {
        this.groupImage = groupImage;
    }

    public Map<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Boolean> members) {
        this.members = members;
    }

    public void addMember(String userId) {
        if (this.members == null) {
            this.members = new HashMap<>();
        }
        this.members.put(userId, true);
    }

    // --- Managers Getters and Setters ---

    public Map<String, Boolean> getManagers() {
        return managers;
    }

    public void setManagers(Map<String, Boolean> managers) {
        this.managers = managers;
    }

    public Map<String, Boolean> getPendingRequests() {
        return pendingRequests;
    }

    public void setPendingRequests(Map<String, Boolean> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }
}