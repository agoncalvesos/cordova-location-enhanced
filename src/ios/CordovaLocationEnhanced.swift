import Foundation
import CoreLocation

@objc(CordovaLocationEnhaced)
class CordovaLocationEnhaced: CDVPlugin, CLLocationManagerDelegate {

    private var locationManager: CLLocationManager?
    private var watchCallback: CDVInvokedUrlCommand?

    override func pluginInitialize() {
        super.pluginInitialize()
        locationManager = CLLocationManager()
        locationManager?.delegate = self
        locationManager?.desiredAccuracy = kCLLocationAccuracyBest
        locationManager?.distanceFilter = 1.0 // Notify every 1 meter change
    }

    @objc(isLocationEnabled)
    func isLocationEnabled(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run {
            let isEnabled = CLLocationManager.locationServicesEnabled()
            
            let pluginResult: CDVPluginResult
            if isEnabled {
                pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: true)
            } else {
                pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: false)
            }
            
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        }
    }

    @objc(getPermissionStatus)
    func getPermissionStatus(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run {
            let status = CLLocationManager.authorizationStatus()
            var statusString = "Unknown"

            switch status {
            case .notDetermined:
                statusString = "Not Determined"
            case .restricted:
                statusString = "Restricted"
            case .denied:
                statusString = "Denied"
            case .authorizedAlways:
                statusString = "Always"
            case .authorizedWhenInUse:
                statusString = "When in Use"
            @unknown default:
                statusString = "Unknown"
            }
            
            let result: [String: String] = ["status": statusString]
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        }
    }

    @objc(getAccuracyLevel)
    func getAccuracyLevel(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run {
            guard let locationManager = self.locationManager else {
                self.sendError(command.callbackId, message: "Location manager not initialized.")
                return
            }

            let accuracy = locationManager.accuracyAuthorization
            var accuracyString = "Unknown"

            if #available(iOS 14.0, *) {
                switch accuracy {
                case .fullAccuracy:
                    accuracyString = "Full Accuracy"
                case .reducedAccuracy:
                    accuracyString = "Reduced Accuracy"
                @unknown default:
                    accuracyString = "Unknown"
                }
            } else {
                // Prior to iOS 14, only full accuracy was available.
                accuracyString = "Full Accuracy"
            }

            let result: [String: String] = ["accuracyLevel": accuracyString]
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        }
    }

    @objc(watchLocation)
    func watchLocation(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run {
            self.watchCallback = command
            if CLLocationManager.locationServicesEnabled() {
                self.locationManager?.startUpdatingLocation()
            } else {
                self.sendError(command.callbackId, message: "Location services are not enabled.")
            }
        }
    }

    @objc(clearWatch)
    func clearWatch(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run {
            self.locationManager?.stopUpdatingLocation()
            self.watchCallback = nil
            self.sendSuccess(command.callbackId, message: "Location watch cleared.")
        }
    }

    // CLLocationManagerDelegate methods
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last, let command = self.watchCallback else { return }

        let resultData: [String: Any] = [
            "latitude": location.coordinate.latitude,
            "longitude": location.coordinate.longitude,
            "accuracy": location.horizontalAccuracy
        ]

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: resultData)
        pluginResult?.setKeepCallbackAs(true) // Keep the callback active for future updates
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        guard let command = self.watchCallback else { return }
        self.sendError(command.callbackId, message: "Location watch failed: \(error.localizedDescription)")
    }

    private func sendSuccess(_ callbackId: String, message: Any? = nil) {
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: message)
        self.commandDelegate.send(pluginResult, callbackId: callbackId)
    }

    private func sendError(_ callbackId: String, message: String) {
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: message)
        self.commandDelegate.send(pluginResult, callbackId: callbackId)
    }
}