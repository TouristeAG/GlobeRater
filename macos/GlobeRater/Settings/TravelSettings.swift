import GlobeKit
import SwiftUI

struct TravelSettings: View {
    @Environment(AppModel.self) private var model

    private var hereIsDevice: Binding<Bool> {
        Binding(
            get: { model.settings.here == .device },
            set: { device in
                if device { model.update { $0.here = .device }; Task { await model.updateLocation() } }
                else if let p = model.store.lastDevicePlace { model.update { $0.here = .fixed(p) } }
                else { fixedPlaceHolder = true }
            })
    }

    @State private var fixedPlaceHolder = false

    private var herePlace: Binding<Place?> {
        Binding(
            get: { if case .fixed(let p) = model.settings.here { p } else { nil } },
            set: { p in model.update { $0.here = p.map { .fixed($0) } ?? .device } })
    }

    var body: some View {
        Form {
            Section {
                PlaceSearchField(title: L("place_home"), prompt: L("travel_home_hint"), place: model.binding(\.home))
            } footer: {
                SettingsFooter(key: "travel_subtitle")
            }
            Section {
                Picker(L("place_here"), selection: hereIsDevice) {
                    Text(verbatim: L("mac_mac_position")).tag(true)
                    Text(verbatim: L("travel_fixed_place")).tag(false)
                }
                .pickerStyle(.radioGroup)
                if model.settings.here == .device && !fixedPlaceHolder {
                    if model.locationDenied {
                        HStack {
                            Label(L("mac_location_denied"), systemImage: "location.slash").foregroundStyle(.orange)
                            Spacer()
                            Button(L("mac_open_privacy")) {
                                NSWorkspace.shared.open(URL(string: "x-apple.systempreferences:com.apple.preference.security?Privacy_LocationServices")!)
                            }
                        }
                    } else {
                        Text(verbatim: model.store.lastDevicePlace?.label ?? L("travel_location_hint"))
                            .foregroundStyle(.secondary)
                    }
                } else {
                    PlaceSearchField(title: L("travel_fixed_place"), prompt: L("travel_second_hint"), place: herePlace)
                }
            }
            Section {
                Picker(L("mac_units"), selection: model.binding(\.units)) {
                    Text(verbatim: "°C").tag(TemperatureUnit.celsius)
                    Text(verbatim: "°F").tag(TemperatureUnit.fahrenheit)
                }
                .pickerStyle(.segmented)
            }
        }
        .formStyle(.grouped)
        .onChange(of: model.settings.home) { Task { await model.refresh(force: false) } }
        .onChange(of: model.settings.here) { Task { await model.refresh(force: false) } }
        .onChange(of: model.settings.units) { Task { await model.refresh(force: false) } }
    }
}
