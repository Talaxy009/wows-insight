import SwiftUI

/// The about page, pushed from the last item of the favorites menu.
///
/// A grouped list drawn on the app background: a centred header with the app
/// icon, name, version and tagline, then the external links, and a quiet footer
/// with the developer credit and the disclaimer.
struct AboutView: View {
    var body: some View {
        List {
            Section {
                header
            }

            Section {
                Link(destination: AboutURL.github) {
                    AboutLinkRow(
                        title: L10n.About.github,
                        subtitle: L10n.About.githubSubtitle,
                        symbol: "chevron.left.forwardslash.chevron.right",
                        tint: .primary
                    )
                }

                Link(destination: AboutURL.yuyuko) {
                    AboutLinkRow(
                        title: L10n.About.yuyuko,
                        subtitle: L10n.About.yuyukoSubtitle,
                        symbol: "water.waves",
                        tint: .blue
                    )
                }
            } header: {
                sectionHeader
            } footer: {
                sectionFooter
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .background(Color.appBackground)
        .navigationTitle(L10n.About.title)
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Header

    private var header: some View {
        VStack(spacing: 14) {
            appIcon

            VStack(spacing: 4) {
                Text(L10n.App.title)
                    .font(.title2.weight(.bold))

                if let versionText {
                    Text(versionText)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }

                Text(L10n.About.tagline)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 4)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        // The header is page content, not a card: no row fill and no separator.
        .listRowSeparator(.hidden)
        .listRowBackground(Color.clear)
    }

    private var appIcon: some View {
        Image("AppIconPreview")
            .resizable()
            .scaledToFit()
            .frame(width: 96, height: 96)
            // Matches the iOS icon mask: continuous corners plus a hairline
            // border so the icon edge stays visible on a light background.
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .strokeBorder(Color.primary.opacity(0.1), lineWidth: 0.5)
            }
            .accessibilityHidden(true)
    }

    /// Marketing version and build number of the running app. The line is
    /// omitted (instead of rendering an empty pair) when the bundle has no
    /// version keys, e.g. in a preview.
    private var versionText: String? {
        guard
            let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String,
            let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String,
            !version.isEmpty,
            !build.isEmpty
        else {
            return nil
        }
        return L10n.About.version(version, build: build)
    }

    // MARK: - Links

    private var sectionHeader: some View {
        Text(L10n.About.linksSection)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(.secondary)
            .textCase(nil)
    }

    private var sectionFooter: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(L10n.About.developer)
            Text(L10n.About.disclaimer)
        }
        .font(.footnote)
        .foregroundStyle(.secondary)
        .fixedSize(horizontal: false, vertical: true)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Link row

/// One external-link row: an icon tile, the title over its subtitle, and a
/// trailing arrow that hints the link leaves the app.
private struct AboutLinkRow: View {
    let title: String
    let subtitle: String
    let symbol: String
    let tint: Color

    @ScaledMetric(relativeTo: .body) private var tileSize: CGFloat = 30

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.callout.weight(.semibold))
                .foregroundStyle(tint)
                .frame(width: tileSize, height: tileSize)
                .background(
                    tint.opacity(0.12),
                    in: RoundedRectangle(cornerRadius: tileSize * 0.27, style: .continuous)
                )
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.body)
                    .foregroundStyle(Color.primary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(subtitle)
                    .font(.rowSubtitle)
                    .foregroundStyle(Color.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 8)

            Image(systemName: "arrow.up.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Color.secondary)
                .accessibilityHidden(true)
        }
        .padding(.vertical, 6)
        .contentShape(Rectangle())
    }
}

/// The external destinations linked from the about page.
private enum AboutURL {
    static let github = URL(string: "https://github.com/Talaxy009/wows-insight")!
    static let yuyuko = URL(string: "https://wows.shinoaki.com/")!
}
