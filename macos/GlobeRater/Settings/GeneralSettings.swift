import GlobeKit
import ServiceManagement
import SwiftUI

struct GeneralSettings: View {
    @Environment(AppModel.self) private var model
    @State private var openAtLogin = SMAppService.mainApp.status == .enabled

    private var version: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
    }

    var body: some View {
        Form {
            Section {
                Toggle(L("mac_open_at_login"), isOn: $openAtLogin)
                    .onChange(of: openAtLogin) { _, on in
                        do {
                            if on { try SMAppService.mainApp.register() } else { try SMAppService.mainApp.unregister() }
                        } catch {
                            openAtLogin = SMAppService.mainApp.status == .enabled
                        }
                    }
                Toggle(L("mac_show_menu_bar"), isOn: model.binding(\.showInMenuBar))
            }
            Section(L("about_title")) {
                LabeledContent { Text(verbatim: version) } label: { Text(verbatim: "GlobeRater") }
                LabeledContent(L("data_sources")) {
                    Text(verbatim: L("data_sources_text")).multilineTextAlignment(.trailing)
                }
                SettingsFooter(key: "mac_privacy")
                Link(destination: URL(string: "https://opensource.org/license/mit")!) { Text(verbatim: "MIT License · © 2026 Tageoa") }
            }
        }
        .formStyle(.grouped)
    }
}
