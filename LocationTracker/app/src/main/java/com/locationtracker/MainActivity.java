package com.locationtracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String LOCATION_WORK_TAG = "location_work_tag";

    private UserPreferences userPreferences;
    private TextView userIdTextView;
    private Button startTrackingButton;
    private Button stopTrackingButton;
    private WorkManager workManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userPreferences = new UserPreferences(this);
        workManager = WorkManager.getInstance(this);

        initViews();
        setupClickListeners();

        // Generate user ID if not exists
        if (userPreferences.getUserId().isEmpty()) {
            String userId = generateUserId();
            userPreferences.setUserId(userId);
        }

        updateUI();
        checkPermissions();
    }

    private void initViews() {
        userIdTextView = findViewById(R.id.userIdTextView);
        startTrackingButton = findViewById(R.id.startTrackingButton);
        stopTrackingButton = findViewById(R.id.stopTrackingButton);
    }

    private void setupClickListeners() {
        startTrackingButton.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                startLocationTracking();
            } else {
                requestPermissions();
            }
        });

        stopTrackingButton.setOnClickListener(v -> stopLocationTracking());
    }

    private void updateUI() {
        String userId = userPreferences.getUserId();
        userIdTextView.setText("User ID: " + userId);

        boolean isTracking = userPreferences.isTrackingEnabled();
        startTrackingButton.setEnabled(!isTracking);
        stopTrackingButton.setEnabled(isTracking);
    }

    private String generateUserId() {
        return "USER_" + UUID.randomUUID().toString();
    }

    private void checkPermissions() {
        if (!hasAllPermissions()) {
            requestPermissions();
        }
    }

    private boolean hasAllPermissions() {
        boolean fineLocation = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocation = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean backgroundLocation = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        return fineLocation && coarseLocation && backgroundLocation;
    }

    private void requestPermissions() {
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            };
        } else {
            permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions required for location tracking",
                    Toast.LENGTH_LONG).show();
            }

            updateUI();
        }
    }

    private void startLocationTracking() {
        if (hasAllPermissions()) {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
                    LocationWorker.class, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build();

            workManager.enqueueUniquePeriodicWork(
                    LOCATION_WORK_TAG,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    periodicWorkRequest
            );

            userPreferences.setTrackingEnabled(true);
            updateUI();
            Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationTracking() {
        workManager.cancelUniqueWork(LOCATION_WORK_TAG);
        userPreferences.setTrackingEnabled(false);
        updateUI();
        Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
}
