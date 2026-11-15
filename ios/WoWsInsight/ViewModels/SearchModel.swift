import Foundation
import Observation

@MainActor
@Observable
final class SearchModel {
    /// The API, the search prompt and this model all require this many
    /// characters before a lookup makes sense.
    static let minimumQueryLength = 3

    private static let debounceDelay: Duration = .milliseconds(350)

    var query = ""
    var server: GameServer = .asia
    var results: [PlayerSummary] = []
    var isSearching = false
    var errorMessage: String?

    private let client: YuyukoClient

    init(client: YuyukoClient = YuyukoClient()) {
        self.client = client
    }

    /// Identifies the effective search. Whitespace-only edits do not change it,
    /// so trimming the query does not trigger a redundant request.
    var requestKey: String { "\(server.rawValue)#\(trimmedQuery)" }

    var trimmedQuery: String { query.trimmingCharacters(in: .whitespacesAndNewlines) }

    var hasValidQuery: Bool { trimmedQuery.count >= Self.minimumQueryLength }

    /// Runs when the query or the server changes. SwiftUI's `.task(id:)`
    /// cancels the previous call, which makes this a debounced search.
    func search() async {
        let key = requestKey
        let text = trimmedQuery

        errorMessage = nil
        guard text.count >= Self.minimumQueryLength else {
            results = []
            isSearching = false
            return
        }

        // Set before the debounce so the screen shows a spinner instead of
        // flashing "no results" while the request is still pending.
        isSearching = true
        defer {
            if key == requestKey {
                isSearching = false
            }
        }

        do {
            try await Task.sleep(for: Self.debounceDelay)
        } catch {
            return
        }

        do {
            let players = try await client.searchPlayers(query: text, server: server)
            guard !Task.isCancelled, key == requestKey else { return }
            results = players
        } catch {
            guard !error.isCancellation, key == requestKey else { return }
            results = []
            errorMessage = error.localizedDescription
        }
    }

    func clear() {
        results = []
        errorMessage = nil
    }
}
