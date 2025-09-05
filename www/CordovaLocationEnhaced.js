var exec = require('cordova/exec');

var CordovaLocationEnhanced = {
    /**
     * Checks if the device's location service is enabled.
     * @param {Function} successCallback - The callback to execute on success. It will receive a boolean.
     * @param {Function} errorCallback - The callback to execute on failure.
     */
    isLocationEnabled: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'CordovaLocationEnhanced', 'isLocationEnabled', []);
    },

    /**
     * Checks the application's location permission status and returns it in a human-readable format.
     * @param {Function} successCallback - The callback to execute on success. It will receive an object with a 'status' string.
     * @param {Function} errorCallback - The callback to execute on failure.
     */
    getPermissionStatus: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'CordovaLocationEnhanced', 'getPermissionStatus', []);
    },

    /**
     * Checks the user-granted location accuracy level (precise or coarse).
     * @param {Function} successCallback - The callback to execute on success. It will receive an object with an 'accuracyLevel' string.
     * @param {Function} errorCallback - The callback to execute on failure.
     */
    getAccuracyLevel: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'CordovaLocationEnhanced', 'getAccuracyLevel', []);
    },

    /**
     * Watches the user's location and notifies the app when it changes.
     * @param {Function} successCallback - The callback to execute on location change. It will receive a location object.
     * @param {Function} errorCallback - The callback to execute on failure.
     * @returns {string} - A watch ID that can be used to clear the watch.
     */
    watchLocation: function(successCallback, errorCallback) {
        var watchId = Math.random().toString(36).substring(7);
        exec(successCallback, errorCallback, 'CordovaLocationEnhanced', 'watchLocation', [watchId]);
        return watchId;
    },

    /**
     * Stops watching the user's location.
     * @param {string} watchId - The ID of the watch to clear.
     */
    clearWatch: function(watchId) {
        exec(null, null, 'CordovaLocationEnhanced', 'clearWatch', [watchId]);
    }
};

module.exports = CordovaLocationEnhanced;
