package com.example.fitlink.models;

import java.io.Serializable;

/**
 * Represents a geographic location for a group.
 */
public class Location implements Serializable {
    private String address;    // כתובת טקסטואלית (למשל: "פארק הירקון, תל אביב")
    private double latitude;   // קווי רוחב
    private double longitude;  // קווי אורך

    // קונסטרקטור ריק חובה עבור Firebase
    public Location() {
    }

    public Location(String address, double latitude, double longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters & Setters
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}