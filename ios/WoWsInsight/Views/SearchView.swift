import SwiftUI
import UniformTypeIdentifiers

struct SearchView: View {
    @State private var model = SearchModel()
    @State private var isImportingFavorites = false
    @State private var importAlert: ImportAlert?

    @Environment(FavoritesStore.self) private var favorites

    var body: some View {
        @Bindable var model = model

        NavigationStack {
            content
                .navigationTitle(L10n.App.title)
                .navigationDestination(for: PlayerTarget.self) { target in
                    PlayerView(account: target.player, server: target.server)
                }
                .searchable(
                    text: $model.query,
                    placement: .navigationBarDrawer(displayMode: .always),
                    prompt: L10n.Search.prompt
                )
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        serverMenu
                    }
                    // iOS 26 merges adjacent trailing items into one glass pill;
                    // a fixed spacer keeps the two menus visually separate.
                    if #available(iOS 26.0, *) {
                        ToolbarSpacer(.fixed, placement: .topBarTrailing)
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        favoritesMenu
                    }
                }
                .task(id: model.requestKey) {
                    await model.search()
                }
                .onChange(of: model.server) { _, _ in
                    model.clear()
                }
                .fileImporter(
                    isPresented: $isImportingFavorites,
                    allowedContentTypes: [.json]
                ) { result in
                    importFavorites(result)
                }
                .alert(
                    importAlert?.title ?? "",
                    isPresented: isShowingImportAlert,
                    presenting: importAlert
                ) { _ in
                    Button(L10n.Common.done) {}
                } message: { alert in
                    Text(alert.message)
                }
        }
    }

    @ViewBuilder
    private var content: some View {
        if !model.hasValidQuery {
            favoritesContent
        } else if let errorMessage = model.errorMessage {
            ErrorStateView(
                title: L10n.Search.errorTitle,
                message: errorMessage,
                symbol: "wifi.exclamationmark"
            ) {
                Task { await model.search() }
            }
        } else if model.isSearching && model.results.isEmpty {
            LoadingStateView(title: L10n.Search.searching)
        } else if model.results.isEmpty {
            ContentUnavailableView.search(text: model.trimmedQuery)
        } else {
            List(model.results) { player in
                NavigationLink(
                    value: PlayerTarget(player: player, server: model.server)
                ) {
                    PlayerRow(player: player)
                }
                .playerCell()
                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                    favoriteButton(for: player)
                }
            }
            .appListStyle()
            .overlay(alignment: .top) {
                if model.isSearching {
                    ProgressView()
                        .padding(8)
                }
            }
        }
    }

    @ViewBuilder
    private var favoritesContent: some View {
        if favorites.players.isEmpty {
            ContentUnavailableView {
                Label(L10n.Search.emptyTitle, systemImage: "magnifyingglass")
            } description: {
                Text(L10n.Search.emptyMessage)
            }
        } else {
            List {
                Section {
                    ForEach(favorites.players) { favorite in
                        NavigationLink(
                            value: PlayerTarget(
                                player: favorite.summary,
                                server: favorite.server
                            )
                        ) {
                            PlayerRow(player: favorite.summary, server: favorite.server)
                        }
                        .playerCell()
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(role: .destructive) {
                                favorites.remove(favorite)
                            } label: {
                                Label(L10n.Search.favoriteRemove, systemImage: "star.slash")
                            }
                        }
                    }
                    .onMove { offsets, destination in
                        favorites.move(fromOffsets: offsets, toOffset: destination)
                    }
                } header: {
                    Text(L10n.Search.favoritesHeader)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .textCase(nil)
                }
            }
            .appListStyle()
        }
    }

    @ViewBuilder
    private func favoriteButton(for player: PlayerSummary) -> some View {
        let isFavorite = favorites.contains(player, server: model.server)
        Button {
            favorites.toggle(player, server: model.server)
        } label: {
            Label(
                isFavorite ? L10n.Search.favoriteRemove : L10n.Search.favoriteAdd,
                systemImage: isFavorite ? "star.slash" : "star"
            )
        }
        .tint(.orange)
    }

    private var favoritesMenu: some View {
        Menu {
            Button {
                isImportingFavorites = true
            } label: {
                Label(L10n.Search.importFavorites, systemImage: "square.and.arrow.down")
            }

            ShareLink(
                item: favorites.exportDocument(),
                preview: SharePreview(L10n.Search.exportPreview)
            ) {
                Label(L10n.Search.exportFavorites, systemImage: "square.and.arrow.up")
            }
            .disabled(favorites.players.isEmpty)
        } label: {
            Label(L10n.Search.transferMenu, systemImage: "ellipsis.circle")
        }
    }

    private var isShowingImportAlert: Binding<Bool> {
        Binding(
            get: { importAlert != nil },
            set: { if !$0 { importAlert = nil } }
        )
    }

    private func importFavorites(_ result: Result<URL, any Error>) {
        let url: URL
        switch result {
        case .success(let picked):
            url = picked
        case .failure(let error):
            // Swallowing this used to make a rejected file look like a dead button.
            importAlert = ImportAlert(
                title: L10n.Search.importFailedTitle,
                message: L10n.Search.importFailedPickerMessage(error.localizedDescription)
            )
            return
        }

        let isScoped = url.startAccessingSecurityScopedResource()
        defer {
            if isScoped {
                url.stopAccessingSecurityScopedResource()
            }
        }

        do {
            let archive = try FavoritesArchive.decode(from: Data(contentsOf: url))
            let summary = favorites.importArchive(archive)
            importAlert = ImportAlert(
                title: L10n.Search.importDoneTitle,
                message: L10n.Search.importSummary(
                    added: summary.added,
                    skipped: summary.skipped
                )
            )
        } catch {
            importAlert = ImportAlert(
                title: L10n.Search.importFailedTitle,
                message: L10n.Search.importFailedMessage
            )
        }
    }

    private var serverMenu: some View {
        Menu {
            ForEach(GameServer.allCases) { server in
                Button {
                    model.server = server
                } label: {
                    if server == model.server {
                        Label(server.displayName, systemImage: "checkmark")
                    } else {
                        Text(server.displayName)
                    }
                }
            }
        } label: {
            // A toolbar `Label` renders icon-only, so the current server is
            // spelled out with an explicit HStack to match Android.
            HStack(spacing: 4) {
                Image(systemName: "globe")
                Text(model.server.displayName)
                    .lineLimit(1)
            }
        }
    }
}

/// The value pushed onto the navigation stack from the search list.
struct PlayerTarget: Hashable {
    let player: PlayerSummary
    let server: GameServer
}

/// The one-shot alert shown after an import attempt.
private struct ImportAlert: Identifiable {
    let id = UUID()
    let title: String
    let message: String
}

private extension View {
    /// Shared chrome for one player cell: no separator and a card drawn as the
    /// row background, so the disclosure chevron sits inside it.
    func playerCell() -> some View {
        listRowSeparator(.hidden)
            .listRowInsets(
                EdgeInsets(top: 4, leading: 32, bottom: 4, trailing: 32)
            )
            .listRowBackground(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Color.appCard)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 4)
            )
    }
}
