import Foundation

/// Every user-facing string in the app.
///
/// Design notes:
/// - Keys are stable semantic identifiers (for example `player.stats.winRate`),
///   not English sentences. Renaming an English label does not break a
///   translation.
/// - `Localizable.xcstrings` is the only place the text lives, English
///   included; this file declares nothing but the keys, so a label never has to
///   be edited twice. The home-screen app name lives in `InfoPlist.xcstrings`.
/// - A key that is missing from the catalog is rendered as the key itself,
///   which makes the omission obvious instead of silently falling back.
/// - `String(localized:)` automatically follows the system language. There is
///   deliberately no in-app language picker. Unsupported languages fall back to
///   English, the development language.
/// - To add a language, translate both string catalogs. If the Wargaming API
///   also supports that language for ship names, extend
///   `AppConfig.shipNameLanguage`.
enum L10n {
    fileprivate static func text(_ key: String.LocalizationValue) -> String {
        String(localized: key)
    }

    fileprivate static func format(
        _ key: String.LocalizationValue,
        _ arguments: CVarArg...
    ) -> String {
        String(format: String(localized: key), arguments: arguments)
    }

    // MARK: - General

    enum App {
        /// Navigation title and generic app name.
        static var title: String { L10n.text("app.title") }
    }

    enum Common {
        static var done: String { L10n.text("common.done") }
        static var edit: String { L10n.text("common.edit") }
        static var retry: String { L10n.text("common.retry") }
    }

    enum Server {
        static func name(for server: GameServer) -> String {
            switch server {
            case .ru: L10n.text("server.ru")
            case .eu: L10n.text("server.eu")
            case .na: L10n.text("server.na")
            case .asia: L10n.text("server.asia")
            case .cn: L10n.text("server.cn")
            }
        }
    }

    // MARK: - Search

    enum Search {
        static var prompt: String { L10n.text("search.prompt") }
        static var emptyTitle: String { L10n.text("search.empty.title") }
        static var emptyMessage: String { L10n.text("search.empty.message") }
        static var errorTitle: String { L10n.text("search.error.title") }
        static var searching: String { L10n.text("search.searching") }
        static var favoritesHeader: String { L10n.text("search.favorites.header") }
        static var favoriteAdd: String { L10n.text("search.favorite.add") }
        static var favoriteRemove: String { L10n.text("search.favorite.remove") }

        // MARK: - Import / export

        static var transferMenu: String { L10n.text("search.transfer.menu") }
        static var importFavorites: String { L10n.text("search.import") }
        static var exportFavorites: String { L10n.text("search.export") }
        static var exportPreview: String { L10n.text("search.export.preview") }
        static var importDoneTitle: String { L10n.text("search.import.done.title") }
        static var importFailedTitle: String { L10n.text("search.import.failed.title") }
        static var importFailedMessage: String { L10n.text("search.import.failed.message") }

        static func importSummary(added: Int, skipped: Int) -> String {
            L10n.format("search.import.summary", added, skipped)
        }
    }

    // MARK: - Player

    enum Player {
        static var loading: String { L10n.text("player.loading") }
        static var errorTitle: String { L10n.text("player.error.title") }
        static var favoriteAdd: String { L10n.text("player.favorite.add") }
        static var favoriteRemove: String { L10n.text("player.favorite.remove") }
        static var ratingInfo: String { L10n.text("player.ratingInfo") }

        static func accountID(_ id: String) -> String {
            L10n.format("player.accountId", id)
        }

        static func accountID(server: String, id: String) -> String {
            L10n.format("player.accountIdWithServer", server, id)
        }

        static var registered: String { L10n.text("player.registered") }
        static var lastBattle: String { L10n.text("player.lastBattle") }

        static var battleType: String { L10n.text("player.battleType") }

        static var statsOverview: String { L10n.text("player.stats.overview") }
        static var statsBattles: String { L10n.text("player.stats.battles") }
        static var statsWinRate: String { L10n.text("player.stats.winRate") }
        static var statsAverageDamage: String { L10n.text("player.stats.avgDamage") }
        static var statsAverageFrags: String { L10n.text("player.stats.avgFrags") }
        static var statsAverageXP: String { L10n.text("player.stats.avgXP") }
        static var statsSurvivalRate: String { L10n.text("player.stats.survivalRate") }

        static var detailsTitle: String { L10n.text("player.details.title") }
        static var winDrawLoss: String { L10n.text("player.details.winDrawLoss") }
        static var totalDamage: String { L10n.text("player.details.totalDamage") }
        static var totalXP: String { L10n.text("player.details.totalXP") }
        static var totalFrags: String { L10n.text("player.details.totalFrags") }
        static var hitRate: String { L10n.text("player.details.hitRate") }
        static var killDeath: String { L10n.text("player.details.kd") }
        static var potentialDamage: String { L10n.text("player.details.potentialDamage") }
        static var spottingDamage: String { L10n.text("player.details.spottingDamage") }
        static var shipsSpotted: String { L10n.text("player.details.shipsSpotted") }
        static var planesDestroyed: String { L10n.text("player.details.planesDestroyed") }
        static var survivedBattles: String { L10n.text("player.details.survivedBattles") }
        static var survivedWinRate: String { L10n.text("player.details.survivedWinRate") }

        static var recordsTitle: String { L10n.text("player.records.title") }
        static var hiddenTitle: String { L10n.text("player.hidden.title") }
        static var hiddenMessage: String { L10n.text("player.hidden.message") }

        static func shipsTitle(_ count: String) -> String {
            L10n.format("player.ships.title", count)
        }
        static var shipsEmpty: String { L10n.text("player.ships.empty") }
        static var sort: String { L10n.text("player.ships.sort") }
        static func shipAverageDamage(_ value: String) -> String {
            L10n.format("player.ships.avgDamage", value)
        }
        static func shipSubtitle(type: String, battles: String, winRate: String) -> String {
            L10n.format("player.ships.subtitle", type, battles, winRate)
        }
    }

    // MARK: - Battle types

    /// Names of the battle types the API reports in `battleTypeInfo`.
    enum BattleMode {
        static var pvp: String { L10n.text("battleMode.pvp") }
        static var pvpSolo: String { L10n.text("battleMode.pvp.solo") }
        static var pvpDiv2: String { L10n.text("battleMode.pvp.div2") }
        static var pvpDiv3: String { L10n.text("battleMode.pvp.div3") }

        static var ranked: String { L10n.text("battleMode.ranked") }
        static var rankedSolo: String { L10n.text("battleMode.ranked.solo") }
        static var rankedDiv2: String { L10n.text("battleMode.ranked.div2") }
        static var rankedDiv3: String { L10n.text("battleMode.ranked.div3") }

        static var rankedOld: String { L10n.text("battleMode.rankedOld") }
        static var rankedOldSolo: String { L10n.text("battleMode.rankedOld.solo") }
        static var rankedOldDiv2: String { L10n.text("battleMode.rankedOld.div2") }
        static var rankedOldDiv3: String { L10n.text("battleMode.rankedOld.div3") }

        static var clan: String { L10n.text("battleMode.clan") }
        static var brawl: String { L10n.text("battleMode.brawl") }
        static var operations: String { L10n.text("battleMode.operations") }

        static var coop: String { L10n.text("battleMode.coop") }
        static var coopSolo: String { L10n.text("battleMode.coop.solo") }
        static var coopDiv2: String { L10n.text("battleMode.coop.div2") }
        static var coopDiv3: String { L10n.text("battleMode.coop.div3") }

        static var rating: String { L10n.text("battleMode.rating") }
        static var ratingSolo: String { L10n.text("battleMode.rating.solo") }
        static var ratingDiv: String { L10n.text("battleMode.rating.div") }
    }

    // MARK: - Ship sorting

    enum Sort {
        static var battles: String { L10n.text("sort.battles") }
        static var winRate: String { L10n.text("sort.winRate") }
        static var averageDamage: String { L10n.text("sort.avgDamage") }
        static var rating: String { L10n.text("sort.rating") }
        static var lastBattle: String { L10n.text("sort.lastBattle") }
    }

    // MARK: - Ship metadata

    enum ShipType {
        static var battleship: String { L10n.text("ship.type.battleship") }
        static var cruiser: String { L10n.text("ship.type.cruiser") }
        static var destroyer: String { L10n.text("ship.type.destroyer") }
        static var airCarrier: String { L10n.text("ship.type.aircarrier") }
        static var submarine: String { L10n.text("ship.type.submarine") }
        static var auxiliary: String { L10n.text("ship.type.auxiliary") }
        static var unknown: String { L10n.text("ship.type.unknown") }
    }

    enum Nation {
        static func name(for nation: ShipNation) -> String {
            switch nation {
            case .usa: L10n.text("ship.nation.usa")
            case .japan: L10n.text("ship.nation.japan")
            case .germany: L10n.text("ship.nation.germany")
            case .ussr: L10n.text("ship.nation.ussr")
            case .uk: L10n.text("ship.nation.uk")
            case .france: L10n.text("ship.nation.france")
            case .italy: L10n.text("ship.nation.italy")
            case .panAsia: L10n.text("ship.nation.pan_asia")
            case .europe: L10n.text("ship.nation.europe")
            case .panAmerica: L10n.text("ship.nation.pan_america")
            case .netherlands: L10n.text("ship.nation.netherlands")
            case .spain: L10n.text("ship.nation.spain")
            case .commonwealth: L10n.text("ship.nation.commonwealth")
            case .latinAmerica: L10n.text("ship.nation.latin_america")
            case .events: L10n.text("ship.nation.events")
            case .unknown: L10n.text("ship.nation.unknown")
            }
        }
    }

    enum Ship {
        static func unknownShip(_ id: String) -> String {
            L10n.format("ship.unknown", id)
        }
        static func meta(tier: String, type: String, nation: String) -> String {
            L10n.format("ship.detail.meta", tier, type, nation)
        }
        static func lastBattle(_ date: String) -> String {
            L10n.format("ship.detail.lastBattle", date)
        }
    }

    // MARK: - Personal rating

    enum Rating {
        static func label(for tier: RatingTier) -> String {
            switch tier {
            case .unknown: L10n.text("rating.tier.unknown")
            case .bad: L10n.text("rating.tier.bad")
            case .belowAverage: L10n.text("rating.tier.belowAverage")
            case .average: L10n.text("rating.tier.average")
            case .good: L10n.text("rating.tier.good")
            case .veryGood: L10n.text("rating.tier.veryGood")
            case .great: L10n.text("rating.tier.great")
            case .unicum: L10n.text("rating.tier.unicum")
            case .superUnicum: L10n.text("rating.tier.superUnicum")
            }
        }

        enum Scale {
            static var title: String { L10n.text("ratingScale.title") }
            static var footer: String { L10n.text("ratingScale.footer") }
        }
    }

    // MARK: - Records

    enum Record {
        static var maxDamage: String { L10n.text("record.maxDamage") }
        static var maxFrags: String { L10n.text("record.maxFrags") }
        static var maxXP: String { L10n.text("record.maxXp") }
        static var maxPlanes: String { L10n.text("record.maxPlanes") }
        static var maxSpotted: String { L10n.text("record.maxSpotted") }
        static var maxAgro: String { L10n.text("record.maxAgro") }
        static var maxScouting: String { L10n.text("record.maxScouting") }
    }

    // MARK: - Errors

    enum Error {
        static var invalidURL: String { L10n.text("error.invalidURL") }
        static func httpStatus(_ code: String) -> String {
            L10n.format("error.httpStatus", code)
        }
        static func api(_ code: String) -> String {
            L10n.format("error.api", code)
        }
        static var notFound: String { L10n.text("error.notFound") }
        static var missingData: String { L10n.text("error.missingData") }
        static func decoding(_ message: String) -> String {
            L10n.format("error.decoding", message)
        }
        static var unknown: String { L10n.text("error.unknown") }
    }
}
