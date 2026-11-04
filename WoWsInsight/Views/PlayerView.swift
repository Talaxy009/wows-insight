import SwiftUI

struct PlayerView: View {
    let account: PlayerSummary
    let server: GameServer

    @State private var model: PlayerModel
    @State private var sort: ShipSort = .defaultSort
    @State private var selectedMode: BattleMode?
    @State private var showRatingInfo = false

    @Environment(FavoritesStore.self) private var favorites

    init(account: PlayerSummary, server: GameServer) {
        self.account = account
        self.server = server
        _model = State(initialValue: PlayerModel(account: account, server: server))
    }

    private var currentPlayer: PlayerSummary {
        guard let info = model.report?.account else { return account }
        return PlayerSummary(accountId: info.accountId, nickname: info.nickname)
    }

    private var isFavorite: Bool {
        favorites.contains(currentPlayer, server: server)
    }

    var body: some View {
        content
            .navigationTitle(model.report?.account.nickname ?? account.nickname)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        favorites.toggle(currentPlayer, server: server)
                    } label: {
                        Image(systemName: isFavorite ? "star.fill" : "star")
                    }
                    .accessibilityLabel(isFavorite ? L10n.Player.favoriteRemove : L10n.Player.favoriteAdd)

                    Button {
                        showRatingInfo = true
                    } label: {
                        Image(systemName: "info.circle")
                    }
                    .disabled(model.report == nil)
                    .accessibilityLabel(L10n.Player.ratingInfo)
                }
            }
            .sheet(isPresented: $showRatingInfo) {
                RatingScaleView()
            }
            .alert(
                L10n.Player.errorTitle,
                isPresented: Binding(
                    get: { model.report != nil && model.errorMessage != nil },
                    set: { if !$0 { model.dismissError() } }
                )
            ) {
                Button(L10n.Common.retry) {
                    Task { await model.load() }
                }
                Button(L10n.Common.done, role: .cancel) {
                    model.dismissError()
                }
            } message: {
                Text(model.errorMessage ?? "")
            }
            .task {
                await model.loadIfNeeded()
                // The API nickname is authoritative; keep favorites in sync.
                favorites.updateNickname(of: currentPlayer, server: server)
            }
    }

    @ViewBuilder
    private var content: some View {
        if let report = model.report {
            reportContent(report)
        } else if let errorMessage = model.errorMessage {
            ErrorStateView(title: L10n.Player.errorTitle, message: errorMessage) {
                Task { await model.load() }
            }
        } else {
            LoadingStateView(title: L10n.Player.loading)
        }
    }

    /// The mode the user picked, falling back to random battles while the report
    /// is loading or when the picked mode has no data.
    private func activeMode(in report: PlayerReport) -> BattleMode? {
        if let selectedMode, report.stats(for: selectedMode) != nil {
            return selectedMode
        }
        return report.defaultMode
    }

    private func reportContent(_ report: PlayerReport) -> some View {
        let mode = activeMode(in: report)

        return DetailScrollView {
            PlayerHeaderCard(
                account: report.account,
                server: server,
                rating: mode.flatMap { report.stats(for: $0)?.rating } ?? .unknown
            )

            if let mode, let stats = report.stats(for: mode) {
                if report.modes.count > 1 {
                    BattleModePicker(
                        modes: report.modes,
                        selection: Binding(
                            get: { mode },
                            set: { selectedMode = $0 }
                        )
                    )
                }

                StatsSections(stats: stats, records: report.records(for: mode))

                ShipsSection(ships: report.ships, mode: mode, sort: $sort)
            } else {
                HiddenProfileCard()
            }
        }
        .refreshable {
            await model.load()
        }
    }
}

// MARK: - Ships

private struct ShipsSection: View {
    let ships: [ShipPerformance]
    let mode: BattleMode
    @Binding var sort: ShipSort

    var body: some View {
        let sorted = ships.ranked(in: mode, by: sort)

        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(L10n.Player.shipsTitle(StatFormat.integer(sorted.count)))
                    .font(.headline)
                Spacer()
                if !sorted.isEmpty {
                    Menu {
                        Picker(L10n.Player.sort, selection: $sort) {
                            ForEach(ShipSort.allCases) { option in
                                Text(option.label).tag(option)
                            }
                        }
                    } label: {
                        Label(sort.label, systemImage: "arrow.up.arrow.down")
                            .font(.subheadline)
                    }
                }
            }

            shipList(sorted)
        }
    }

    @ViewBuilder
    private func shipList(_ sorted: [ShipPerformance]) -> some View {
        if sorted.isEmpty {
            Text(L10n.Player.shipsEmpty)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .padding(.vertical, 8)
        } else {
            VStack(spacing: 0) {
                ForEach(sorted) { ship in
                    NavigationLink {
                        ShipDetailView(ship: ship, mode: mode)
                    } label: {
                        ShipRow(
                            performance: ship,
                            stats: ship.stats(for: mode),
                            showLastBattle: sort == .lastBattle
                        )
                    }
                    .buttonStyle(.plain)

                    if ship.id != sorted.last?.id {
                        Divider()
                            .padding(.leading, 54)
                    }
                }
            }
            .background(
                Color.appCard,
                in: RoundedRectangle(cornerRadius: 16, style: .continuous)
            )
        }
    }
}

private struct ShipRow: View {
    let performance: ShipPerformance
    let stats: ModeStats?
    let showLastBattle: Bool

    var body: some View {
        HStack(spacing: 12) {
            Text(performance.ship.tierLabel)
                .font(.footnote.weight(.bold))
                .foregroundStyle(.secondary)
                .frame(width: 26, alignment: .leading)

            VStack(alignment: .leading, spacing: 3) {
                Text(performance.ship.name)
                    .font(.subheadline.weight(.medium))
                    .lineLimit(1)
                Text(subtitle)
                    .font(.rowSubtitle)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer(minLength: 8)

            VStack(alignment: .trailing, spacing: 3) {
                Text(rating.displayText)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(rating.tier.color)
                    .monospacedDigit()
                Text(L10n.Player.shipAverageDamage(StatFormat.decimal(stats?.stats.averageDamage ?? 0)))
                    .font(.rowSubtitle)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }

            Image(systemName: "chevron.right")
                .font(.rowChevron)
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 16)
        .contentShape(Rectangle())
    }

    private var rating: PlayerRating { stats?.rating ?? .unknown }

    private var subtitle: String {
        guard let stats else { return performance.ship.typeLabel }
        if showLastBattle {
            return "\(performance.ship.typeLabel) · \(StatFormat.date(stats.lastBattleTime)) · \(StatFormat.percent(stats.stats.winRate, digits: 1))"
        }
        return L10n.Player.shipSubtitle(
            type: performance.ship.typeLabel,
            battles: StatFormat.integer(stats.stats.battleCount),
            winRate: StatFormat.percent(stats.stats.winRate, digits: 1)
        )
    }
}
