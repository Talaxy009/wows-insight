import Foundation

enum YuyukoAPIError: LocalizedError, Sendable {
    case invalidURL
    case badStatus(Int)
    case network(String)
    case notFound
    case api(code: Int, message: String?)
    case missingData
    case decoding(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            L10n.Error.invalidURL
        case .badStatus(let code):
            L10n.Error.httpStatus(String(code))
        case .network(let message):
            message.isEmpty ? L10n.Error.unknown : message
        case .notFound:
            L10n.Error.notFound
        case .api(let code, let message):
            if let message, !message.isEmpty {
                message
            } else {
                L10n.Error.api(String(code))
            }
        case .missingData:
            L10n.Error.missingData
        case .decoding(let message):
            L10n.Error.decoding(message)
        }
    }
}

/// A small async/await wrapper around the Yuyuko World of Warships API
/// (https://v3-api.wows.shinoaki.com/v3/index.html).
///
/// Only `/public/*` endpoints are used, so no credentials are involved.
struct YuyukoClient: Sendable {
    private let language: ShipNameLanguage

    private let session: URLSession

    init(
        language: ShipNameLanguage = AppConfig.shared.shipNameLanguage,
        session: URLSession = YuyukoClient.defaultSession
    ) {
        self.language = language
        self.session = session
    }

    /// Waits for a connection instead of failing at once, and never serves a
    /// cached body: the screens must always show the current totals.
    private static let defaultSession: URLSession = {
        let configuration = URLSessionConfiguration.default
        configuration.waitsForConnectivity = true
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        configuration.urlCache = nil
        return URLSession(configuration: configuration)
    }()

    // MARK: - Endpoints

    /// Fuzzy nickname lookup. The China realm only supports an exact match.
    func searchPlayers(query: String, server: GameServer) async throws -> [PlayerSummary] {
        struct Body: Encodable {
            let userName: String
            let one: Bool
            let limit: Int
        }

        do {
            let users: [YuyukoSearchUser] = try await post(
                path: "public/wows/account/search/\(server.rawValue)/user",
                body: Body(
                    userName: query,
                    one: server.usesExactSearch,
                    limit: Constants.searchLimit
                )
            )
            return users.map { PlayerSummary(accountId: $0.accountId, nickname: $0.userName) }
        } catch YuyukoAPIError.notFound {
            // The exact-match endpoint answers 404 when nothing matches.
            return []
        } catch YuyukoAPIError.api(code: 540, _) where server.usesExactSearch {
            // The China realm reports a partial (or unknown) nickname as an
            // upstream HTTP 400 instead of an empty result.
            return []
        }
    }

    /// Account details plus the totals and rating of every battle type.
    func accountInfo(accountID: Int, server: GameServer) async throws -> AccountOverview {
        let data: YuyukoUserInfoData = try await get(
            path: "public/wows/account/user/info2",
            query: Self.accountQuery(accountID: accountID, server: server)
        )

        guard let user = data.userInfo, let name = user.userName else {
            throw YuyukoAPIError.notFound
        }

        let account = AccountInfo(
            accountId: user.accountId ?? accountID,
            nickname: name,
            createdAt: user.accountCreateTime,
            lastBattleTime: data.lastBattleTime,
            dogTagURL: user.dogTag.flatMap(URL.init(string:))
        )

        return AccountOverview(account: account, modes: data.battleTypeInfo?.modeStats ?? [:])
    }

    /// Per-ship statistics, including the metadata and the totals of every
    /// battle type the ship was played in.
    func shipStats(accountID: Int, server: GameServer) async throws -> [ShipStat] {
        let data: YuyukoShipListData = try await get(
            path: "public/wows/account/ship/info/list",
            query: Self.accountQuery(accountID: accountID, server: server)
        )

        return (data.shipInfo ?? []).compactMap { entry in
            guard let meta = entry.shipInfo else { return nil }
            return ShipStat(
                ship: meta.shipInfo(language: language, server: server),
                modes: entry.typeInfo?.modeStats ?? [:]
            )
        }
    }

    // MARK: - Transport

    private enum Constants {
        static let baseURL = "https://v3-api.wows.shinoaki.com"
        /// Cold players make the service fetch from Wargaming, which takes 6–8 s.
        static let timeout: TimeInterval = 30
        /// Documented as required, and used by the service to tell clients apart.
        static let clientType = "APP;WoWsInsight;1.0"
        static let searchLimit = 25
    }

    private static func accountQuery(accountID: Int, server: GameServer) -> [URLQueryItem] {
        [
            URLQueryItem(name: "server", value: server.rawValue),
            URLQueryItem(name: "accountId", value: String(accountID)),
        ]
    }

    private func get<Payload: Decodable>(
        path: String,
        query: [URLQueryItem]
    ) async throws -> Payload {
        try await send(path: path, query: query, body: nil)
    }

    private func post<Payload: Decodable, Body: Encodable>(
        path: String,
        body: Body
    ) async throws -> Payload {
        let data = try JSONEncoder().encode(body)
        return try await send(path: path, query: [], body: data)
    }

    private func send<Payload: Decodable>(
        path: String,
        query: [URLQueryItem],
        body: Data?
    ) async throws -> Payload {
        var components = URLComponents(string: Constants.baseURL)
        components?.path = "/" + path
        components?.queryItems = query.isEmpty ? nil : query

        guard let url = components?.url else {
            throw YuyukoAPIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.timeoutInterval = Constants.timeout
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(Constants.clientType, forHTTPHeaderField: "Yuyuko-Client-Type")

        if let body {
            request.httpMethod = "POST"
            request.httpBody = body
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch let error as URLError where error.code == .cancelled {
            // `URLSession` reports a cancelled task as a `URLError`, but callers
            // (and SwiftUI's `.task`) treat cancellation as `CancellationError`.
            throw CancellationError()
        } catch let error as URLError {
            throw YuyukoAPIError.network(error.localizedDescription)
        }

        guard let http = response as? HTTPURLResponse else {
            throw YuyukoAPIError.badStatus(0)
        }
        switch http.statusCode {
        case 200..<300:
            break
        case 404, 405:
            // The search endpoint answers 404/405 when nothing matches.
            throw YuyukoAPIError.notFound
        default:
            throw YuyukoAPIError.badStatus(http.statusCode)
        }

        do {
            let envelope = try JSONDecoder().decode(YuyukoEnvelope<Payload>.self, from: data)
            switch envelope.code {
            case 200:
                guard let payload = envelope.data else {
                    throw YuyukoAPIError.missingData
                }
                return payload
            case 404, 405:
                throw YuyukoAPIError.notFound
            default:
                throw YuyukoAPIError.api(code: envelope.code, message: envelope.message)
            }
        } catch let error as YuyukoAPIError {
            throw error
        } catch {
            throw YuyukoAPIError.decoding(error.localizedDescription)
        }
    }
}
