package com.locationtracker;

public class LocationData {
    public String userId;
    public double latitude;
    public double longitude;
    public long timestamp;
    public float accuracy;

    public LocationData(String userId, double latitude, double longitude,
                      long timestamp, float accuracy) {
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.accuracy = accuracy;
    }
}
