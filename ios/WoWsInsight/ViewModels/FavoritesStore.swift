import Foundation
import Observation
import SwiftUI

/// Stores the user's favourite players in `UserDefaults`.
///
/// The list is tiny, so JSON in a single defaults key keeps the implementation
/// simple and avoids adding a persistence framework for one screen.
///
/// iCloud is deliberately not used: a free Apple developer team cannot sign the
/// required entitlement. Favorites move between devices with the JSON archive in
/// `FavoritesArchive` instead.
@MainActor
@Observable
final class FavoritesStore {
    /// How an import was resolved, so the UI can tell the user what happened.
    struct ImportSummary: Equatable {
        var added: Int
        var skipped: Int
    }

    private(set) var players: [FavoritePlayer] = []

    private let defaults: UserDefaults
    private let storageKey: String

    init(
        defaults: UserDefaults = .standard,
        storageKey: String = "favorites.players"
    ) {
        self.defaults = defaults
        self.storageKey = storageKey
        load()
    }

    func contains(_ player: PlayerSummary, server: GameServer) -> Bool {
        index(of: player.accountId, server: server) != nil
    }

    /// Keeps the cached nickname in sync when a player page is loaded again.
    func updateNickname(of player: PlayerSummary, server: GameServer) {
        guard let index = index(of: player.accountId, server: server),
              players[index].nickname != player.nickname
        else {
            return
        }
        players[index] = FavoritePlayer(
            accountId: player.accountId,
            nickname: player.nickname,
            server: server
        )
        save()
    }

    func toggle(_ player: PlayerSummary, server: GameServer) {
        if let index = index(of: player.accountId, server: server) {
            players.remove(at: index)
        } else {
            players.insert(
                FavoritePlayer(
                    accountId: player.accountId,
                    nickname: player.nickname,
                    server: server
                ),
                at: 0
            )
        }
        save()
    }

    func remove(_ favorite: FavoritePlayer) {
        guard let index = index(of: favorite.accountId, server: favorite.server) else {
            return
        }
        players.remove(at: index)
        save()
    }

    func move(fromOffsets source: IndexSet, toOffset destination: Int) {
        players.move(fromOffsets: source, toOffset: destination)
        save()
    }

    /// Appends the favorites of an exported file that are not saved yet and
    /// keeps the existing order, so importing never drops local favorites.
    func importArchive(_ archive: FavoritesArchive) -> ImportSummary {
        var summary = ImportSummary(added: 0, skipped: 0)
        for player in archive.players {
            if index(of: player.accountId, server: player.server) == nil {
                players.append(player)
                summary.added += 1
            } else {
                summary.skipped += 1
            }
        }
        if summary.added > 0 {
            save()
        }
        return summary
    }

    /// The current list as a shareable file.
    func exportDocument() -> FavoritesDocument {
        FavoritesDocument(players: players)
    }

    // MARK: - Private

    private func index(of accountID: Int, server: GameServer) -> Int? {
        players.firstIndex { $0.accountId == accountID && $0.server == server }
    }

    private func load() {
        guard let data = defaults.data(forKey: storageKey),
              let stored = try? JSONDecoder().decode([FavoritePlayer].self, from: data)
        else {
            return
        }
        players = stored
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(players) else { return }
        defaults.set(data, forKey: storageKey)
    }
}
