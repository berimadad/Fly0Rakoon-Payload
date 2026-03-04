package com.android.system.update;

import android.app.Application;
import android.util.Log;

import com.android.system.update.services.ConnectionService;

public class AppController extends Application {
    private static final String TAG = "AppController";
    private static ConnectionService connectionService;
    private static AppController instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "AppController created");
    }

    public static synchronized ConnectionService getConnectionService() {
        return connectionService;
    }

    public static synchronized void setConnectionService(ConnectionService service) {
        connectionService = service;
    }

    public static synchronized void clearConnectionService() {
        connectionService = null;
    }

    public static AppController getInstance() {
        return instance;
    }
}
