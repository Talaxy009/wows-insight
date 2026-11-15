import Foundation

/// A warship nation.
///
/// The API mixes display-style names ("usa", "united_kingdom", …) with the
/// keys used by `L10n.Nation`, so normalising is done once here instead of in
/// every view. The raw values match the keys in `Localizable.xcstrings` and are
/// what `L10n.Nation.name(for:)` switches on.
enum ShipNation: String, Sendable {
    case usa
    case japan
    case germany
    case ussr
    case uk
    case france
    case italy
    case panAsia = "pan_asia"
    case europe
    case panAmerica = "pan_america"
    case netherlands
    case spain
    case commonwealth
    case latinAmerica = "latin_america"
    case events
    case unknown

    /// Accepts every spelling the API has used for a nation.
    init(apiName: String?) {
        switch apiName?.lowercased() {
        case "usa": self = .usa
        case "japan": self = .japan
        case "germany": self = .germany
        case "russia", "ussr": self = .ussr
        case "united_kingdom", "uk": self = .uk
        case "france": self = .france
        case "italy": self = .italy
        case "pan_asia": self = .panAsia
        case "europe": self = .europe
        case "pan_america": self = .panAmerica
        case "netherlands": self = .netherlands
        case "spain": self = .spain
        case "commonwealth": self = .commonwealth
        case "latin_america": self = .latinAmerica
        case "events": self = .events
        default: self = .unknown
        }
    }

    var label: String { L10n.Nation.name(for: self) }
}
