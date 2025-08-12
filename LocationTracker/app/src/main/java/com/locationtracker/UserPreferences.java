package com.locationtracker;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPreferences {

    private static final String PREF_NAME = "LocationTrackerPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_TRACKING_ENABLED = "tracking_enabled";

    private SharedPreferences sharedPreferences;

    public UserPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setUserId(String userId) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, "");
    }

    public void setTrackingEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_TRACKING_ENABLED, enabled).apply();
    }

    public boolean isTrackingEnabled() {
        return sharedPreferences.getBoolean(KEY_TRACKING_ENABLED, false);
    }
}
