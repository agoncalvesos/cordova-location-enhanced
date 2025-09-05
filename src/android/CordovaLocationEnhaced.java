package com.outsystems.cordova.locationenhanced;

import android.content.Context;
import android.location.LocationManager;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

public class CordovaLocationEnhanced extends CordovaPlugin {

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
                callbackContext.error("Error checking location status: " + e.getMessage());
            }
        });
    }

    private void getPermissionStatus(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                Context context = this.cordova.getActivity().getApplicationContext();
                JSONObject result = new JSONObject();

                int fineLocation = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION);
                int coarseLocation = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION);

                if (fineLocation == PackageManager.PERMISSION_GRANTED) {
                    result.put("status", "Granted (Precise)");
                } else if (coarseLocation == PackageManager.PERMISSION_GRANTED) {
                    result.put("status", "Granted (Approximate)");
                } else {
                    result.put("status", "Denied");
                }
                
                callbackContext.success(result);
            } catch (Exception e) {
                callbackContext.error("Error getting permission status: " + e.getMessage());
            }
        });
    }

    private void getAccuracyLevel(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                Context context = this.cordova.getActivity().getApplicationContext();
                JSONObject result = new JSONObject();
                
                int fineLocation = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION);

                if (fineLocation == PackageManager.PERMISSION_GRANTED) {
                    result.put("accuracyLevel", "Precise");
                } else {
                    result.put("accuracyLevel", "Approximate");
                }
                
                callbackContext.success(result);
            } catch (Exception e) {
                callbackContext.error("Error getting accuracy level: " + e.getMessage());
            }
        });
    }

    private void watchLocation(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                final String watchId = callbackContext.getCallbackId();
                
                // If a watch is already active, remove the old listener
                if (this.locationListener != null) {
                    locationManager.removeUpdates(this.locationListener);
                }

                // Create a new location listener
                this.locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        try {
                            JSONObject result = new JSONObject();
                            result.put("latitude", location.getLatitude());
                            result.put("longitude", location.getLongitude());
                            result.put("accuracy", location.getAccuracy());
                            
                            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                            pluginResult.setKeepCallback(true); // Keep the callback active
                            callbackContext.sendPluginResult(pluginResult);
                        } catch (JSONException e) {
                            callbackContext.error("JSON Exception: " + e.getMessage());
                        }
                    }
                };

                // Request location updates
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this.locationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this.locationListener);

                watchCallbacks.put(watchId, callbackContext);
                
            } catch (SecurityException e) {
                callbackContext.error("Location permission not granted: " + e.getMessage());
            } catch (Exception e) {
                callbackContext.error("Error watching location: " + e.getMessage());
            }
        });
    }

    private void clearWatch(String watchId, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            if (watchCallbacks.containsKey(watchId)) {
                if (this.locationListener != null) {
                    locationManager.removeUpdates(this.locationListener);
                }
                watchCallbacks.remove(watchId);
                callbackContext.success("Location watch cleared.");
            } else {
                callbackContext.error("Watch ID not found.");
            }
        });
    }

}