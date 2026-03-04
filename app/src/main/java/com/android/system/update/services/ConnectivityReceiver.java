package com.android.system.update.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

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
            // Start ConnectionService when network becomes available
            Intent serviceIntent = new Intent(context, ConnectionService.class);
            context.startService(serviceIntent);
            
            Intent foregroundIntent = new Intent(context, ForegroundService.class);
            context.startService(foregroundIntent);
        }
    }
}
