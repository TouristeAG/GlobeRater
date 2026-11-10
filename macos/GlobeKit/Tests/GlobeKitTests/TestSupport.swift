import Foundation
@testable import GlobeKit

func temporaryDirectory() -> URL {
    let url = FileManager.default.temporaryDirectory.appendingPathComponent("globekit-\(UUID().uuidString)")
    try! FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
    return url
}

func makeStore(directory: URL = temporaryDirectory(), suite: String = UUID().uuidString) -> SharedStore {
    SharedStore(defaults: UserDefaults(suiteName: suite)!, directory: directory, notify: false)
}

func monthlyBudget(currency: String = "USD", amount: Double = 100) -> BudgetSettings {
    var b = BudgetSettings(anchor: Date(timeIntervalSince1970: 0))
    b.currency = currency
    b.amount = amount
    return b
}
