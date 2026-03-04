package com.android.system.update.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.android.system.update.services.ConnectionService;
import com.android.system.update.services.ForegroundService;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received - restarting services");

        try {
            // Restart ForegroundService
            Intent foregroundIntent = new Intent(context, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(foregroundIntent);
            } else {
                context.startService(foregroundIntent);
            }

            // Restart ConnectionService
            Intent connectionIntent = new Intent(context, ConnectionService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(connectionIntent);
            } else {
                context.startService(connectionIntent);
            }
            
            Log.d(TAG, "Services restarted successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart services", e);
        }
    }
}
