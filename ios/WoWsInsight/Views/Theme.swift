import SwiftUI
import UIKit

extension Color {
    init(hex: String) {
        let cleaned = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        let value = UInt64(cleaned, radix: 16) ?? 0

        let red: Double
        let green: Double
        let blue: Double
        let alpha: Double

        switch cleaned.count {
        case 8:
            red = Double((value >> 24) & 0xFF) / 255
            green = Double((value >> 16) & 0xFF) / 255
            blue = Double((value >> 8) & 0xFF) / 255
            alpha = Double(value & 0xFF) / 255
        default:
            red = Double((value >> 16) & 0xFF) / 255
            green = Double((value >> 8) & 0xFF) / 255
            blue = Double(value & 0xFF) / 255
            alpha = 1
        }

        self.init(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }

    static var appBackground: Color { Color(uiColor: .systemGroupedBackground) }
    static var appCard: Color { Color(uiColor: .secondarySystemGroupedBackground) }
    static var appPlaceholder: Color { Color(uiColor: .tertiarySystemGroupedBackground) }
}

extension RatingTier {
    var color: Color { Color(hex: hexColor) }
}

/// The type scale shared by the screens.
///
/// Everything uses a text style rather than a point size so the whole app tracks
/// Dynamic Type. `footnote` (13pt) is the smallest step used for readable text:
/// `caption` and `caption2` are 12pt and 11pt at the default size, which is too
/// small to read there. Hierarchy comes from weight and colour, not from
/// shrinking the font.
extension Font {
    /// Field name above a value, e.g. "Avg. damage".
    static let statLabel = Font.footnote
    /// Extra line under a value, e.g. the ship a record was achieved on.
    static let statNote = Font.footnote
    /// The number a stat cell is about, in the three-column overview.
    static let statValue = Font.system(.headline, design: .rounded)
    /// The number in the denser two-column grids.
    static let detailValue = Font.subheadline.weight(.semibold)
    /// Secondary line of a list row.
    static let rowSubtitle = Font.footnote
    /// Trailing disclosure chevron of a list row.
    static let rowChevron = Font.footnote.weight(.semibold)
    /// The large personal rating number.
    static let ratingValue = Font.system(.largeTitle, design: .rounded).weight(.bold)
    /// The rating tier name under that number.
    static let ratingTier = Font.footnote.weight(.medium)
}

extension View {
    /// A simple grouped card used by the detail screens.
    func cardStyle() -> some View {
        padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                Color.appCard,
                in: RoundedRectangle(cornerRadius: 16, style: .continuous)
            )
    }

    /// Plain list chrome drawn on the app background, shared by the search
    /// results and the favorites list.
    func appListStyle() -> some View {
        listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(Color.appBackground)
    }
}

/// The scroll container shared by the player and the ship detail screens.
struct DetailScrollView<Content: View>: View {
    private let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                content
            }
            .padding(16)
        }
        .background(Color.appBackground)
    }
}
