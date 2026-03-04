package com.android.system.update.services;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class PermissionRequestActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean allGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            finish();
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            Intent result = new Intent("PERMISSION_RESULT");
            for (int i = 0; i < permissions.length; i++) {
                result.putExtra(permissions[i],
                        grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(result);
        }
        finish();
    }
}
