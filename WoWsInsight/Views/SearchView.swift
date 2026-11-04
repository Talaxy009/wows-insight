import SwiftUI
import UniformTypeIdentifiers

struct SearchView: View {
    @State private var model = SearchModel()
    @State private var path = NavigationPath()
    @State private var editMode: EditMode = .inactive
    @State private var isImportingFavorites = false
    @State private var importAlert: ImportAlert?

    @Environment(FavoritesStore.self) private var favorites

    var body: some View {
        @Bindable var model = model

        NavigationStack(path: $path) {
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
                        favoritesMenu
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        serverMenu
                    }
                }
                .environment(\.editMode, $editMode)
                .task(id: model.requestKey) {
                    await model.search()
                }
                .onChange(of: model.server) { _, _ in
                    model.clear()
                }
                .onChange(of: model.hasValidQuery) { _, isSearching in
                    if isSearching {
                        editMode = .inactive
                    }
                }
                .onChange(of: favorites.players.isEmpty) { _, isEmpty in
                    if isEmpty {
                        editMode = .inactive
                    }
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

    /// The favorites list is the only place where rows can be reordered.
    private var showsFavoriteControls: Bool {
        !model.hasValidQuery && !favorites.players.isEmpty
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
                        favoriteRow(favorite)
                            .playerCell(isEditing: editMode.isEditing)
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

    /// One favourites row. `NavigationLink` always draws its own chevron, which
    /// would end up next to the reorder handle while editing, so the row pushes
    /// the player page itself and swaps the chevron for a delete button when
    /// `editMode` is active. The delete button is drawn inside the card instead
    /// of using `onDelete`: the system control sits flush against the card's
    /// leading edge, while this one keeps the same inset as the text.
    @ViewBuilder
    private func favoriteRow(_ favorite: FavoritePlayer) -> some View {
        HStack(spacing: 12) {
            if editMode.isEditing {
                Button {
                    favorites.remove(favorite)
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.red)
                        .frame(width: 26)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(L10n.Search.favoriteRemove)
            }

            Button {
                guard !editMode.isEditing else { return }
                path.append(
                    PlayerTarget(player: favorite.summary, server: favorite.server)
                )
            } label: {
                HStack(spacing: 10) {
                    PlayerRow(player: favorite.summary, server: favorite.server)
                    if !editMode.isEditing {
                        Image(systemName: "chevron.right")
                            .font(.rowChevron)
                            .foregroundStyle(.tertiary)
                    }
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
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
                editMode = editMode.isEditing ? .inactive : .active
            } label: {
                Label(
                    editMode.isEditing ? L10n.Common.done : L10n.Common.edit,
                    systemImage: editMode.isEditing ? "checkmark" : "arrow.up.arrow.down"
                )
            }
            .disabled(!showsFavoriteControls)

            Divider()

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
        guard case .success(let url) = result else { return }

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
            Label(model.server.displayName, systemImage: "globe")
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
    ///
    /// While editing the list reserves ~24pt at the trailing edge for the
    /// system reorder handle, which is laid out against the cell edge and
    /// ignores `listRowInsets`. The card and the content are pulled in by the
    /// same amount so the handle keeps a gap from the card instead of sitting
    /// on its edge.
    func playerCell(isEditing: Bool = false) -> some View {
        listRowSeparator(.hidden)
            .listRowInsets(
                EdgeInsets(
                    top: 4,
                    leading: 32,
                    bottom: 4,
                    trailing: isEditing ? 62 : 32
                )
            )
            .listRowBackground(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Color.appCard)
                    .padding(.horizontal, 16)
                    .padding(.trailing, isEditing ? 30 : 0)
                    .padding(.vertical, 4)
            )
    }
}
