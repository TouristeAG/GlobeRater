import GlobeKit
import SwiftUI
import WidgetKit

/// Budget widget: small (fill or ring, with "+") and medium (horizontal bar).
struct BudgetView: View {
    let snapshot: WidgetSnapshot
    let family: WidgetFamily
    let style: BudgetStyle
    let accent: AccentChoice
    let interactive: Bool

    private var b: BudgetSummary { snapshot.budget }
    private var tint: Color { b.isOver ? .red : accent.color }

    var body: some View {
        if !snapshot.settings.budget.enabled {
            Text(verbatim: L("budget_track")).font(.callout).foregroundStyle(.secondary).multilineTextAlignment(.center)
        } else if family == .systemMedium {
            medium
        } else if style == .ring {
            ring
        } else {
            fill
        }
    }

    private var remainingText: some View {
        Text(verbatim: Money.number(abs(b.remaining), currency: b.currency))
            .font(.system(size: 22, weight: .semibold, design: .rounded))
            .monospacedDigit().lineLimit(1).minimumScaleFactor(0.5)
    }

    private var caption: some View {
        Text(verbatim: b.remaining >= 0 ? b.currency : L("budget_over", b.currency))
            .font(.caption.weight(.medium)).foregroundStyle(.secondary).lineLimit(1)
    }

    private func plusButton(size: CGFloat = 36) -> some View {
        PadKey(intent: AddLastInputIntent(), symbol: "plus", prominent: snapshot.pad.lastInput > 0,
               accent: accent.color, interactive: interactive && snapshot.pad.lastInput > 0)
            .clipShape(Circle())
            .frame(width: size, height: size)
    }

    private var fill: some View {
        GeometryReader { geo in
            ZStack(alignment: .bottom) {
                RoundedRectangle(cornerRadius: 14, style: .continuous).fill(.quaternary)
                Rectangle().fill(tint.opacity(0.85))
                    .frame(height: geo.size.height * b.fraction)
                    .widgetAccentable()
                VStack(spacing: 2) {
                    remainingText
                    caption
                    Spacer(minLength: 0)
                    plusButton()
                }
                .padding(10)
            }
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
    }

    private var ring: some View {
        VStack(spacing: 8) {
            ZStack {
                Circle().stroke(.quaternary, lineWidth: 10)
                Circle().trim(from: 0, to: b.fraction)
                    .stroke(tint, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .widgetAccentable()
                VStack(spacing: 0) { remainingText; caption }.padding(14)
            }
            .padding(.horizontal, 6)
            plusButton(size: 28)
        }
    }

    private var medium: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text(verbatim: L("budget_spent_period", Formats.periodName(snapshot.settings.budget.displaySpan)))
                    .font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                Text(verbatim: Money.format(b.spent, currency: b.currency))
                    .font(.system(size: 28, weight: .semibold, design: .rounded))
                    .monospacedDigit().lineLimit(1).minimumScaleFactor(0.5)
                Spacer(minLength: 0)
                BudgetBar(fraction: b.fraction, isOver: b.isOver, accent: accent.color, height: 10)
                HStack {
                    Text(verbatim: Formats.leftOrOver(b))
                    Spacer(minLength: 4)
                    Text(verbatim: L("budget_resets", Formats.shortDate(b.nextReset))).foregroundStyle(.secondary)
                }
                .font(.caption).lineLimit(1)
            }
            plusButton()
        }
    }
}
