var exec = require('cordova/exec');

var CordovaLocationEnhaced = {
    /**
     * Checks if the device's location service is enabled.
     * @param {Function} successCallback - The callback to execute on success. It will receive a boolean.
     * @param {Function} errorCallback - The callback to execute on failure.
     */
    isLocationEnabled: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'CordovaLocationEnhaced', 'isLocationEnabled', []);
    },

    /**
     * Checks the application's location permission status and returns it in a human-readable format.
     * @param {Function} successCallback - The callback to execute on success. It will receive an object with a 'status' string.
     * @param {Function} errorCallback - The callback to execute on failure.
     */
    getPermissionStatus: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'CordovaLocationEnhaced', 'getPermissionStatus', []);
    },

    /**
     * Checks the user-granted location accuracy level (precise or coarse).
     * @param {Function} successCallback - The callback to execute on success. It will receive an object with an 'accuracyLevel' string.
     * @param {Function} errorCallback - The callback to execute on failure.
     */
    getAccuracyLevel: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'CordovaLocationEnhaced', 'getAccuracyLevel', []);
    }
};

module.exports = CordovaLocationEnhaced;
