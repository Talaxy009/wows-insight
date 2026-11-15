import Foundation

/// A locally saved player. `server` is stored because the same account ID can
/// exist on different realms.
struct FavoritePlayer: Codable, Identifiable, Hashable, Sendable {
    let accountId: Int
    let nickname: String
    let server: GameServer

    var id: String { "\(server.rawValue)-\(accountId)" }

    var summary: PlayerSummary {
        PlayerSummary(accountId: accountId, nickname: nickname)
    }
}
