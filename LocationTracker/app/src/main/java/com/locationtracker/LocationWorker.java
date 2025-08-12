package com.locationtracker;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.concurrent.ExecutionException;

public class LocationWorker extends Worker {

    private static final String TAG = "LocationWorker";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationRepository locationRepository;
    private final UserPreferences userPreferences;
    private final Context context;

    public LocationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        locationRepository = new LocationRepository();
        userPreferences = new UserPreferences(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "LocationWorker started");

        setForegroundAsync(createForegroundInfo());

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return Result.failure();
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        try {
            Location location = Tasks.await(locationTask);
            if (location != null) {
                Log.d(TAG, "Location found: " + location.getLatitude() + ", " + location.getLongitude());
                String userId = userPreferences.getUserId();
                if (userId.isEmpty()) {
                    Log.e(TAG, "User ID is not set");
                    return Result.failure();
                }

                LocationData locationData = new LocationData(
                        userId,
                        location.getLatitude(),
                        location.getLongitude(),
                        System.currentTimeMillis(),
                        location.getAccuracy()
                );

                ApiResponse response = locationRepository.sendLocation(locationData);
                if (response.success) {
                    Log.d(TAG, "Location sent successfully");
                    return Result.success();
                } else {
                    Log.e(TAG, "Failed to send location: " + response.message);
                    return Result.failure();
                }
            } else {
                Log.w(TAG, "Location is null");
                return Result.failure();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get location or send it to server", e);
            return Result.failure();
        }
    }

    @NonNull
    private ForegroundInfo createForegroundInfo() {
        createNotificationChannel();
        Notification notification = createNotification();
        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for location tracking service");

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Location Tracker")
                .setContentText("Tracking your location...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}
