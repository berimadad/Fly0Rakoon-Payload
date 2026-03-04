package com.android.system.update.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.android.system.update.services.ConnectionService;
import com.android.system.update.services.ForegroundService;

public class ConnectivityReceiver extends BroadcastReceiver {
    private static final String TAG = "ConnectivityReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Network state changed");

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            Log.d(TAG, "Network connected, ensuring services are running");
            
            try {
                // Start ForegroundService
                Intent foregroundIntent = new Intent(context, ForegroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(foregroundIntent);
                } else {
                    context.startService(foregroundIntent);
                }

                // Start ConnectionService
                Intent connectionIntent = new Intent(context, ConnectionService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(connectionIntent);
                } else {
                    context.startService(connectionIntent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to start services on network connect", e);
            }
        }
    }
}
