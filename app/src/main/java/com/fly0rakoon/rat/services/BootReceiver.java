package com.fly0rakoon.rat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        Log.d(TAG, "Received broadcast: " + intent.getAction());

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals(Intent.ACTION_REBOOT) ||
                intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON") ||
                intent.getAction().equals("android.intent.action.LOCKED_BOOT_COMPLETED")) {

            try {
                Log.d(TAG, "Starting ConnectionService after boot");
                Intent serviceIntent = new Intent(context, ConnectionService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to start service after boot", e);
            }
        }
    }
}
