package com.locationtracker;

import android.util.Log;

import retrofit2.Call;
import retrofit2.Response;

public class LocationRepository {

    private static final String TAG = "LocationRepository";
    private final ApiService apiService;

    public LocationRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public ApiResponse sendLocation(LocationData locationData) throws IOException {
        Call<ApiResponse> call = apiService.sendLocation(locationData);
        Response<ApiResponse> response = call.execute();

        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        } else {
            throw new IOException("Server error: " + response.code() + " " + response.message());
        }
    }
}
