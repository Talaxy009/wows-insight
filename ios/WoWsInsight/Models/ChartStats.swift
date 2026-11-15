import Foundation

/// One day of the recently recorded battles of `account/day/list/info`.
///
/// The service keeps one record per day it was queried, so a day without a
/// record is missing from the series instead of being zero.
struct DailyStats: Identifiable, Sendable {
    /// The exact time of the record; unique even if two records share a day.
    let recordedAt: Date
    /// Local calendar day the record was taken on, used as the chart's x value.
    let day: Date
    let modes: [BattleMode: ModeStats]

    var id: Date { recordedAt }
}

/// The two numbers the trend chart can plot for one battle type.
enum ChartMetric: String, CaseIterable, Identifiable, Sendable {
    case rating
    case winRate

    var id: String { rawValue }

    /// The rating reads the same as the ship-list sort option, the win rate as
    /// the overview cell, so neither label is duplicated in the catalog.
    var label: String {
        switch self {
        case .rating: L10n.Sort.rating
        case .winRate: L10n.Player.statsWinRate
        }
    }

    func value(of stats: ModeStats) -> Double {
        switch self {
        case .rating: Double(max(stats.rating.value, 0))
        case .winRate: stats.stats.winRate
        }
    }

    func text(of stats: ModeStats) -> String {
        switch self {
        case .rating: stats.rating.displayText
        case .winRate: StatFormat.percent(stats.stats.winRate, digits: 1)
        }
    }
}

/// The categories the breakdown chart groups the player's battles by.
enum ChartBreakdown: String, CaseIterable, Identifiable, Sendable {
    case shipType
    case tier

    var id: String { rawValue }

    var label: String {
        switch self {
        case .shipType: L10n.Chart.shipType
        case .tier: L10n.Chart.tier
        }
    }
}

/// The key of one category of `shipTypeInfo` or `levelInfo`.
enum CategoryKey: Hashable, Sendable {
    /// The ship type code the API keys `shipTypeInfo` with, e.g. `Cruiser`.
    case shipType(String)
    case tier(Int)
}

/// Totals and rating of one ship type or tier, split per battle type.
struct CategoryStats: Identifiable, Sendable {
    let key: CategoryKey
    let modes: [BattleMode: ModeStats]

    var id: CategoryKey { key }

    var label: String {
        switch key {
        case .shipType(let type): ShipInfo.typeLabel(for: type)
        case .tier(let tier): ShipInfo.tierLabel(for: tier)
        }
    }

    /// Ship types in the game's own order, then tiers from I to ★.
    var sortOrder: Int {
        switch key {
        case .shipType(let type):
            ShipInfo.typeOrder.firstIndex(of: type) ?? ShipInfo.typeOrder.count
        case .tier(let tier):
            tier
        }
    }
}
