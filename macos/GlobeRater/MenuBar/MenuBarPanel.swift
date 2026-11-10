import AppKit
import GlobeKit
import SwiftUI

/// Menu bar panel: type an amount with the keyboard, see the conversions, add it to the budget.
struct MenuBarPanel: View {
    @Environment(AppModel.self) private var model
    @Environment(\.openWindow) private var openWindow
    @State private var amount = ""
    @State private var copied: String?
    @FocusState private var focused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            TextField(text: $amount, prompt: Text(verbatim: "0")) { EmptyView() }
                .labelsHidden()
                .textFieldStyle(.plain)
                .font(.system(size: 30, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .focused($focused)
                .onChange(of: amount) { _, typed in model.setAmount(typed: typed) }

            Picker(selection: Binding(get: { model.pad.currency }, set: { model.selectCurrency($0) })) {
                ForEach(model.settings.inputCurrencies, id: \.self) { Text(verbatim: $0).tag($0) }
            } label: { EmptyView() }
            .labelsHidden()
            .pickerStyle(.segmented)

            VStack(spacing: 2) {
                ForEach(model.conversions(), id: \.code) { row($0) }
            }

            if model.settings.budget.enabled {
                VStack(alignment: .leading, spacing: 6) {
                    BudgetBar(fraction: model.budget.fraction, isOver: model.budget.isOver, accent: .accentColor)
                    HStack {
                        Text(verbatim: Formats.leftOrOver(model.budget)).font(.caption).foregroundStyle(.secondary)
                        Spacer()
                        Button(L("mac_add_to_budget")) { model.addPadToBudget() }
                            .keyboardShortcut(.return, modifiers: .command)
                            .disabled(KeypadInput.parse(model.pad.text) <= 0)
                    }
                }
            }

            Divider()
            HStack {
                Button(L("mac_open_app")) {
                    openWindow(id: "main")
                    NSApp.activate(ignoringOtherApps: true)
                }
                SettingsLink { Text(verbatim: L("tab_settings")) }
                Spacer()
                Button(L("mac_quit")) { NSApp.terminate(nil) }
            }
            .buttonStyle(.borderless)
        }
        .padding(14)
        .frame(width: 280)
        .onAppear {
            amount = KeypadInput.display(model.pad.text, grouped: false)
            focused = true
            Task { await model.updateLocation() }
        }
        .onChange(of: model.pad.text) { _, text in
            if KeypadInput.sanitize(amount) != text { amount = KeypadInput.display(text, grouped: false) }
        }
    }

    private func row(_ c: Conversion) -> some View {
        let value = c.amount.map { Money.number($0, currency: c.code) } ?? "—"
        return Button {
            NSPasteboard.general.clearContents()
            NSPasteboard.general.setString(value, forType: .string)
            copied = c.code
            Task {
                try? await Task.sleep(for: .seconds(1))
                if copied == c.code { copied = nil }
            }
        } label: {
            HStack {
                Text(verbatim: c.code).foregroundStyle(.secondary)
                Spacer()
                if copied == c.code {
                    Label(L("mac_copied"), systemImage: "checkmark").labelStyle(.iconOnly).foregroundStyle(.green)
                }
                Text(verbatim: value).monospacedDigit().contentTransition(.numericText())
            }
            .font(.title3.weight(.medium))
            .padding(.vertical, 3)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .help(L("mac_copied"))
    }
}
