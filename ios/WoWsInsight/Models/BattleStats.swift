import Foundation

/// PVP statistics for a player or a single ship.
///
/// Values are already normalised by `YuyukoStatsBlock`, which treats every
/// missing field as zero, so the computed properties below are always safe to
/// display.
struct BattleStats: Sendable {
    let battleCount: Int
    let winCount: Int
    let lossCount: Int
    let survivedCount: Int
    let survivedWinCount: Int
    let damage: Int
    let scoutingDamage: Int
    let fragCount: Int
    let experience: Int
    let planesDestroyed: Int
    let spottedCount: Int
    let potentialDamage: Int
    let hitRate: Double
    /// The best battle of every record type.
    let records: [BattleRecordKind: BattleRecordValue]
}

/// One best-battle record: its value and the ship it was achieved on.
struct BattleRecordValue: Sendable, Hashable {
    let value: Int
    let shipId: Int?
}

extension BattleStats {
    // MARK: - Derived values

    var drawCount: Int { max(0, battleCount - winCount - lossCount) }
    var winRate: Double { Self.ratio(winCount, battleCount) * 100 }
    var averageDamage: Double { Self.ratio(damage, battleCount) }
    var averageFrags: Double { Self.ratio(fragCount, battleCount) }
    var averageExperience: Double { Self.ratio(experience, battleCount) }
    var survivalRate: Double { Self.ratio(survivedCount, battleCount) * 100 }
    var survivedWinRate: Double { Self.ratio(survivedWinCount, survivedCount) * 100 }

    var killDeathRatio: Double {
        let deaths = battleCount - survivedCount
        guard deaths > 0 else { return 0 }
        return Double(fragCount) / Double(deaths)
    }

    private static func ratio(_ value: Int, _ total: Int) -> Double {
        guard total > 0 else { return 0 }
        return Double(value) / Double(total)
    }
}
