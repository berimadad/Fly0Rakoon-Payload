package com.android.system.update.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.android.system.update.FakeActivity;

public class ForegroundService extends Service {
    private static final String TAG = "ForegroundService";
    private static final String CHANNEL_ID = "system_update_foreground";
    private static final int NOTIFICATION_ID = 102;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ForegroundService created");
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, FakeActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Update")
                .setContentText("Optimizing system performance")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "System Update Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Maintains background operation");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ForegroundService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
