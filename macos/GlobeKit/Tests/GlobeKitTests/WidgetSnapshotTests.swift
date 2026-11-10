import Foundation
import Testing
@testable import GlobeKit

private let geneva = Place(name: "Geneva", label: "Geneva, Switzerland", latitude: 46.2, longitude: 6.15, timeZoneID: "Europe/Zurich")
private let bangkok = Place(name: "Bangkok", label: "Bangkok, Thailand", latitude: 13.75, longitude: 100.5, timeZoneID: "Asia/Bangkok")

@Test func conversionsSkipTheInputCurrency() {
    let store = makeStore()
    store.pad = PadState(text: "100", currency: "USD", lastInput: 100)
    let snap = WidgetSnapshot.load(store, now: .now)
    #expect(snap.conversions(limit: 3).map(\.code) == ["EUR", "GBP", "JPY"])
    #expect(snap.conversions(limit: 9).count == 4)
    #expect(snap.conversions(limit: 1).first?.amount == 92)   // sample rates
    #expect(snap.amount == 100)
}

@Test func clockPlacesFollowSettings() {
    let store = makeStore()
    var snap = WidgetSnapshot.load(store, now: .now)
    #expect(snap.homeClock == nil)
    #expect(snap.hereClock.zone == TimeZone.current)
    store.settings.home = geneva
    store.settings.here = .fixed(bangkok)
    snap = WidgetSnapshot.load(store, now: .now)
    #expect(snap.homeClock == ClockPlace(name: "Geneva", zone: TimeZone(identifier: "Europe/Zurich")!))
    #expect(snap.hereClock == ClockPlace(name: "Bangkok", zone: TimeZone(identifier: "Asia/Bangkok")!))
}

@Test func deviceHereUsesLastKnownCity() {
    let store = makeStore()
    store.lastDevicePlace = bangkok
    #expect(WidgetSnapshot.load(store, now: .now).hereClock.name == "Bangkok")
}

@Test func sampleSnapshotIsComplete() {
    let s = WidgetSnapshot.sample
    #expect(s.homeClock != nil && s.home != nil && s.here != nil && s.pad.text == "100" && s.budget.spent > 0)
}
