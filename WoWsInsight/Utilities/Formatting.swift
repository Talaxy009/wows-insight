import Foundation

/// Small, locale-aware display helpers shared by the views.
enum StatFormat {
    /// Shown instead of a value the API did not provide.
    static let placeholder = "—"

    static func integer(_ value: Int) -> String {
        value.formatted()
    }

    static func decimal(_ value: Double, digits: Int = 0) -> String {
        value.formatted(.number.precision(.fractionLength(digits)))
    }

    static func percent(_ value: Double, digits: Int = 2) -> String {
        // `value` is already a percentage (0–100); the percent style applies the
        // locale's spacing and sign.
        (value / 100).formatted(.percent.precision(.fractionLength(digits)))
    }

    static func date(_ timestamp: Int?) -> String {
        guard let timestamp, timestamp > 0 else { return placeholder }
        return Date(timeIntervalSince1970: TimeInterval(timestamp))
            .formatted(date: .numeric, time: .omitted)
    }
}
