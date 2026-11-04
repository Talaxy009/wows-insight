import Foundation

/// Language used to pick a ship name from the encyclopedia payload, which
/// carries a Chinese and an English name for every ship.
enum ShipNameLanguage: Sendable {
    case english
    case chinese
}

/// Runtime configuration derived from the language iOS selected for the app.
///
/// There is deliberately no API key: the Yuyuko API serves every endpoint used
/// by this app without authentication.
struct AppConfig: Sendable {
    static let shared = AppConfig()

    let shipNameLanguage: ShipNameLanguage

    private init() {
        let preferred = Bundle.main.preferredLocalizations.first
            ?? Locale.preferredLanguages.first
            ?? "en"

        // The API carries Chinese, English and Russian ship names only.
        // Both zh-Hans and zh-Hant share the Chinese names; ja / ko fall back
        // to English like any other unsupported language.
        shipNameLanguage = preferred.hasPrefix("zh") ? .chinese : .english
    }
}
