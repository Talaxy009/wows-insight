import Foundation

/// A battle type reported by `account/user/info2` under `battleTypeInfo`.
///
/// The raw value is the code the API keys that dictionary with; the same code
/// appears in `typeInfo` of every ship, so the picker on the player page filters
/// both the totals and the ship list. Codes the app has no name for are dropped
/// instead of being shown raw. Declaration order is the picker order.
enum BattleMode: String, CaseIterable, Comparable, Identifiable, Sendable {
    case pvp = "PVP"
    case pvpSolo = "PVP_SOLO"
    case pvpDiv2 = "PVP_DIV2"
    case pvpDiv3 = "PVP_DIV3"
    case ranked = "RANK"
    case rankedSolo = "RANK_SOLO"
    case rankedDiv2 = "RANK_DIV2"
    case rankedDiv3 = "RANK_DIV3"
    case rankedOld = "RANK_OLD"
    case rankedOldSolo = "RANK_OLD_SOLO"
    case rankedOldDiv2 = "RANK_OLD_DIV2"
    case rankedOldDiv3 = "RANK_OLD_DIV3"
    case clan = "CLAN"
    case brawl = "BRAWL"
    case operations = "OPER_MM"
    case coop = "PVE"
    case coopSolo = "PVE_SOLO"
    case coopDiv2 = "PVE_DIV2"
    case coopDiv3 = "PVE_DIV3"
    case rating = "RATING"
    case ratingSolo = "RATING_SOLO"
    case ratingDiv = "RATING_DIV"

    var id: String { rawValue }

    static func < (lhs: Self, rhs: Self) -> Bool {
        let modes = allCases
        return (modes.firstIndex(of: lhs) ?? 0) < (modes.firstIndex(of: rhs) ?? 0)
    }

    var label: String {
        switch self {
        case .pvp: L10n.BattleMode.pvp
        case .pvpSolo: L10n.BattleMode.pvpSolo
        case .pvpDiv2: L10n.BattleMode.pvpDiv2
        case .pvpDiv3: L10n.BattleMode.pvpDiv3
        case .ranked: L10n.BattleMode.ranked
        case .rankedSolo: L10n.BattleMode.rankedSolo
        case .rankedDiv2: L10n.BattleMode.rankedDiv2
        case .rankedDiv3: L10n.BattleMode.rankedDiv3
        case .rankedOld: L10n.BattleMode.rankedOld
        case .rankedOldSolo: L10n.BattleMode.rankedOldSolo
        case .rankedOldDiv2: L10n.BattleMode.rankedOldDiv2
        case .rankedOldDiv3: L10n.BattleMode.rankedOldDiv3
        case .clan: L10n.BattleMode.clan
        case .brawl: L10n.BattleMode.brawl
        case .operations: L10n.BattleMode.operations
        case .coop: L10n.BattleMode.coop
        case .coopSolo: L10n.BattleMode.coopSolo
        case .coopDiv2: L10n.BattleMode.coopDiv2
        case .coopDiv3: L10n.BattleMode.coopDiv3
        case .rating: L10n.BattleMode.rating
        case .ratingSolo: L10n.BattleMode.ratingSolo
        case .ratingDiv: L10n.BattleMode.ratingDiv
        }
    }
}
