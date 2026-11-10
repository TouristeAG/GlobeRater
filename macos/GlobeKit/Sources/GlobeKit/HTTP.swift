import Foundation

/// Minimal GET client, so tests can answer requests without the network.
public protocol HTTPClient: Sendable {
    func get(_ url: URL) async throws -> (Data, Int)
}

extension URLSession: HTTPClient {
    public func get(_ url: URL) async throws -> (Data, Int) {
        let (data, response) = try await data(from: url)
        return (data, (response as? HTTPURLResponse)?.statusCode ?? 0)
    }
}
