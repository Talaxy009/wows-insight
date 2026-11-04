import Foundation

/// Everything the detail screen needs, assembled once after loading.
///
/// Totals, ratings and records are kept per battle type; the screen shows one
/// type at a time and defaults to random battles (`PVP`).
struct PlayerReport: Sendable {
    let account: AccountInfo
    /// Battle types that have at least one battle, in picker order.
    let modes: [BattleMode]
    let modeStats: [BattleMode: ModeStats]
    let modeRecords: [BattleMode: [BattleRecord]]
    let ships: [ShipPerformance]

    init(overview: AccountOverview, shipStats: [ShipStat]) {
        account = overview.account

        // Every ship goes into the catalog, not just the ones with visible
        // stats, so a record can always name the ship it was achieved on.
        let catalog = Dictionary(
            shipStats.map { ($0.ship.shipId, $0.ship) },
            uniquingKeysWith: { first, _ in first }
        )

        let played = overview.modes.filter { $0.value.stats.battleCount > 0 }
        modes = played.keys.sorted()
        modeStats = played
        modeRecords = played.mapValues { BattleRecords.list(from: $0.stats, catalog: catalog) }

        ships = shipStats.map { ShipPerformance(ship: $0.ship, modes: $0.modes) }
    }

    /// Random battles when the player has them, otherwise the first mode that
    /// does have battles.
    var defaultMode: BattleMode? {
        modes.contains(.pvp) ? .pvp : modes.first
    }

    func stats(for mode: BattleMode) -> ModeStats? { modeStats[mode] }
    func records(for mode: BattleMode) -> [BattleRecord] { modeRecords[mode] ?? [] }
}

struct ShipPerformance: Identifiable, Sendable {
    let ship: ShipInfo
    let modes: [BattleMode: ModeStats]

    var id: Int { ship.shipId }

    func stats(for mode: BattleMode) -> ModeStats? { modes[mode] }
}

// MARK: - Sorting

/// Sorting options for the player's ship list.
enum ShipSort: String, CaseIterable, Identifiable, Sendable {
    case battles
    case winRate
    case averageDamage
    case rating
    case lastBattle

    var id: String { rawValue }

    /// The order the ship list opens with: most recent battle first.
    static let defaultSort: ShipSort = .lastBattle

    var label: String {
        switch self {
        case .battles: L10n.Sort.battles
        case .winRate: L10n.Sort.winRate
        case .averageDamage: L10n.Sort.averageDamage
        case .rating: L10n.Sort.rating
        case .lastBattle: L10n.Sort.lastBattle
        }
    }
}

extension [ShipPerformance] {
    /// Ships that actually fought in `mode`, sorted by the selected metric with
    /// the ship name as a stable tie-break.
    func ranked(in mode: BattleMode, by sort: ShipSort) -> [ShipPerformance] {
        func score(_ stats: ModeStats) -> Double {
            switch sort {
            case .battles: Double(stats.stats.battleCount)
            case .winRate: stats.stats.winRate
            case .averageDamage: stats.stats.averageDamage
            case .rating: Double(stats.rating.value)
            case .lastBattle: Double(stats.lastBattleTime ?? 0)
            }
        }

        let played: [(performance: ShipPerformance, stats: ModeStats)] = compactMap { ship in
            guard let stats = ship.modes[mode], stats.stats.battleCount > 0 else { return nil }
            return (ship, stats)
        }

        return played
            .sorted { lhs, rhs in
                let left = score(lhs.stats)
                let right = score(rhs.stats)
                if left == right {
                    let names = (lhs.performance.ship.name, rhs.performance.ship.name)
                    return names.0.localizedStandardCompare(names.1) == .orderedAscending
                }
                return left > right
            }
            .map(\.performance)
    }
}

// MARK: - Records

struct BattleRecord: Identifiable, Sendable {
    let id: String
    let title: String
    let value: Int
    let shipName: String?
}

/// A best-battle record reported by the API in `maxInfo`. The raw value is the
/// key it uses there.
enum BattleRecordKind: String, CaseIterable, Sendable {
    case maxDamage = "maxDamageDealt"
    case maxFrags = "maxFrags"
    case maxXp = "maxXp"
    case maxPlanes = "maxPlanesKilled"
    case maxSpotted = "maxShipsSpotted"
    case maxAgro = "maxTotalAgro"
    case maxScouting = "maxScoutingDamage"

    var title: String {
        switch self {
        case .maxDamage: L10n.Record.maxDamage
        case .maxFrags: L10n.Record.maxFrags
        case .maxXp: L10n.Record.maxXP
        case .maxPlanes: L10n.Record.maxPlanes
        case .maxSpotted: L10n.Record.maxSpotted
        case .maxAgro: L10n.Record.maxAgro
        case .maxScouting: L10n.Record.maxScouting
        }
    }
}

enum BattleRecords {
    /// The records that were actually achieved, in `BattleRecordKind` order.
    /// `catalog` names the ships; it is empty where the ship is already known.
    static func list(from stats: BattleStats, catalog: [Int: ShipInfo]) -> [BattleRecord] {
        BattleRecordKind.allCases.compactMap { kind in
            guard let record = stats.records[kind], record.value > 0 else { return nil }
            return BattleRecord(
                id: kind.rawValue,
                title: kind.title,
                value: record.value,
                shipName: record.shipId.flatMap { catalog[$0]?.name }
            )
        }
    }
}
