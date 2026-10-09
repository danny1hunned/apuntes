import SwiftUI

struct RootView: View {
    @EnvironmentObject private var store: AppStore
    @EnvironmentObject private var ads: AdsManager
    @State private var path: [Route] = []

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if let error = store.loadError {
                    VStack(spacing: 20) {
                        Image(systemName: "externaldrive.badge.exclamationmark").font(.largeTitle)
                        Text("Saved games could not be opened").font(.headline)
                        Text(error).foregroundStyle(.secondary)
                        Button("Try Again") { store.reload() }
                    }.padding()
                } else {
                    home
                }
            }
            .navigationTitle("Apuntes")
            .navigationDestination(for: Route.self) { route in
                switch route {
                case .seriesSetup: SeriesSetupView(path: $path)
                case .tournamentSetup: TournamentSetupView(path: $path)
                case .recovery: SavedGamesView(active: true)
                case .history: SavedGamesView(active: false)
                case .match(let id): MatchView(matchID: id)
                case .tournament(let id): TournamentView(tournamentID: id)
                case .standings(let id): StandingsView(tournamentID: id)
                case .matchHistory(let id): MatchHistoryView(matchID: id)
                }
            }
            .toolbar {
                if ads.privacyOptionsRequired {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button { Task { await ads.showPrivacyOptions() } } label: {
                            Image(systemName: "hand.raised")
                        }.accessibilityLabel("Privacy options")
                    }
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if ads.canShowAds { AdBanner().frame(width: 320, height: 50).frame(maxWidth: .infinity) }
        }
        .task { await ads.start() }
        .alert("Could not complete action", isPresented: Binding(get: { store.errorMessage != nil }, set: { if !$0 { store.errorMessage = nil } })) {
            Button("OK", role: .cancel) { store.errorMessage = nil }
        } message: { Text(store.errorMessage ?? "") }
        .alert("Privacy options", isPresented: Binding(get: { ads.privacyError != nil }, set: { if !$0 { ads.privacyError = nil } })) {
            Button("OK", role: .cancel) { ads.privacyError = nil }
        } message: { Text(ads.privacyError ?? "") }
        .sheet(item: $store.notice) { notice in
            WinnerView(notice: notice).presentationDetents([.medium, .large])
        }
    }

    private var home: some View {
        List {
            Section {
                Image("Dominoes").resizable().scaledToFit()
                    .frame(maxWidth: .infinity).frame(height: 150).accessibilityHidden(true)
                    .listRowBackground(Color.clear)
            }
            Section {
                if !store.savedItems(active: true).isEmpty {
                    NavigationLink(value: Route.recovery) {
                        Label("Resume Games (\(store.savedItems(active: true).count))", systemImage: "play.circle")
                    }.accessibilityIdentifier("resumeGames")
                }
                NavigationLink(value: Route.seriesSetup) { Label("Start Series", systemImage: "plus.circle") }
                    .accessibilityIdentifier("startSeries")
                NavigationLink(value: Route.tournamentSetup) { Label("Start Tournament", systemImage: "trophy") }
                    .accessibilityIdentifier("startTournament")
                NavigationLink(value: Route.history) { Label("History", systemImage: "clock.arrow.circlepath") }
                    .accessibilityIdentifier("history")
            }
        }.listStyle(.insetGrouped)
    }
}

struct WinnerView: View {
    let notice: GameNotice
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var appeared = false

    private var title: LocalizedStringKey {
        switch notice.kind {
        case "champion": return "Tournament Champion"
        case "series": return "Series Winner"
        default: return "Game Winner"
        }
    }

    var body: some View {
        ScrollView {
          VStack(spacing: 18) {
            Image(systemName: "trophy.fill").font(.system(size: 56)).foregroundStyle(.yellow)
                .scaleEffect(appeared ? 1 : 0.7)
            Text(title).font(.headline)
            Text(notice.winnerName).font(.title2.bold()).multilineTextAlignment(.center)
            Text("\(notice.scoreA) - \(notice.scoreB)").font(.title3.monospacedDigit())
            Button("Continue") { dismiss() }.buttonStyle(.borderedProminent)
                .accessibilityIdentifier("continueAfterWin")
          }.padding(24).frame(maxWidth: .infinity)
        }
            .onAppear { withAnimation(reduceMotion ? nil : .spring()) { appeared = true } }
    }
}
