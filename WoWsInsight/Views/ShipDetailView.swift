import SwiftUI

struct ShipDetailView: View {
    let ship: ShipPerformance
    /// The battle type selected on the player page; only that mode is shown.
    let mode: BattleMode

    var body: some View {
        DetailScrollView {
            ShipHeaderCard(ship: ship, stats: stats)

            if let stats {
                StatsSections(
                    stats: stats,
                    records: BattleRecords.list(from: stats.stats, catalog: [:])
                )
            }
        }
        .navigationTitle(ship.ship.name)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var stats: ModeStats? { ship.stats(for: mode) }
}

private struct ShipHeaderCard: View {
    let ship: ShipPerformance
    let stats: ModeStats?

    var body: some View {
        VStack(spacing: 16) {
            shipImage
                .frame(height: 130)

            HStack(alignment: .center, spacing: 16) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(ship.ship.name)
                        .font(.title3.weight(.bold))
                        .lineLimit(2)
                    Text(
                        L10n.Ship.meta(
                            tier: ship.ship.tierLabel,
                            type: ship.ship.typeLabel,
                            nation: ship.ship.nation.label
                        )
                    )
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(L10n.Ship.lastBattle(StatFormat.date(stats?.lastBattleTime)))
                        .font(.statNote)
                        .foregroundStyle(.tertiary)
                }
                Spacer(minLength: 8)
                RatingBadge(rating: stats?.rating ?? .unknown)
            }
        }
        .cardStyle()
    }

    @ViewBuilder
    private var shipImage: some View {
        if let url = ship.ship.imageURL {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFit()
                case .failure:
                    imagePlaceholder
                case .empty:
                    ProgressView()
                @unknown default:
                    imagePlaceholder
                }
            }
        } else {
            imagePlaceholder
        }
    }

    private var imagePlaceholder: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color.appPlaceholder)
            Image(systemName: "ferry.fill")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
        }
    }
}
