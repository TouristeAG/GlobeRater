import GlobeKit
import SwiftUI

struct TodayView: View {
    @Environment(AppModel.self) private var model
    @State private var amount = ""
    @State private var showExpense = false
    @FocusState private var amountFocused: Bool
    let scrollTarget: String?

    var body: some View {
        ScrollViewReader { proxy in
            Form {
                amountSection
                convertedSection
                if model.settings.budget.enabled { budgetSection }
                TravelSection().id("travel")
            }
            .formStyle(.grouped)
            .onChange(of: scrollTarget) { _, target in
                if let target { withAnimation { proxy.scrollTo(target, anchor: .top) } }
            }
        }
        .onAppear { amount = KeypadInput.display(model.pad.text, grouped: false); amountFocused = true }
        .onChange(of: model.pad.text) { _, text in
            if KeypadInput.sanitize(amount) != text { amount = KeypadInput.display(text, grouped: false) }
        }
        .sheet(isPresented: $showExpense) { ExpenseSheet() }
    }

    private var amountSection: some View {
        Section {
            HStack(alignment: .firstTextBaseline) {
                TextField(text: $amount, prompt: Text(verbatim: "0")) { EmptyView() }
                    .labelsHidden()
                    .textFieldStyle(.plain)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .font(.system(size: 52, weight: .semibold, design: .rounded))
                    .monospacedDigit()
                    .focused($amountFocused)
                    .onChange(of: amount) { _, typed in model.setAmount(typed: typed) }
                Picker(selection: Binding(get: { model.pad.currency }, set: { model.selectCurrency($0) })) {
                    ForEach(model.settings.inputCurrencies, id: \.self) { Text(verbatim: $0).tag($0) }
                } label: { EmptyView() }
                .labelsHidden()
                .fixedSize()
                .help(L("change_currency"))
            }
            HStack {
                Text(verbatim: ratesStatus).foregroundStyle(.secondary)
                Spacer()
                if model.snapshot.rates.isSample {
                    SettingsLink { Text(verbatim: L("rates_add_live")) }
                } else if model.isRefreshing {
                    ProgressView().controlSize(.small)
                } else {
                    Button { Task { await model.refresh(force: true) } } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .buttonStyle(.borderless)
                    .help(L("rates_refresh_cd"))
                }
            }
            .font(.callout)
        }
    }

    private var ratesStatus: String {
        guard let at = model.snapshot.rates.updatedAt else { return L("rates_example") }
        let minutes = Int(Date.now.timeIntervalSince(at) / 60)
        switch minutes {
        case ..<1: return L("updated_just_now")
        case ..<60: return L("updated_minutes", minutes)
        case ..<(24 * 60): return L("updated_hours", minutes / 60)
        default: return L("updated_days", minutes / (24 * 60))
        }
    }

    private var convertedSection: some View {
        Section(L("section_converted")) {
            ForEach(model.conversions(), id: \.code) { c in
                HStack(spacing: 12) {
                    Text(verbatim: Money.symbol(for: c.code))
                        .font(.system(.body, design: .rounded).weight(.bold))
                        .foregroundStyle(.white)
                        .frame(width: 30, height: 30)
                        .background(RoundedRectangle(cornerRadius: 7, style: .continuous).fill(Color.accentColor.gradient))
                    Text(verbatim: c.code).font(.headline)
                    Spacer()
                    Text(verbatim: c.amount.map { Money.number($0, currency: c.code) } ?? "—")
                        .font(.title3.weight(.semibold)).monospacedDigit()
                        .contentTransition(.numericText())
                        .textSelection(.enabled)
                }
                .padding(.vertical, 2)
            }
        }
        .animation(.snappy, value: model.pad.text)
    }

    private var budgetSection: some View {
        let b = model.budget
        return Section {
            VStack(alignment: .leading, spacing: 10) {
                Text(verbatim: Money.format(b.spent, currency: b.currency))
                    .font(.system(size: 30, weight: .semibold, design: .rounded)).monospacedDigit()
                    .contentTransition(.numericText())
                ProgressView(value: b.fraction).tint(b.isOver ? .red : .accentColor)
                HStack {
                    Text(verbatim: Formats.leftOrOver(b))
                    Spacer()
                    Text(verbatim: L("budget_resets", Formats.shortDate(b.nextReset))).foregroundStyle(.secondary)
                }
                .font(.callout)
            }
            .padding(.vertical, 4)
        } header: {
            HStack {
                Text(verbatim: L("budget_spent_period", Formats.periodName(model.settings.budget.displaySpan)))
                Spacer()
                Button { showExpense = true } label: { Label(L("add_expense"), systemImage: "plus") }
                    .buttonStyle(.borderless)
            }
        }
        .animation(.snappy, value: b.spent)
    }
}
