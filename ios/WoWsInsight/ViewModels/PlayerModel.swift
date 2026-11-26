import Foundation
import Observation

@MainActor
@Observable
final class PlayerModel {
    let account: PlayerSummary
    let server: GameServer

    private(set) var report: PlayerReport?
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    private let client: YuyukoClient

    init(
        account: PlayerSummary,
        server: GameServer,
        client: YuyukoClient = YuyukoClient()
    ) {
        self.account = account
        self.server = server
        self.client = client
    }

    func loadIfNeeded() async {
        guard report == nil else { return }
        await load()
    }

    func load() async {
        guard !isLoading else { return }

        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            async let infoRequest = client.accountInfo(
                accountID: account.accountId,
                server: server
            )
            async let shipsRequest = client.shipStats(
                accountID: account.accountId,
                server: server
            )
            // Never throws: a player without daily records simply gets no chart.
            async let dailyRequest = client.recentDays(
                accountID: account.accountId,
                server: server
            )

            let (info, shipStats, daily) = try await (infoRequest, shipsRequest, dailyRequest)
            guard !Task.isCancelled else { return }

            report = PlayerReport(
                overview: info,
                shipStats: shipStats,
                dailyStats: daily
            )
        } catch {
            guard !error.isCancellation else { return }
            errorMessage = error.localizedDescription
        }
    }

    /// Dismisses the error raised by a failed refresh while keeping the report
    /// that is still on screen.
    func dismissError() {
        errorMessage = nil
    }
}
