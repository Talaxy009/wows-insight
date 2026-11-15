import SwiftUI

/// Column layouts shared by the statistic cards.
enum StatGridLayout {
    /// Two columns, used by the detailed statistics and the records.
    static let two = [GridItem(.flexible(), spacing: 16), GridItem(.flexible(), spacing: 16)]

    /// Three columns, used by the overview.
    static let three = Array(repeating: GridItem(.flexible(), spacing: 12), count: 3)
}

/// Circular personal rating badge.
struct RatingBadge: View {
    let rating: PlayerRating

    var body: some View {
        VStack(spacing: 2) {
            Text(rating.displayText)
                .font(.ratingValue)
                .monospacedDigit()
            Text(rating.tier.label)
                .font(.ratingTier)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .foregroundStyle(rating.tier.color)
    }
}

/// One cell of the three-column overview grid.
struct StatCell: View {
    let symbol: String
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 5) {
            Image(systemName: symbol)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.statValue)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Text(label)
                .font(.statLabel)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity)
    }
}

/// One cell of the two-column detailed statistics grid.
struct DetailStat: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.statLabel)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Text(value)
                .font(.detailValue)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// One cell of the record grid.
struct RecordCell: View {
    let record: BattleRecord

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(record.title)
                .font(.statLabel)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Text(StatFormat.integer(record.value))
                .font(.detailValue)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            if let shipName = record.shipName {
                Text(shipName)
                    .font(.statNote)
                    .foregroundStyle(.tertiary)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
    }
}

/// A player cell shared by the search results and the favorites list.
///
/// The API has no player avatars, so the row leads with the nickname and keeps
/// the account ID as quiet secondary text instead of showing a placeholder
/// icon that every row would repeat. The card background is applied by the
/// enclosing list so the disclosure chevron stays inside it.
struct PlayerRow: View {
    let player: PlayerSummary
    var server: GameServer?

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(player.nickname)
                .font(.headline)
                .lineLimit(1)
            Text(detail)
                .font(.rowSubtitle)
                .monospacedDigit()
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 10)
    }

    private var detail: String {
        guard let server else {
            return L10n.Player.accountID(String(player.accountId))
        }
        return L10n.Player.accountID(server: server.displayName, id: String(player.accountId))
    }
}

/// Full-screen error with a retry action, shared by the search and player screens.
struct ErrorStateView: View {
    let title: String
    let message: String
    var symbol = "exclamationmark.triangle"
    let retry: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: symbol)
        } description: {
            Text(message)
        } actions: {
            Button(L10n.Common.retry, action: retry)
                .buttonStyle(.borderedProminent)
        }
    }
}

/// Full-screen loading indicator.
struct LoadingStateView: View {
    let title: String

    var body: some View {
        ProgressView(title)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
