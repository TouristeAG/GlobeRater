import Foundation

public enum RatesError: Error, Equatable, Sendable {
    case invalidKey, keyNotAllowed, quota, service(Int), empty, network
}

/// Open Exchange Rates, with the user's own key. The free plan only allows USD as base.
public struct ExchangeRateClient: Sendable {
    private let http: HTTPClient

    public init(http: HTTPClient = URLSession.shared) { self.http = http }

    private struct Response: Decodable { var rates: [String: Double]? }

    public func latest(apiKey: String, now: Date) async throws -> Rates {
        var components = URLComponents(string: "https://openexchangerates.org/api/latest.json")!
        components.queryItems = [URLQueryItem(name: "app_id", value: apiKey.trimmingCharacters(in: .whitespacesAndNewlines))]
        let data: Data, code: Int
        do { (data, code) = try await http.get(components.url!) } catch { throw RatesError.network }
        switch code {
        case 200: break
        case 401: throw RatesError.invalidKey
        case 403: throw RatesError.keyNotAllowed
        case 429: throw RatesError.quota
        default: throw RatesError.service(code)
        }
        guard let rates = try? JSONDecoder().decode(Response.self, from: data).rates, !rates.isEmpty else {
            throw RatesError.empty
        }
        var usd = rates
        usd["USD"] = 1
        return Rates(usd: usd, updatedAt: now)
    }
}
