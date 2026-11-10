import GlobeKit
import SwiftUI
import WidgetKit

/// Converter widget: medium (keypad on the right) and large (Android "Tall" layout), with or without budget.
struct ConverterView: View {
    let snapshot: WidgetSnapshot
    let family: WidgetFamily
    let withBudget: Bool
    let accent: AccentChoice
    let interactive: Bool

    private var tint: Color { accent.color }
    private var pad: PadState { snapshot.pad }

    var body: some View {
        if family == .systemLarge { large } else { medium }
    }

    // MARK: Medium

    private var medium: some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 5) {
                HStack(spacing: 4) {
                    chips
                    Spacer(minLength: 0)
                    key("C").frame(width: 26, height: 20)
                }
                amountText(size: 26)
                ForEach(snapshot.conversions(limit: withBudget ? 2 : 3), id: \.code) { conversionRow($0) }
                if withBudget { budgetRow }
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            keyGrid([["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"], [",", "0", withBudget ? "+" : "<"]])
                .frame(width: 138)
        }
    }

    // MARK: Large

    private var large: some View {
        VStack(spacing: 8) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(verbatim: pad.currency).font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                    amountText(size: 34)
                }
                Spacer(minLength: 4)
                chips
            }
            if withBudget { budgetRow }
            keyGrid([["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"], ["0", ",", "000"]])
            HStack(spacing: 6) {
                VStack(alignment: .leading, spacing: 3) {
                    ForEach(snapshot.conversions(limit: 5), id: \.code) { conversionRow($0) }
                    Spacer(minLength: 0)
                }
                .padding(8)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .background(RoundedRectangle(cornerRadius: 8, style: .continuous).strokeBorder(.quaternary, lineWidth: 1.5))
                VStack(spacing: 6) {
                    HStack(spacing: 6) { key("C"); key("<") }
                    if withBudget { key("+") }
                }
                .frame(width: 112)
            }
            .frame(height: 96)
        }
    }

    // MARK: Parts

    private var chips: some View {
        HStack(spacing: 4) {
            ForEach(snapshot.settings.inputCurrencies, id: \.self) { code in
                CurrencyChip(code: code, selected: code == pad.currency, accent: tint, interactive: interactive)
            }
        }
    }

    private func amountText(size: CGFloat) -> some View {
        Text(verbatim: KeypadInput.display(pad.text))
            .font(.system(size: size, weight: .semibold, design: .rounded))
            .monospacedDigit()
            .lineLimit(1)
            .minimumScaleFactor(0.4)
            .contentTransition(.numericText())
    }

    private func conversionRow(_ c: Conversion) -> some View {
        HStack(spacing: 4) {
            Text(verbatim: c.code).foregroundStyle(.secondary)
            Spacer(minLength: 2)
            Text(verbatim: c.amount.map { Money.number($0, currency: c.code) } ?? "—")
                .lineLimit(1).minimumScaleFactor(0.6)
        }
        .font(.callout.weight(.medium))
        .monospacedDigit()
    }

    private var budgetRow: some View {
        VStack(alignment: .leading, spacing: 3) {
            BudgetBar(fraction: snapshot.budget.fraction, isOver: snapshot.budget.isOver, accent: tint)
            Text(verbatim: Formats.leftOrOver(snapshot.budget))
                .font(.caption2).foregroundStyle(.secondary).lineLimit(1)
        }
    }

    private func keyGrid(_ rows: [[String]]) -> some View {
        Grid(horizontalSpacing: 5, verticalSpacing: 5) {
            ForEach(rows, id: \.self) { row in
                GridRow { ForEach(row, id: \.self) { key($0) } }
            }
        }
    }

    @ViewBuilder
    private func key(_ token: String) -> some View {
        switch token {
        case "+":
            PadKey(intent: AddPadToBudgetIntent(), symbol: "plus", prominent: true, accent: tint, interactive: interactive)
        case "<":
            PadKey(intent: KeyIntent("<"), symbol: "delete.left", accent: tint, interactive: interactive)
        case ",":
            PadKey(intent: KeyIntent(","), label: Formats.decimalSeparator(), accent: tint, interactive: interactive)
        default:
            PadKey(intent: KeyIntent(token), label: token, accent: tint, interactive: interactive)
        }
    }
}
