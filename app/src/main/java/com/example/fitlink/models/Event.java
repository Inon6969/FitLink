package com.example.fitlink.models;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a specific scheduled event.
 * It can be linked to a Group or act as an independent/public event.
 */
public class Event implements Serializable {

    private String id;                 // Unique ID from Firebase
    private String groupId;            // The ID of the group this event belongs to (null/empty if independent)
    private String title;              // Title of the event (e.g., "Morning 5K Run")
    private String description;        // Details about the event

    // Newly added fields for independent events
    private SportType sportType;       // Type of sport
    private DifficultyLevel level;     // Difficulty of the event

    private long startTimestamp;       // Start date and time in milliseconds (Epoch)
    private long durationMillis;       // Expected duration of the event in milliseconds (הפורמט החדש והנכון)
    private Location location;         // Meeting point / Location of the event
    private String creatorId;          // The UID of the user (admin) who created the event
    private int maxParticipants;       // Maximum allowed participants (0 means unlimited)

    // Map of user IDs who have joined the event (Key = userId, Value = true)
    private Map<String, Boolean> participants;

    // Required empty constructor for Firebase
    public Event() {
        this.participants = new HashMap<>();
    }

    public Event(String id, String groupId, String title, String description,
                 SportType sportType, DifficultyLevel level, long startTimestamp,
                 long durationMillis, Location location, String creatorId, int maxParticipants) {
        this.id = id;
        this.groupId = groupId;
        this.title = title;
        this.description = description;
        this.sportType = sportType;
        this.level = level;
        this.startTimestamp = startTimestamp;
        this.durationMillis = durationMillis;
        this.location = location;
        this.creatorId = creatorId;
        this.maxParticipants = maxParticipants;
        this.participants = new HashMap<>();

        // Add the creator as the first participant automatically
        this.participants.put(creatorId, true);
    }

    // --- Helper Methods ---

    /**
     * Checks if the event is independent (not linked to any specific group).
     * @return true if independent, false if it belongs to a group.
     */
    @Exclude
    public boolean isIndependent() {
        return groupId == null || groupId.trim().isEmpty();
    }

    /**
     * מחזיר את משך הזמן כטקסט קריא ויפה למשתמש (לדוגמה: "1h 30m" או "45m")
     */
    @Exclude
    public String getFormattedDuration() {
        long minutes = (durationMillis / (1000 * 60)) % 60;
        long hours = (durationMillis / (1000 * 60 * 60)) % 24;

        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return minutes + "m";
        }
    }

    /**
     * מחשב ומחזיר את תאריך ושעת הסיום של האירוע במילישניות
     */
    @Exclude
    public long getEndTimestamp() {
        return startTimestamp + durationMillis;
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
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

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Map<String, Boolean> getParticipants() {
        return participants;
    }

    public void setParticipants(Map<String, Boolean> participants) {
        this.participants = participants;
    }

    public void addParticipant(String userId) {
        if (this.participants == null) {
            this.participants = new HashMap<>();
        }
        this.participants.put(userId, true);
    }

    public void removeParticipant(String userId) {
        if (this.participants != null) {
            this.participants.remove(userId);
        }
    }

    @Exclude
    public int getParticipantsCount() {
        return participants != null ? participants.size() : 0;
    }
}