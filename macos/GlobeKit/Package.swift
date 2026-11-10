// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "GlobeKit",
    defaultLocalization: "en",
    platforms: [.macOS(.v14)],
    products: [.library(name: "GlobeKit", targets: ["GlobeKit"])],
    targets: [
        .target(name: "GlobeKit"),
        .testTarget(name: "GlobeKitTests", dependencies: ["GlobeKit"], resources: [.copy("Fixtures")]),
    ]
)
