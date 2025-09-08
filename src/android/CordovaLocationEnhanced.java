package com.outsystems.cordova.locationenhanced;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

public class CordovaLocationEnhanced extends CordovaPlugin {

    private static final int REQUEST_LOCATION_PERMISSIONS = 100;
    private static final String COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION;

    private LocationManager locationManager;
    private Map<String, CallbackContext> watchCallbacks = new HashMap<>();
    private LocationListener watchLocationListener;
    private CallbackContext permissionCallback;

    private ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> currentPositionTimeout;
    private CallbackContext currentPositionCallback;
    private LocationListener currentPositionListener;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.locationManager = (LocationManager) this.cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("isLocationEnabled".equals(action)) {
            this.isLocationEnabled(callbackContext);
            return true;
        } else if ("getPermissionStatus".equals(action)) {
            this.getPermissionStatus(callbackContext);
            return true;
        } else if ("getAccuracyLevel".equals(action)) {
            this.getAccuracyLevel(callbackContext);
            return true;
        } else if ("watchLocation".equals(action)) {
            this.watchLocation(callbackContext);
            return true;
        } else if ("clearWatch".equals(action)) {
            String watchId = args.getString(0);
            this.clearWatch(watchId, callbackContext);
            return true;
        } else if ("requestPermission".equals(action)) {
            String accuracyLevel = args.getString(0);
            this.requestPermission(accuracyLevel, callbackContext);
            return true;
        } else if ("getCurrentPosition".equals(action)) {
            JSONObject options = args.getJSONObject(0);
            this.getCurrentPosition(options, callbackContext);
            return true;
        }
        return false;
    }

    private void isLocationEnabled(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                Context context = this.cordova.getActivity().getApplicationContext();
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                boolean isLocationEnabled = isGpsEnabled || isNetworkEnabled;
                callbackContext.success(isLocationEnabled ? 1 : 0);
            } catch (Exception e) {
                sendError(callbackContext, "LOCATION_ERROR", "Error checking location status: " + e.getMessage());
            }
        });
    }

    private void getPermissionStatus(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                Context context = this.cordova.getActivity().getApplicationContext();
                JSONObject result = new JSONObject();

                int fineLocation = ContextCompat.checkSelfPermission(context, FINE_LOCATION);
                int coarseLocation = ContextCompat.checkSelfPermission(context, COARSE_LOCATION);

                if (fineLocation == PackageManager.PERMISSION_GRANTED) {
                    result.put("status", "Granted (Precise)");
                } else if (coarseLocation == PackageManager.PERMISSION_GRANTED) {
                    result.put("status", "Granted (Approximate)");
                } else {
                    result.put("status", "Denied");
                }
                callbackContext.success(result);
            } catch (Exception e) {
                sendError(callbackContext, "PERMISSION_ERROR", "Error getting permission status: " + e.getMessage());
            }
        });
    }

    private void getAccuracyLevel(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                Context context = this.cordova.getActivity().getApplicationContext();
                JSONObject result = new JSONObject();

                int fineLocation = ContextCompat.checkSelfPermission(context, FINE_LOCATION);

                if (fineLocation == PackageManager.PERMISSION_GRANTED) {
                    result.put("accuracyLevel", "Precise");
                } else {
                    result.put("accuracyLevel", "Approximate");
                }
                callbackContext.success(result);
            } catch (Exception e) {
                sendError(callbackContext, "ACCURACY_ERROR", "Error getting accuracy level: " + e.getMessage());
            }
        });
    }

    private void watchLocation(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                if (!hasPermissions(new String[]{FINE_LOCATION, COARSE_LOCATION})) {
                    sendError(callbackContext, "PERMISSION_DENIED", "Location permission not granted.");
                    return;
                }

                final String watchId = UUID.randomUUID().toString();
                watchCallbacks.put(watchId, callbackContext);

                // If a watch is already active, remove the old listener
                if (this.watchLocationListener != null) {
                    locationManager.removeUpdates(this.watchLocationListener);
                }

                // Create a new location listener
                this.watchLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        try {
                            JSONObject result = new JSONObject();
                            result.put("latitude", location.getLatitude());
                            result.put("longitude", location.getLongitude());
                            result.put("accuracy", location.getAccuracy());
                            result.put("altitude", location.getAltitude());
                            result.put("timestamp", location.getTime());

                            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                            pluginResult.setKeepCallback(true);
                            callbackContext.sendPluginResult(pluginResult);
                        } catch (JSONException e) {
                            sendError(callbackContext, "JSON_ERROR", "JSON Exception: " + e.getMessage());
                        }
                    }
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override
                    public void onProviderEnabled(String provider) {}
                    @Override
                    public void onProviderDisabled(String provider) {
                        sendError(callbackContext, "PROVIDER_DISABLED", "Location provider " + provider + " is disabled.");
                    }
                };

                // Request location updates
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this.watchLocationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this.watchLocationListener);

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, watchId);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            } catch (SecurityException e) {
                sendError(callbackContext, "PERMISSION_DENIED", "Location permission not granted: " + e.getMessage());
            } catch (Exception e) {
                sendError(callbackContext, "WATCH_ERROR", "Error watching location: " + e.getMessage());
            }
        });
    }

    private void clearWatch(String watchId, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            if (watchCallbacks.containsKey(watchId)) {
                // If this is the last watch, stop location updates
                if (watchCallbacks.size() == 1) {
                    if (this.watchLocationListener != null) {
                        locationManager.removeUpdates(this.watchLocationListener);
                        this.watchLocationListener = null;
                    }
                }
                watchCallbacks.remove(watchId);
                callbackContext.success("Location watch cleared.");
            } else {
                sendError(callbackContext, "WATCH_NOT_FOUND", "Watch ID not found.");
            }
        });
    }

    private void requestPermission(String accuracyLevel, CallbackContext callbackContext) {
        this.permissionCallback = callbackContext;
        String[] permissions;

        if ("precise".equalsIgnoreCase(accuracyLevel)) {
            permissions = new String[]{FINE_LOCATION, COARSE_LOCATION};
        } else {
            permissions = new String[]{COARSE_LOCATION};
        }

        if (hasPermissions(permissions)) {
            this.permissionCallback.success("Permissions already granted.");
        } else {
            cordova.requestPermissions(this, REQUEST_LOCATION_PERMISSIONS, permissions);
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    allGranted = false;
                    break;
                }
            }
            if (permissionCallback != null) {
                if (allGranted) {
                    permissionCallback.success("Permissions granted.");
                } else {
                    sendError(permissionCallback, "PERMISSION_DENIED", "Permissions denied by user.");
                }
            }
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this.cordova.getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void getCurrentPosition(final JSONObject options, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                long timeout = options.optLong("timeout", 0);
                long maxAge = options.optLong("maximumAge", 0);
                String accuracyLevel = options.optString("accuracyLevel", "precise");

                if (!hasPermissions(new String[]{FINE_LOCATION, COARSE_LOCATION})) {
                    sendError(callbackContext, "PERMISSION_DENIED", "Location permission not granted.");
                    return;
                }

                String provider = "precise".equalsIgnoreCase(accuracyLevel) ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
                Location lastLocation = locationManager.getLastKnownLocation(provider);

                if (lastLocation != null && (System.currentTimeMillis() - lastLocation.getTime()) < maxAge) {
                    sendLocationResult(lastLocation, callbackContext);
                    return;
                }

                currentPositionCallback = callbackContext;
                currentPositionListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        stopCurrentPositionUpdates();
                        sendLocationResult(location, currentPositionCallback);
                    }
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override
                    public void onProviderEnabled(String provider) {}
                    @Override
                    public void onProviderDisabled(String provider) {
                        stopCurrentPositionUpdates();
                        sendError(currentPositionCallback, "PROVIDER_DISABLED", "Location provider " + provider + " is disabled.");
                    }
                };

                locationManager.requestSingleUpdate(provider, currentPositionListener, null);

                if (timeout > 0) {
                    currentPositionTimeout = timeoutExecutor.schedule(() -> {
                        stopCurrentPositionUpdates();
                        sendError(currentPositionCallback, "TIMEOUT", "Timeout getting location.");
                    }, timeout, TimeUnit.MILLISECONDS);
                }
            } catch (SecurityException e) {
                sendError(callbackContext, "PERMISSION_DENIED", "Location permission not granted: " + e.getMessage());
            } catch (Exception e) {
                sendError(callbackContext, "GET_CURRENT_POSITION_ERROR", "Error getting current position: " + e.getMessage());
            }
        });
    }

    private void sendLocationResult(Location location, CallbackContext callbackContext) {
        try {
            JSONObject jsonLocation = new JSONObject();
            jsonLocation.put("latitude", location.getLatitude());
            jsonLocation.put("longitude", location.getLongitude());
            jsonLocation.put("accuracy", location.getAccuracy());
            jsonLocation.put("altitude", location.getAltitude());
            jsonLocation.put("timestamp", location.getTime());
            callbackContext.success(jsonLocation);
        } catch (JSONException e) {
            sendError(callbackContext, "JSON_ERROR", "Error creating JSON response: " + e.getMessage());
        }
    }

    private void stopCurrentPositionUpdates() {
        if (currentPositionListener != null) {
            locationManager.removeUpdates(currentPositionListener);
            currentPositionListener = null;
        }
        if (currentPositionTimeout != null) {
            currentPositionTimeout.cancel(true);
            currentPositionTimeout = null;
        }
    }

    private void sendError(CallbackContext callbackContext, String errorCode, String errorMessage) {
        try {
            JSONObject errorObject = new JSONObject();
            errorObject.put("errorCode", errorCode);
            errorObject.put("errorMessage", errorMessage);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, errorObject);
            callbackContext.sendPluginResult(pluginResult);
        } catch (JSONException e) {
            // Fallback in case JSON fails
            callbackContext.error("Error creating JSON response for error: " + e.getMessage());
        }
    }
}
