import GlobeKit
import SwiftUI

struct ExpenseSheet: View {
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    @State private var amount = ""
    @State private var currency = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(verbatim: L("add_expense")).font(.headline)
            HStack {
                TextField(text: $amount, prompt: Text(verbatim: "0")) { EmptyView() }
                    .font(.title2.monospacedDigit())
                    .frame(minWidth: 160)
                Picker(selection: $currency) {
                    ForEach(model.settings.inputCurrencies, id: \.self) { Text(verbatim: $0).tag($0) }
                } label: { EmptyView() }
                .labelsHidden()
                .fixedSize()
            }
            HStack {
                Spacer()
                Button(L("action_cancel"), role: .cancel) { dismiss() }
                    .keyboardShortcut(.cancelAction)
                Button(L("action_add")) {
                    if model.addExpense(typed: amount, currency: currency) { dismiss() }
                }
                .keyboardShortcut(.defaultAction)
                .disabled(KeypadInput.parse(KeypadInput.sanitize(amount)) <= 0)
            }
        }
        .padding(20)
        .frame(width: 340)
        .onAppear { currency = model.pad.currency }
    }
}
