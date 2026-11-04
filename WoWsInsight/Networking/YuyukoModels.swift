import Foundation

// MARK: - Envelope

/// Every Yuyuko endpoint answers with the same envelope.
struct YuyukoEnvelope<Payload: Decodable>: Decodable {
    let code: Int
    let message: String?
    let data: Payload?
}

// MARK: - Account search

struct YuyukoSearchUser: Decodable, Sendable {
    let accountId: Int
    let userName: String
}

// MARK: - Account overview

struct YuyukoUserInfoData: Decodable, Sendable {
    let userInfo: YuyukoUserDetail?
    let battleTypeInfo: [String: YuyukoModeSummary]?
    let lastBattleTime: Int?
}

struct YuyukoUserDetail: Decodable, Sendable {
    let accountId: Int?
    let userName: String?
    let accountCreateTime: Int?
    /// Absolute URL of the in-game dog tag image.
    let dogTag: String?
}

struct YuyukoModeSummary: Decodable, Sendable {
    let prInfo: YuyukoRating?
    /// `account/user/info2` nests the totals under `shipInfo`.
    let shipInfo: YuyukoStatsBlock?
    /// `account/ship/info/list` nests the totals under `battleInfo`.
    let battleInfo: YuyukoStatsBlock?

    var stats: YuyukoStatsBlock? { battleInfo ?? shipInfo }
}

// MARK: - Per-ship statistics

struct YuyukoShipListData: Decodable, Sendable {
    let userInfo: YuyukoUserDetail?
    let shipInfo: [YuyukoShipEntry]?
}

struct YuyukoShipEntry: Decodable, Sendable {
    let shipInfo: YuyukoShipMeta?
    let typeInfo: [String: YuyukoModeSummary]?
}

struct YuyukoShipMeta: Decodable, Sendable {
    let shipId: Int
    let nameCn: String?
    let nameCn360: String?
    let nameEnglish: String?
    let nameRu: String?
    let level: Int?
    let shipType: String?
    let country: String?
    let imgSmall: String?
}

// MARK: - Shared numbers

struct YuyukoRating: Decodable, Sendable {
    /// `0` means "not rated yet"; higher values are rating tiers.
    let code: Int?
    let value: Int?
    let name: String?
}

struct YuyukoStatsBlock: Decodable, Sendable {
    let battleInfo: YuyukoTotals?
    let fragsInfo: YuyukoFrags?
    let maxInfo: YuyukoMax?
    let hitRatioInfo: YuyukoHitRatio?
    let lastBattleTime: Int?
}

struct YuyukoTotals: Decodable, Sendable {
    let battle: Int?
    let wins: Int?
    let losses: Int?
    let survived: Int?
    let winAndSurvived: Int?
    let xp: Int?
    let damageDealt: Int?
    let scoutingDamage: Int?
    let shipsSpotted: Int?
    let planesKilled: Int?
    let artAgro: Int?
}

struct YuyukoFrags: Decodable, Sendable {
    let frags: Int?
}

struct YuyukoHitRatio: Decodable, Sendable {
    let ratioMain: Double?
}

struct YuyukoMax: Decodable, Sendable {
    let maxFrags: YuyukoMaxValue?
    let maxDamageDealt: YuyukoMaxValue?
    let maxScoutingDamage: YuyukoMaxValue?
    let maxPlanesKilled: YuyukoMaxValue?
    let maxShipsSpotted: YuyukoMaxValue?
    let maxTotalAgro: YuyukoMaxValue?
    let maxXp: YuyukoMaxValue?
}

struct YuyukoMaxValue: Decodable, Sendable {
    let shipId: Int?
    let value: Int?
}

// MARK: - Mapping

extension YuyukoRating {
    var playerRating: PlayerRating {
        guard let code, code != 0, let value, value > 0 else { return .unknown }
        return PlayerRating(value: value)
    }
}

extension Dictionary where Key == String, Value == YuyukoModeSummary {
    /// The battle types the app can name and that carry a stats block.
    var modeStats: [BattleMode: ModeStats] {
        reduce(into: [BattleMode: ModeStats]()) { modes, entry in
            guard let mode = BattleMode(rawValue: entry.key), let block = entry.value.stats else {
                return
            }
            modes[mode] = ModeStats(
                stats: BattleStats(block: block),
                rating: entry.value.prInfo?.playerRating ?? .unknown,
                lastBattleTime: block.lastBattleTime
            )
        }
    }
}

extension BattleStats {
    /// Fills every missing number with zero so the screens never have to check,
    /// and collects the best battles into one dictionary.
    init(block: YuyukoStatsBlock?) {
        let totals = block?.battleInfo
        let best = block?.maxInfo

        var records: [BattleRecordKind: BattleRecordValue] = [:]
        func add(_ kind: BattleRecordKind, _ entry: YuyukoMaxValue?) {
            guard let entry, let value = entry.value else { return }
            records[kind] = BattleRecordValue(value: value, shipId: entry.shipId)
        }
        add(.maxDamage, best?.maxDamageDealt)
        add(.maxFrags, best?.maxFrags)
        add(.maxXp, best?.maxXp)
        add(.maxPlanes, best?.maxPlanesKilled)
        add(.maxSpotted, best?.maxShipsSpotted)
        add(.maxAgro, best?.maxTotalAgro)
        add(.maxScouting, best?.maxScoutingDamage)

        self.init(
            battleCount: totals?.battle ?? 0,
            winCount: totals?.wins ?? 0,
            lossCount: totals?.losses ?? 0,
            survivedCount: totals?.survived ?? 0,
            survivedWinCount: totals?.winAndSurvived ?? 0,
            damage: totals?.damageDealt ?? 0,
            scoutingDamage: totals?.scoutingDamage ?? 0,
            fragCount: block?.fragsInfo?.frags ?? 0,
            experience: totals?.xp ?? 0,
            planesDestroyed: totals?.planesKilled ?? 0,
            spottedCount: totals?.shipsSpotted ?? 0,
            potentialDamage: totals?.artAgro ?? 0,
            hitRate: block?.hitRatioInfo?.ratioMain ?? 0,
            records: records
        )
    }
}

extension YuyukoShipMeta {
    /// Picks the localized name. Chinese players on the China realm see the
    /// 360 translation, everyone else the WG one.
    func name(language: ShipNameLanguage, server: GameServer) -> String {
        func first(_ candidates: [String?]) -> String? {
            candidates.compactMap { $0 }.first { !$0.isEmpty }
        }

        let chinese = server == .cn
            ? first([nameCn360, nameCn, nameEnglish])
            : first([nameCn, nameCn360, nameEnglish])
        let english = first([nameEnglish, nameCn, nameCn360])

        let resolved = language == .chinese ? chinese : english
        return resolved ?? L10n.Ship.unknownShip(String(shipId))
    }

    func shipInfo(language: ShipNameLanguage, server: GameServer) -> ShipInfo {
        ShipInfo(
            shipId: shipId,
            name: name(language: language, server: server),
            tier: level ?? 0,
            type: shipType ?? "",
            nation: ShipNation(apiName: country),
            imageURLString: imgSmall
        )
    }
}
