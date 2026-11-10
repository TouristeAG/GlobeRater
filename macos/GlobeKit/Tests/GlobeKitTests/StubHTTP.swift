import Foundation
@testable import GlobeKit

/// Answers requests by URL prefix and counts calls.
final class StubHTTP: HTTPClient, @unchecked Sendable {
    enum Reply { case body(String, Int), fixture(String, Int), failure }
    private let lock = NSLock()
    private var routes: [(String, Reply)] = []
    private(set) var calls: [URL] = []

    func on(_ prefix: String, _ reply: Reply) { lock.withLock { routes.append((prefix, reply)) } }

    func get(_ url: URL) async throws -> (Data, Int) {
        let reply: Reply? = lock.withLock {
            calls.append(url)
            return routes.first { url.absoluteString.hasPrefix($0.0) }?.1
        }
        switch reply {
        case .body(let text, let code): return (Data(text.utf8), code)
        case .fixture(let name, let code): return (try fixture(name), code)
        case .failure, nil: throw URLError(.notConnectedToInternet)
        }
    }
}

func fixture(_ name: String) throws -> Data {
    let url = Bundle.module.url(forResource: name, withExtension: "json", subdirectory: "Fixtures")!
    return try Data(contentsOf: url)
}
