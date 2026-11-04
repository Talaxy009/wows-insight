import SwiftUI

struct RootView: View {
    @State private var favorites = FavoritesStore()

    var body: some View {
        SearchView()
            .environment(favorites)
    }
}

/// A short explanation of the wows-numbers colour scale.
struct RatingScaleView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(RatingTier.allCases.filter { $0 != .unknown }) { tier in
                        HStack(spacing: 12) {
                            Circle()
                                .fill(tier.color)
                                .frame(width: 10, height: 10)
                            Text(tier.label)
                            Spacer()
                            Text(tier.rangeLabel)
                                .foregroundStyle(.secondary)
                                .monospacedDigit()
                        }
                    }
                } footer: {
                    Text(L10n.Rating.Scale.footer)
                }
            }
            .navigationTitle(L10n.Rating.Scale.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.Common.done) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
