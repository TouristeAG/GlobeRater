import SwiftUI

@main
struct GlobeRaterApp: App {
    @State private var model = AppModel()

    var body: some Scene {
        WindowGroup(id: "main") {
            MainView().environment(model)
        }
        .defaultSize(width: 860, height: 760)
        .handlesExternalEvents(matching: ["*"])
    }
}
