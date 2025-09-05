# cordova-location-enhanced

A Cordova plugin to check device location services, permission status, and continuously watch the user's location.

## Features

* **isLocationEnabled**: Checks if the device's location service (GPS/Network) is enabled from the device settings.

* **getPermissionStatus**: Returns the app's location permission status in a human-readable format.

* **getAccuracyLevel**: Checks if the user has granted precise or coarse location access.

* **watchLocation**: Starts continuously watching the user's location for changes.

* **clearWatch**: Stops a specific location watch.

## Installation

To install this plugin, run the following command in your Cordova project's root directory:

```bash
cordova plugin add https://github.com/agoncalvesos/cordova-location-enhanced
````

### iOS Configuration

For iOS, you must add the following descriptions to your `config.xml` file. These messages will be displayed to the user when the app requests location permissions.

```xml
<edit-config target="*-Info.plist" parent="NSLocationWhenInUseUsageDescription" mode="merge">
    <string>This app needs your location to provide features based on your current position.</string>
</edit-config>
<edit-config target="*-Info.plist" parent="NSLocationAlwaysAndWhenInUseUsageDescription" mode="merge">
    <string>This app needs your location to provide features even when the app is in the background.</string>
</edit-config>
```

## Usage

The plugin exposes a global `cordova.plugins.CordovaLocationEnhanced` object after the `deviceready` event has fired.

### `isLocationEnabled()`

Checks if the device's main location service is enabled.

```javascript
cordova.plugins.CordovaLocationEnhanced.isLocationEnabled(
    function(isEnabled) {
        if (isEnabled) {
            console.log('Location services are enabled.');
        } else {
            console.log('Location services are disabled.');
        }
    },
    function(error) {
        console.error('Error checking location status: ' + error);
    }
);
```

### `getPermissionStatus()`

Checks the app's location permission status.

```javascript
cordova.plugins.CordovaLocationEnhanced.getPermissionStatus(
    function(result) {
        console.log('Permission Status: ' + result.status);
    },
    function(error) {
        console.error('Error getting permission status: ' + error);
    }
);
```

### `getAccuracyLevel()`

Checks the granted accuracy level (Precise or Coarse).

```javascript
cordova.plugins.CordovaLocationEnhanced.getAccuracyLevel(
    function(result) {
        console.log('Accuracy Level: ' + result.accuracyLevel);
    },
    function(error) {
        console.error('Error getting accuracy level: ' + error);
    }
);
```

### `watchLocation()`

Starts watching the user's location for changes. This method returns a watch ID that you can use to clear the watch later.

```javascript
var watchID = cordova.plugins.CordovaLocationEnhanced.watchLocation(
    function(location) {
        console.log('New location: Latitude ' + location.latitude + ', Longitude ' + location.longitude);
    },
    function(error) {
        console.error('Error watching location: ' + error);
    }
);
```

### `clearWatch()`

Stops a specific location watch.

```javascript
// Assuming you have a watchID from a previous call to watchLocation
cordova.plugins.CordovaLocationEnhanced.clearWatch(watchID);