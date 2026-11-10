import Testing
@testable import GlobeKit

@Test func groupIsUsedOnlyWhenEntitled() {
    #expect(SharedStore.appGroup(declared: "ABCDE12345.ch.tageoa.globerater", entitled: ["ABCDE12345.ch.tageoa.globerater"]) == "ABCDE12345.ch.tageoa.globerater")
    #expect(SharedStore.appGroup(declared: "ch.tageoa.globerater", entitled: ["ABCDE12345.ch.tageoa.globerater"]) == nil)
    #expect(SharedStore.appGroup(declared: "ABCDE12345.ch.tageoa.globerater", entitled: nil) == nil)
    #expect(SharedStore.appGroup(declared: nil, entitled: ["x"]) == nil)
    #expect(SharedStore.appGroup(declared: "$(APP_GROUP)", entitled: ["$(APP_GROUP)"]) == nil)
    // Ad-hoc build: no Team ID, so no real group.
    #expect(SharedStore.appGroup(declared: ".ch.tageoa.globerater", entitled: [".ch.tageoa.globerater"]) == nil)
    #expect(SharedStore.appGroup(declared: "", entitled: [""]) == nil)
}
