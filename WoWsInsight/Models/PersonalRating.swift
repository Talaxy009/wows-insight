import Foundation

/// A personal rating value (-1 means "not rated").
struct PlayerRating: Hashable, Sendable {
    let value: Int

    static let unknown = PlayerRating(value: -1)

    var isValid: Bool { value >= 0 }
    var tier: RatingTier { RatingTier(rating: value) }

    /// The rating as displayed in badges and rows; an em dash when the API has
    /// no rating for the account or ship.
    var displayText: String { isValid ? value.formatted() : StatFormat.placeholder }
}

/// Rating boundaries and labels used by wows-numbers.
enum RatingTier: Int, CaseIterable, Identifiable, Sendable {
    case unknown
    case bad
    case belowAverage
    case average
    case good
    case veryGood
    case great
    case unicum
    case superUnicum

    var id: Int { rawValue }

    /// Lowest rating that belongs to this tier. `allCases` is declared in
    /// ascending order, which both `init(rating:)` and `rangeLabel` rely on.
    /// `nil` marks the tier used only when no rating is available.
    var lowerBound: Int? {
        switch self {
        case .unknown: nil
        case .bad: 0
        case .belowAverage: 750
        case .average: 1_100
        case .good: 1_350
        case .veryGood: 1_550
        case .great: 1_750
        case .unicum: 2_100
        case .superUnicum: 2_450
        }
    }

    init(rating: Int) {
        guard rating >= 0 else {
            self = .unknown
            return
        }
        self = Self.allCases.last { ($0.lowerBound ?? .max) <= rating } ?? .bad
    }

    var label: String {
        L10n.Rating.label(for: self)
    }

    /// "0 – 749", "2450+" and "—" for the unknown tier.
    var rangeLabel: String {
        guard let lowerBound else { return StatFormat.placeholder }
        guard let next = RatingTier(rawValue: rawValue + 1)?.lowerBound else {
            return "\(lowerBound)+"
        }
        return "\(lowerBound) – \(next - 1)"
    }

    var hexColor: String {
        switch self {
        case .unknown: "#607D8B"
        case .bad: "#D32F2F"
        case .belowAverage: "#FF9800"
        case .average: "#FFB300"
        case .good: "#7CB342"
        case .veryGood: "#388E3C"
        case .great: "#03A9F4"
        case .unicum: "#9C27B0"
        case .superUnicum: "#673AB7"
        }
    }
}
