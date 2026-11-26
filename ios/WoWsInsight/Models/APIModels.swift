import Foundation

/// A player returned by the account search endpoint.
struct PlayerSummary: Identifiable, Hashable, Sendable {
    let accountId: Int
    let nickname: String

    var id: Int { accountId }
}

/// The account details shown on the player header card.
struct AccountInfo: Sendable {
    let accountId: Int
    let nickname: String
    let createdAt: Int?
    let lastBattleTime: Int?
    /// The in-game dog tag shown next to the nickname.
    let dogTagURL: URL?
    /// "[TAG] Name" of the current clan, or `nil` when the account is not in one.
    let clanName: String?
}

/// One ship of the encyclopedia as rendered by the app.
struct ShipInfo: Identifiable, Hashable, Sendable {
    let shipId: Int
    let name: String
    let tier: Int
    let type: String
    let nation: ShipNation
    let imageURLString: String?

    var id: Int { shipId }

    var imageURL: URL? {
        guard let imageURLString, !imageURLString.isEmpty else { return nil }
        return URL(string: imageURLString)
    }

    var tierLabel: String { Self.tierLabel(for: tier) }

    /// "I" … "X", then "★" for superships.
    static func tierLabel(for tier: Int) -> String {
        guard tier >= 1, tier <= tierLabels.count else { return StatFormat.placeholder }
        return tierLabels[tier - 1]
    }

    private static let tierLabels = [
        "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "★",
    ]

    var typeLabel: String { Self.typeLabel(for: type) }

    static func typeLabel(for type: String) -> String {
        switch type {
        case "Battleship": L10n.ShipType.battleship
        case "Cruiser": L10n.ShipType.cruiser
        case "Destroyer": L10n.ShipType.destroyer
        case "AirCarrier": L10n.ShipType.airCarrier
        case "Submarine": L10n.ShipType.submarine
        case "Auxiliary": L10n.ShipType.auxiliary
        default: type.isEmpty ? L10n.ShipType.unknown : type
        }
    }

    /// The order the game lists the ship types in, used by the chart legend.
    static let typeOrder = [
        "Destroyer", "Cruiser", "Battleship", "AirCarrier", "Submarine", "Auxiliary",
    ]
}

/// Totals, rating and last battle of a player or of one ship in one battle
/// type.
struct ModeStats: Sendable {
    let stats: BattleStats
    let rating: PlayerRating
    let lastBattleTime: Int?
}

/// The account overview returned by `account/user/info2`, split per battle type.
struct AccountOverview: Sendable {
    let account: AccountInfo
    let modes: [BattleMode: ModeStats]
    /// `shipTypeInfo`, grouped by ship type.
    let shipTypes: [CategoryStats]
    /// `levelInfo`, grouped by ship tier.
    let tiers: [CategoryStats]
}

/// The Yuyuko API returns the ship metadata, the totals of every battle type and
/// the per-mode rating in the same object, so no encyclopedia download is needed.
struct ShipStat: Identifiable, Sendable {
    /// The API always returns the ship metadata together with its stats.
    let ship: ShipInfo
    let modes: [BattleMode: ModeStats]

    var id: Int { ship.shipId }
}
