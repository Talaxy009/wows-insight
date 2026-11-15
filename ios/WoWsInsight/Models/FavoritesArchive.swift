import CoreTransferable
import Foundation
import UniformTypeIdentifiers

/// The JSON file used to move favorites between devices.
struct FavoritesArchive: Codable, Sendable {
    static let currentVersion = 1

    var version: Int
    var exportedAt: Date
    var players: [FavoritePlayer]

    init(players: [FavoritePlayer], exportedAt: Date = Date()) {
        version = Self.currentVersion
        self.exportedAt = exportedAt
        self.players = players
    }

    func encoded() throws -> Data {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes]
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(self)
    }

    static func decode(from data: Data) throws -> FavoritesArchive {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try decoder.decode(FavoritesArchive.self, from: data)
    }
}

/// Lets `ShareLink` hand the archive to Files, AirDrop, Mail and friends.
///
/// The JSON is encoded when the user shares, not while the menu is being laid
/// out.
struct FavoritesDocument: Transferable {
    let players: [FavoritePlayer]

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .json) { document in
            try FavoritesArchive(players: document.players).encoded()
        }
        .suggestedFileName("WoWs Insight Favorites.json")
    }
}
