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
    },

    /**
     * Requests location permission from the user.
     * @param {string} accuracyLevel - The desired accuracy level ('precise' or 'coarse').
     * @param {Function} success - The callback function for a successful permission request.
     * @param {Function} error - The callback function for a failed or denied permission request.
     */
    requestPermission: function (accuracyLevel, success, error) {
        exec(success, error, "CordovaLocationEnhanced", "requestPermission", [accuracyLevel]);
    },

    /**
     * Retrieves the user's current location once.
     * @param {object} options - An object containing location request options.
     * @param {string} options.accuracyLevel - The desired accuracy ('precise' or 'coarse'). Defaults to 'precise'.
     * @param {number} options.timeout - The maximum time in milliseconds to wait for a location.
     * @param {number} options.maximumAge - The maximum age in milliseconds of a cached location to be accepted.
     * @param {Function} success - The callback function for a successful location retrieval.
     * @param {Function} error - The callback function for a failed location retrieval.
     */
    getCurrentPosition: function (options, success, error) {
        exec(success, error, "CordovaLocationEnhanced", "getCurrentPosition", [options]);
    }
};

/**
 * @enum {string}
 * An enumeration of possible location permission authorization statuses.
 */
const PermissionStatus = {
    NOT_DETERMINED: "Not Determined",
    RESTRICTED: "Restricted",
    DENIED: "Denied",
    AUTHORIZED_WHEN_IN_USE: "Authorized When In Use",
    AUTHORIZED_ALWAYS: "Authorized Always",
};

/**
 * @enum {string}
 * An enumeration of possible location accuracy levels.
 */
const AccuracyLevel = {
    PRECISE: "Precise",
    COARSE: "Coarse",
};

/**
 * @enum {string}
 * An enumeration of possible location permission statuses, with accuracy details.
 */
const DetailedPermissionStatus = {
    NOT_DETERMINED: "Not Determined",
    RESTRICTED: "Restricted",
    DENIED: "Denied",
    GRANTED_PRECISE: "Granted (Precise)",
    GRANTED_COARSE: "Granted (Coarse)",
    GRANTED_WHEN_IN_USE_PRECISE: "Granted (When In Use, Precise)",
    GRANTED_WHEN_IN_USE_COARSE: "Granted (When In Use, Coarse)",
    GRANTED_ALWAYS_PRECISE: "Granted (Always, Precise)",
    GRANTED_ALWAYS_COARSE: "Granted (Always, Coarse)"
};

exports.PermissionStatus = PermissionStatus;
exports.AccuracyLevel = AccuracyLevel;
exports.DetailedPermissionStatus = DetailedPermissionStatus;

module.exports = CordovaLocationEnhanced;
