package com.android.system.update.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class JobSchedulerService extends JobService {
    private static final String TAG = "JobSchedulerService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started, launching services");

        try {
            // Start ForegroundService
            Intent foregroundIntent = new Intent(this, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(foregroundIntent);
            } else {
                startService(foregroundIntent);
            }

            // Start ConnectionService
            Intent connectionIntent = new Intent(this, ConnectionService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(connectionIntent);
            } else {
                startService(connectionIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start services from job", e);
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job stopped");
        return false; // Don't reschedule if stopped
    }
}
