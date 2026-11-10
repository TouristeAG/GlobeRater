import CoreLocation
import GlobeKit

/// Approximate position of the Mac, read only when the app or the menu bar panel opens.
@MainActor
final class LocationProvider: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var pending: CheckedContinuation<CLLocation?, Never>?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyReduced
    }

    var isDenied: Bool {
        let s = manager.authorizationStatus
        return s == .denied || s == .restricted
    }

    /// Asks for permission if needed, reads one position (10 s at most) and names the city.
    func currentPlace() async -> Place? {
        guard !isDenied, pending == nil else { return nil }
        if manager.authorizationStatus == .notDetermined { manager.requestWhenInUseAuthorization() }
        let location: CLLocation? = await withCheckedContinuation { continuation in
            pending = continuation
            manager.requestLocation()
            Task { @MainActor [weak self] in
                try? await Task.sleep(for: .seconds(10))
                self?.finish(nil)
            }
        }
        guard let location else { return nil }
        let mark = try? await CLGeocoder().reverseGeocodeLocation(location).first
        let name = mark?.locality ?? mark?.name ?? TimeOffset.city(fromTimeZoneID: TimeZone.current.identifier)
        let label = [name, mark?.country].compactMap { $0 }.joined(separator: ", ")
        return Place(name: name, label: label, latitude: location.coordinate.latitude,
                     longitude: location.coordinate.longitude,
                     timeZoneID: mark?.timeZone?.identifier ?? TimeZone.current.identifier)
    }

    private func finish(_ location: CLLocation?) {
        pending?.resume(returning: location)
        pending = nil
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let last = locations.last
        MainActor.assumeIsolated { finish(last) }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: any Error) {
        MainActor.assumeIsolated { finish(nil) }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let denied = manager.authorizationStatus == .denied || manager.authorizationStatus == .restricted
        MainActor.assumeIsolated { if denied { finish(nil) } }
    }
}
