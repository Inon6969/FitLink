package com.example.fitlink.models;

import androidx.annotation.NonNull;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/// Model class for the user
/// This class represents a user in the application
/// It contains the user's information
///
/// @see Serializable
public class User implements Serializable {

    /// unique id of the user
    private String id;

    private String email, password;
    private String firstName, lastName;
    private String phone;
    private boolean isAdmin;
    private String profileImage;

    // מפה של מזהי קבוצות (Key = groupId, Value = true)
    private Map<String, Boolean> groupIds;

    public User() {
        this.groupIds = new HashMap<>();
    }

    public User(String id, String email, String password, String firstName, String lastName, String phone, boolean isAdmin, String profileImage) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.isAdmin = isAdmin;
        this.profileImage = profileImage;
        this.groupIds = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean admin) {
        isAdmin = admin;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    // Getters and Setters עבור הקבוצות
    public Map<String, Boolean> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(Map<String, Boolean> groupIds) {
        this.groupIds = groupIds;
    }

    /**
     * פונקציית עזר להוספת קבוצה למשתמש
     */
    public void addGroup(String groupId) {
        if (this.groupIds == null) {
            this.groupIds = new HashMap<>();
        }
        this.groupIds.put(groupId, true);
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", isAdmin=" + isAdmin +
                ", groupsCount=" + (groupIds != null ? groupIds.size() : 0) +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        User user = (User) object;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Exclude
    public String getFullName() {
        return firstName + " " + lastName;
    }
}