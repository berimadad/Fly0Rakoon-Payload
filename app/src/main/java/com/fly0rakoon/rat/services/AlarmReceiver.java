package com.fly0rakoon.rat.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    
    private static final String TAG = "AlarmReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received, ensuring service is running");
        
        // Try to start foreground service
        try {
            Intent serviceIntent = new Intent(context, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "Service started from alarm");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service from alarm: " + e.getMessage());
            
            // Try alternative service
            try {
                Intent altIntent = new Intent(context, ConnectionManager.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(altIntent);
                } else {
                    context.startService(altIntent);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Alternative also failed: " + ex.getMessage());
            }
        }
    }
}
