import AppIntents
import SwiftUI

/// Per-widget accent colour, chosen with right-click → Edit widget. Same hues as System Settings → Appearance.
enum AccentChoice: String, AppEnum, CaseIterable {
    case system, blue, purple, pink, red, orange, yellow, green, graphite

    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "mac_accent")
    static let caseDisplayRepresentations: [AccentChoice: DisplayRepresentation] = [
        .system: DisplayRepresentation(title: "mac_accent_system"),
        .blue: DisplayRepresentation(title: "mac_accent_blue"),
        .purple: DisplayRepresentation(title: "mac_accent_purple"),
        .pink: DisplayRepresentation(title: "mac_accent_pink"),
        .red: DisplayRepresentation(title: "mac_accent_red"),
        .orange: DisplayRepresentation(title: "mac_accent_orange"),
        .yellow: DisplayRepresentation(title: "mac_accent_yellow"),
        .green: DisplayRepresentation(title: "mac_accent_green"),
        .graphite: DisplayRepresentation(title: "mac_accent_graphite"),
    ]

    var color: Color {
        switch self {
        case .system: .accentColor
        case .blue: .blue
        case .purple: .purple
        case .pink: .pink
        case .red: .red
        case .orange: .orange
        case .yellow: .yellow
        case .green: .green
        case .graphite: .gray
        }
    }
}

enum BudgetStyle: String, AppEnum, CaseIterable {
    case fill, ring

    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "mac_style")
    static let caseDisplayRepresentations: [BudgetStyle: DisplayRepresentation] = [
        .fill: DisplayRepresentation(title: "mac_style_fill"),
        .ring: DisplayRepresentation(title: "mac_style_ring"),
    ]
}
