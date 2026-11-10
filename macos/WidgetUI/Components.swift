import AppIntents
import SwiftUI
import WidgetKit

/// A keypad key. In the app's gallery (`interactive == false`) it is drawn but does nothing.
struct PadKey<I: AppIntent>: View {
    let intent: I
    var label: String = ""
    var symbol: String? = nil
    var prominent = false
    let accent: Color
    let interactive: Bool

    var body: some View {
        let face = RoundedRectangle(cornerRadius: 7, style: .continuous)
            .fill(prominent ? AnyShapeStyle(accent) : AnyShapeStyle(.quaternary))
            .widgetAccentable(prominent)
            .overlay {
                Group {
                    if let symbol { Image(systemName: symbol) } else { Text(verbatim: label) }
                }
                .font(.system(.body, design: .rounded).weight(.semibold))
                .monospacedDigit()
                .foregroundStyle(prominent ? AnyShapeStyle(.white) : AnyShapeStyle(.primary))
            }
        if interactive {
            Button(intent: intent) { face }.buttonStyle(.plain)
        } else {
            face
        }
    }
}

/// Input currency chip: tap to type in this currency.
struct CurrencyChip: View {
    let code: String
    let selected: Bool
    let accent: Color
    let interactive: Bool

    var body: some View {
        let face = Text(verbatim: code)
            .font(.caption2.weight(.bold))
            .padding(.horizontal, 7)
            .padding(.vertical, 4)
            .foregroundStyle(selected ? AnyShapeStyle(.white) : AnyShapeStyle(.secondary))
            .background(Capsule().fill(selected ? AnyShapeStyle(accent) : AnyShapeStyle(.quaternary)).widgetAccentable(selected))
        if interactive {
            Button(intent: SelectCurrencyIntent(code)) { face }.buttonStyle(.plain)
        } else {
            face
        }
    }
}

/// Thin budget bar, red when over budget.
struct BudgetBar: View {
    let fraction: Double
    let isOver: Bool
    let accent: Color
    var height: CGFloat = 6

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(.quaternary)
                Capsule().fill(isOver ? Color.red : accent)
                    .frame(width: max(geo.size.width * fraction, fraction > 0 ? height : 0))
                    .widgetAccentable()
            }
        }
        .frame(height: height)
    }
}
