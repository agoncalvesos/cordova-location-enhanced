import Foundation
import CoreLocation

@objc(CordovaLocationEnhanced)
class CordovaLocationEnhanced: CDVPlugin {

    private var locationManager: CLLocationManager!
    private var permissionCommand: CDVInvokedUrlCommand?
    private var watchCallbacks: [String: CDVInvokedUrlCommand] = [:]
    private var locationUpdatesCallbackId: String?
    
    private var currentPositionCommand: CDVInvokedUrlCommand?
    private var currentPositionTimer: Timer?
    
    override func pluginInitialize() {
        super.pluginInitialize()
        locationManager = CLLocationManager()
        locationManager.delegate = self
    }
    
    private func sendError(command: CDVInvokedUrlCommand, errorCode: String, errorMessage: String) {
        let errorDict: [String: String] = ["errorCode": errorCode, "errorMessage": errorMessage]
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: errorDict)
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(isLocationEnabled:)
    func isLocationEnabled(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run {
            let isEnabled = CLLocationManager.locationServicesEnabled()
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: isEnabled)
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        }
    }

    @objc(getPermissionStatus:)
    func getPermissionStatus(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run {
            var statusString: String
            
            switch self.locationManager.authorizationStatus {
            case .notDetermined:
                statusString = "Not Determined"
            case .restricted:
                statusString = "Restricted"
            case .denied:
                statusString = "Denied"
            case .authorizedAlways:
                statusString = "Authorized Always"
            case .authorizedWhenInUse:
                statusString = "Authorized When In Use"
            @unknown default:
                statusString = "Unknown"
            }
            
            let result: [String: String] = ["status": statusString]
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        }
    }
    
    @objc(getAccuracyLevel:)
    func getAccuracyLevel(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run {
            var accuracyLevel: String
            
            if #available(iOS 14.0, *) {
                switch self.locationManager.accuracyAuthorization {
                case .fullAccuracy:
                    accuracyLevel = "Precise"
                case .reducedAccuracy:
                    accuracyLevel = "Coarse"
                @unknown default:
                    accuracyLevel = "Unknown"
                }
            } else {
                accuracyLevel = "Precise"
            }
            
            let result: [String: String] = ["accuracyLevel": accuracyLevel]
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        }
    }

    @objc(watchLocation:)
    func watchLocation(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run {
            let watchId = command.arguments[0] as? String ?? UUID().uuidString
            self.watchCallbacks[watchId] = command
            
            // Setting the desired accuracy here is a best practice.            
            self.locationManager.desiredAccuracy = kCLLocationAccuracyBest
            self.locationManager.startUpdatingLocation()

            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: watchId)
            pluginResult?.setKeepCallbackAs(true)
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        }
    }

    @objc(clearWatch:)
    func clearWatch(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run {
            guard let watchId = command.arguments[0] as? String else {
                sendError(command: command, errorCode: "INVALID_WATCH_ID", errorMessage: "Invalid watch ID.")
                return
            }
            
            self.watchCallbacks.removeValue(forKey: watchId)
            
            if self.watchCallbacks.isEmpty && self.currentPositionCommand == nil {
                self.locationManager.stopUpdatingLocation()
            }
            
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        }
    }

    @objc(requestPermission:)
    func requestPermission(_ command: CDVInvokedUrlCommand) {
        self.permissionCommand = command
        guard let accuracyLevel = command.arguments[0] as? String else {
            sendError(command: command, errorCode: "INVALID_ACCURACY_LEVEL", errorMessage: "Accuracy level not specified.")
            return
        }

        switch self.locationManager.authorizationStatus {
        case .notDetermined:
            if accuracyLevel == "always" {
                self.locationManager.requestAlwaysAuthorization()
            } else {
                self.locationManager.requestWhenInUseAuthorization()
            }
        case .authorizedWhenInUse:
            if accuracyLevel == "always" {
                self.locationManager.requestAlwaysAuthorization()
            } else {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "Permissions already granted for When In Use.")
                self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                self.permissionCommand = nil
            }
        case .authorizedAlways:
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "Permissions already granted for Always.")
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
            self.permissionCommand = nil
        case .denied, .restricted:
            sendError(command: command, errorCode: "PERMISSION_DENIED", errorMessage: "Permission denied or restricted.")
            self.permissionCommand = nil
        @unknown default:
            sendError(command: command, errorCode: "UNKNOWN_STATUS", errorMessage: "Unknown authorization status.")
            self.permissionCommand = nil
        }
    }
    
    @objc(getCurrentPosition:)
    func getCurrentPosition(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate.run {
            guard self.locationManager.authorizationStatus != .notDetermined else {
                sendError(command: command, errorCode: "PERMISSION_DENIED", errorMessage: "Location permission not granted.")
                return
            }
            guard let options = command.arguments[0] as? [String: Any] else {
                sendError(command: command, errorCode: "INVALID_OPTIONS", errorMessage: "Invalid options provided.")
                return
            }

            let timeout = options["timeout"] as? TimeInterval ?? 0
            let maxAge = options["maximumAge"] as? TimeInterval ?? 0

            if let lastLocation = self.locationManager.location,
               (Date().timeIntervalSince(lastLocation.timestamp) * 1000) < maxAge {
                self.sendLocationResult(location: lastLocation, callbackId: command.callbackId)
            } else {
                self.currentPositionCommand = command
                self.locationManager.desiredAccuracy = kCLLocationAccuracyBest
                self.locationManager.startUpdatingLocation()

                if timeout > 0 {
                    self.currentPositionTimer = Timer.scheduledTimer(withTimeInterval: timeout / 1000, repeats: false) { _ in
                        self.stopCurrentPositionUpdates()
                        self.sendError(command: command, errorCode: "TIMEOUT", errorMessage: "Timeout getting location.")
                    }
                }
            }
        }
    }

    private func sendLocationResult(location: CLLocation, callbackId: String) {
        let result: [String: Any] = [
            "latitude": location.coordinate.latitude,
            "longitude": location.coordinate.longitude,
            "accuracy": location.horizontalAccuracy,
            "altitude": location.altitude,
            "timestamp": location.timestamp.timeIntervalSince1970 * 1000
        ]
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        self.commandDelegate.send(pluginResult, callbackId: callbackId)
    }

    private func stopCurrentPositionUpdates() {
        if self.currentPositionCommand != nil {
            self.locationManager.stopUpdatingLocation()
            self.currentPositionCommand = nil
        }
        self.currentPositionTimer?.invalidate()
        self.currentPositionTimer = nil
    }
}

// MARK: - CLLocationManagerDelegate
extension CordovaLocationEnhanced: CLLocationManagerDelegate {
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        guard let command = self.permissionCommand else { return }

        let result: [String: String]
        
        switch manager.authorizationStatus {
        case .notDetermined, .restricted, .denied:
            result = ["status": "Denied"]
        case .authorizedWhenInUse:
            if #available(iOS 14.0, *) {
                let accuracy = manager.accuracyAuthorization == .fullAccuracy ? "Precise" : "Coarse"
                result = ["status": "Authorized When In Use", "accuracy": accuracy]
            } else {
                result = ["status": "Authorized When In Use", "accuracy": "Precise"]
            }
        case .authorizedAlways:
            if #available(iOS 14.0, *) {
                let accuracy = manager.accuracyAuthorization == .fullAccuracy ? "Precise" : "Coarse"
                result = ["status": "Authorized Always", "accuracy": accuracy]
            } else {
                result = ["status": "Authorized Always", "accuracy": "Precise"]
            }
        @unknown default:
            result = ["status": "Unknown"]
        }

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        self.permissionCommand = nil
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard !locations.isEmpty else { return }
        
        // Handle watchLocation callbacks
        if !self.watchCallbacks.isEmpty {
            for (_, command) in self.watchCallbacks {
                let location = locations.last!
                let result: [String: Any] = [
                    "latitude": location.coordinate.latitude,
                    "longitude": location.coordinate.longitude,
                    "accuracy": location.horizontalAccuracy,
                    "altitude": location.altitude,
                    "timestamp": location.timestamp.timeIntervalSince1970 * 1000
                ]
                
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
                pluginResult?.setKeepCallbackAs(true)
                self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
            }
        }
        
        // Handle getCurrentPosition callback
        if let command = self.currentPositionCommand, let location = locations.last {
            self.stopCurrentPositionUpdates()
            self.sendLocationResult(location: location, callbackId: command.callbackId)
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // Handle watchLocation callbacks
        if !self.watchCallbacks.isEmpty {
            for (_, command) in self.watchCallbacks {
                sendError(command: command, errorCode: "LOCATION_ERROR", errorMessage: error.localizedDescription)
            }
        }
        
        // Handle getCurrentPosition callback
        if let command = self.currentPositionCommand {
            self.stopCurrentPositionUpdates()
            sendError(command: command, errorCode: "LOCATION_ERROR", errorMessage: error.localizedDescription)
        }
    }
}
