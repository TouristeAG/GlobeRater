import SwiftUI

enum MainSection: String, Hashable { case today, widgets }

struct MainView: View {
    @State private var section: MainSection? = .today
    @State private var scrollTarget: String?

    var body: some View {
        NavigationSplitView {
            List(selection: $section) {
                Label(L("tab_today"), systemImage: "sun.max").tag(MainSection.today)
                Label(L("tab_widgets"), systemImage: "square.grid.2x2").tag(MainSection.widgets)
            }
            .navigationSplitViewColumnWidth(min: 160, ideal: 180)
        } detail: {
            if let tab = UserDefaults.standard.string(forKey: "GRPreviewTab") {  // TEMP: screenshots
                Group {
                    switch tab {
                    case "rates": RatesSettings()
                    case "budget": BudgetSettingsView()
                    case "travel": TravelSettings()
                    case "menubar": MenuBarPanel()
                    case "gallery": WidgetGalleryView()
                    default: GeneralSettings()
                    }
                }.frame(minWidth: 520)
            } else {
            switch section {
            case .widgets: WidgetGalleryView().navigationTitle(L("tab_widgets"))
            default: TodayView(scrollTarget: scrollTarget).navigationTitle(L("tab_today"))
            }
            }
        }
        .frame(minWidth: 720, minHeight: 560)
        .onOpenURL { url in
            section = .today
            scrollTarget = url.host() == "travel" ? "travel" : nil
        }
    }
}
