import SwiftUI

// MARK: - Cards

struct PlayerHeaderCard: View {
    let account: AccountInfo
    let server: GameServer
    /// Rating of the battle type the screen is currently showing.
    let rating: PlayerRating

    var body: some View {
        VStack(spacing: 14) {
            HStack(alignment: .top, spacing: 16) {
                DogTagBadge(url: account.dogTagURL)

                VStack(alignment: .leading, spacing: 6) {
                    Text(account.nickname)
                        .font(.title2.weight(.bold))
                        .lineLimit(2)
                    Text(server.displayName)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 8)
                RatingBadge(rating: rating)
            }

            Divider()

            VStack(spacing: 8) {
                InfoRow(title: L10n.Player.registered, value: StatFormat.date(account.createdAt))
                InfoRow(title: L10n.Player.lastBattle, value: StatFormat.date(account.lastBattleTime))
            }
        }
        .cardStyle()
    }
}

/// The in-game dog tag shown to the left of the nickname.
private struct DogTagBadge: View {
    let url: URL?

    var body: some View {
        AsyncImage(url: url) { phase in
            if case .success(let image) = phase {
                image
                    .resizable()
                    .scaledToFit()
            } else {
                placeholder
            }
        }
        .frame(width: 56, height: 56)
        .accessibilityHidden(true)
    }

    private var placeholder: some View {
        RoundedRectangle(cornerRadius: 12, style: .continuous)
            .fill(Color.appPlaceholder)
            .overlay {
                Image(systemName: "shield.lefthalf.filled")
                    .font(.title3)
                    .foregroundStyle(.secondary)
            }
    }
}

/// Switches the whole screen between the battle types the player has played.
struct BattleModePicker: View {
    let modes: [BattleMode]
    @Binding var selection: BattleMode

    var body: some View {
        Menu {
            Picker(L10n.Player.battleType, selection: $selection) {
                ForEach(modes) { mode in
                    Text(mode.label).tag(mode)
                }
            }
        } label: {
            HStack(spacing: 8) {
                Text(L10n.Player.battleType)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Spacer(minLength: 12)
                Text(selection.label)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Image(systemName: "chevron.up.chevron.down")
                    .font(.rowChevron)
                    .foregroundStyle(.tertiary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(
                Color.appCard,
                in: RoundedRectangle(cornerRadius: 12, style: .continuous)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// The stat cards shared by the player page and the ship page.
struct StatsSections: View {
    let stats: ModeStats
    let records: [BattleRecord]

    var body: some View {
        StatsOverviewCard(stats: stats.stats)
        DetailedStatsCard(stats: stats.stats)
        if !records.isEmpty {
            RecordsCard(records: records)
        }
    }
}

private struct StatsOverviewCard: View {
    let stats: BattleStats

    var body: some View {
        StatCard(title: L10n.Player.statsOverview) {
            LazyVGrid(columns: StatGridLayout.three, spacing: 18) {
                ForEach(items) { item in
                    StatCell(symbol: item.symbol, value: item.value, label: item.label)
                }
            }
        }
    }

    private var items: [OverviewItem] {
        [
            OverviewItem(
                id: "battles",
                symbol: "flag.fill",
                value: StatFormat.integer(stats.battleCount),
                label: L10n.Player.statsBattles
            ),
            OverviewItem(
                id: "winRate",
                symbol: "percent",
                value: StatFormat.percent(stats.winRate),
                label: L10n.Player.statsWinRate
            ),
            OverviewItem(
                id: "averageDamage",
                symbol: "flame.fill",
                value: StatFormat.decimal(stats.averageDamage),
                label: L10n.Player.statsAverageDamage
            ),
            OverviewItem(
                id: "averageFrags",
                symbol: "scope",
                value: StatFormat.decimal(stats.averageFrags, digits: 2),
                label: L10n.Player.statsAverageFrags
            ),
            OverviewItem(
                id: "averageExperience",
                symbol: "bolt.fill",
                value: StatFormat.decimal(stats.averageExperience),
                label: L10n.Player.statsAverageXP
            ),
            OverviewItem(
                id: "survivalRate",
                symbol: "heart.fill",
                value: StatFormat.percent(stats.survivalRate, digits: 1),
                label: L10n.Player.statsSurvivalRate
            ),
        ]
    }
}

private struct OverviewItem: Identifiable {
    let id: String
    let symbol: String
    let value: String
    let label: String
}

private struct DetailedStatsCard: View {
    let stats: BattleStats

    var body: some View {
        StatCard(title: L10n.Player.detailsTitle) {
            LazyVGrid(columns: StatGridLayout.two, alignment: .leading, spacing: 14) {
                ForEach(items) { item in
                    DetailStat(title: item.title, value: item.value)
                }
            }
        }
    }

    private var items: [DetailItem] {
        [
            DetailItem(
                id: "winDrawLoss",
                title: L10n.Player.winDrawLoss,
                value: [
                    StatFormat.integer(stats.winCount),
                    StatFormat.integer(stats.drawCount),
                    StatFormat.integer(stats.lossCount),
                ].joined(separator: " / ")
            ),
            DetailItem(id: "totalDamage", title: L10n.Player.totalDamage, value: StatFormat.integer(stats.damage)),
            DetailItem(id: "totalXP", title: L10n.Player.totalXP, value: StatFormat.integer(stats.experience)),
            DetailItem(id: "totalFrags", title: L10n.Player.totalFrags, value: StatFormat.integer(stats.fragCount)),
            DetailItem(
                id: "hitRate",
                title: L10n.Player.hitRate,
                value: StatFormat.percent(stats.hitRate, digits: 1)
            ),
            DetailItem(
                id: "killDeath",
                title: L10n.Player.killDeath,
                value: StatFormat.decimal(stats.killDeathRatio, digits: 2)
            ),
            DetailItem(
                id: "potentialDamage",
                title: L10n.Player.potentialDamage,
                value: StatFormat.integer(stats.potentialDamage)
            ),
            DetailItem(
                id: "spottingDamage",
                title: L10n.Player.spottingDamage,
                value: StatFormat.integer(stats.scoutingDamage)
            ),
            DetailItem(
                id: "shipsSpotted",
                title: L10n.Player.shipsSpotted,
                value: StatFormat.integer(stats.spottedCount)
            ),
            DetailItem(
                id: "planesDestroyed",
                title: L10n.Player.planesDestroyed,
                value: StatFormat.integer(stats.planesDestroyed)
            ),
            DetailItem(
                id: "survivedBattles",
                title: L10n.Player.survivedBattles,
                value: StatFormat.integer(stats.survivedCount)
            ),
            DetailItem(
                id: "survivedWinRate",
                title: L10n.Player.survivedWinRate,
                value: StatFormat.percent(stats.survivedWinRate, digits: 1)
            ),
        ]
    }
}

private struct DetailItem: Identifiable {
    let id: String
    let title: String
    let value: String
}

private struct RecordsCard: View {
    let records: [BattleRecord]

    var body: some View {
        StatCard(title: L10n.Player.recordsTitle) {
            LazyVGrid(columns: StatGridLayout.two, alignment: .leading, spacing: 14) {
                ForEach(records) { record in
                    RecordCell(record: record)
                }
            }
        }
    }
}

struct HiddenProfileCard: View {
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "eye.slash")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text(L10n.Player.hiddenTitle)
                .font(.headline)
            Text(L10n.Player.hiddenMessage)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .cardStyle()
    }
}

/// A card with a headline and a flexible grid body.
private struct StatCard<Content: View>: View {
    let title: String
    let content: Content

    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title)
                .font(.headline)
            content
        }
        .cardStyle()
    }
}

private struct InfoRow: View {
    let title: String
    let value: String

    var body: some View {
        HStack {
            Text(title)
                .foregroundStyle(.secondary)
            Spacer()
            Text(value)
                .monospacedDigit()
        }
        .font(.subheadline)
    }
}
