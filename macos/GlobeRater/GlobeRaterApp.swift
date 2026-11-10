import SwiftUI

@main
struct GlobeRaterApp: App {
    var body: some Scene {
        WindowGroup(id: "main") {
            WidgetGalleryView().frame(minWidth: 820, minHeight: 600)
        }
    }
}
