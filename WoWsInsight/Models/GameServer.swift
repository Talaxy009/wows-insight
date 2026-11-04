import Foundation

/// The World of Warships realms exposed by the Yuyuko API.
///
/// The raw value doubles as the API code and the value persisted in favorites.
enum GameServer: String, CaseIterable, Codable, Identifiable, Hashable, Sendable {
    case ru
    case eu
    case na
    case asia
    case cn

    var id: String { rawValue }

    /// The China realm only supports exact nickname lookups, so the search
    /// screen asks the API for a single match instead of a fuzzy list.
    var usesExactSearch: Bool { self == .cn }

    var displayName: String {
        L10n.Server.name(for: self)
    }
}
