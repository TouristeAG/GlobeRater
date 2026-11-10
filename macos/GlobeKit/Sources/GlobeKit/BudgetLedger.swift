import Foundation

public struct Expense: Codable, Equatable, Identifiable, Sendable {
    public var id: UUID
    public var amount: Double
    public var currency: String
    public var date: Date

    public init(id: UUID = UUID(), amount: Double, currency: String, date: Date) {
        self.id = id; self.amount = amount; self.currency = currency; self.date = date
    }
}

/// Expenses of the current budget period, stored as JSON in the shared container.
/// Every read-modify-write holds an exclusive `flock`, so the app and the widget extension
/// (two processes) never overwrite each other's expenses.
public final class BudgetLedger: @unchecked Sendable {
    private struct File: Codable {
        var resetAt: Date?
        var expenses: [Expense] = []
    }

    public let fileURL: URL
    private var lockURL: URL { fileURL.deletingPathExtension().appendingPathExtension("lock") }

    public init(fileURL: URL) { self.fileURL = fileURL }

    public func add(_ amount: Double, currency: String, at date: Date, budget: BudgetSettings) throws {
        // Keep a whole year, so switching the display period (day ↔ month) never loses expenses.
        let keepSince = BudgetPeriod.currentStart(anchor: budget.anchor, refreshHour: budget.refreshHour,
                                                  span: .yearly, now: date)
        try update { file in
            file.expenses.removeAll { $0.date < keepSince }
            file.expenses.append(Expense(amount: amount, currency: currency, date: date))
        }
    }

    /// "Reset now": only expenses after this moment count until the next period.
    public func resetNow(at date: Date) throws {
        try update { $0.resetAt = date }
    }

    public func expenses() -> [Expense] { (try? withLock { read() })?.expenses ?? [] }

    /// Sum of the current period, in the budget currency. An expense whose rate is unknown
    /// is counted as typed rather than dropped (same as Android).
    public func spent(budget: BudgetSettings, rates: Rates, now: Date) -> Double {
        guard let file = try? withLock({ read() }) else { return 0 }
        let periodStart = BudgetPeriod.currentStart(anchor: budget.anchor, refreshHour: budget.refreshHour,
                                                    span: budget.displaySpan, now: now)
        let since = max(periodStart, file.resetAt ?? .distantPast)
        return file.expenses
            .filter { $0.date >= since }
            .reduce(0) { $0 + (rates.convert($1.amount, from: $1.currency, to: budget.currency) ?? $1.amount) }
    }

    // MARK: - File access

    private func update(_ change: (inout File) -> Void) throws {
        try withLock {
            var file = read()
            change(&file)
            let data = try JSONEncoder().encode(file)
            try data.write(to: fileURL, options: .atomic)
        }
    }

    /// Reads the file; an unreadable file is set aside and replaced by an empty one.
    private func read() -> File {
        guard let data = try? Data(contentsOf: fileURL) else { return File() }
        if let file = try? JSONDecoder().decode(File.self, from: data) { return file }
        let stamp = Int(Date().timeIntervalSince1970)
        let aside = fileURL.deletingLastPathComponent().appendingPathComponent("ledger.corrupt-\(stamp).json")
        try? FileManager.default.moveItem(at: fileURL, to: aside)
        return File()
    }

    private func withLock<T>(_ body: () throws -> T) throws -> T {
        let fd = open(lockURL.path, O_CREAT | O_RDWR, 0o644)
        guard fd >= 0 else { throw CocoaError(.fileWriteUnknown) }
        defer { close(fd) }
        guard flock(fd, LOCK_EX) == 0 else { throw CocoaError(.fileLocking) }
        defer { flock(fd, LOCK_UN) }
        return try body()
    }
}

public struct BudgetSummary: Equatable, Sendable {
    public var spent: Double
    public var limit: Double
    public var remaining: Double
    /// Share of the limit already spent, clamped to 0…1.
    public var fraction: Double
    public var isOver: Bool
    public var currency: String
    public var periodStart: Date
    public var nextReset: Date

    public init(spent: Double, limit: Double, currency: String, periodStart: Date, nextReset: Date) {
        self.spent = spent
        self.limit = limit
        self.remaining = limit - spent
        self.fraction = limit > 0 ? min(max(spent / limit, 0), 1) : (spent > 0 ? 1 : 0)
        self.isOver = spent > limit
        self.currency = currency
        self.periodStart = periodStart
        self.nextReset = nextReset
    }

    public static func make(ledger: BudgetLedger, budget: BudgetSettings, rates: Rates, now: Date) -> BudgetSummary {
        BudgetSummary(
            spent: ledger.spent(budget: budget, rates: rates, now: now),
            limit: budget.displayLimit,
            currency: budget.currency,
            periodStart: BudgetPeriod.currentStart(anchor: budget.anchor, refreshHour: budget.refreshHour, span: budget.displaySpan, now: now),
            nextReset: BudgetPeriod.nextStart(anchor: budget.anchor, refreshHour: budget.refreshHour, span: budget.displaySpan, now: now)
        )
    }
}
