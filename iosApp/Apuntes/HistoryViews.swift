import SwiftUI

struct SavedGamesView: View {
    let active: Bool
    @EnvironmentObject private var store: AppStore
    @State private var abandoning: SavedItem?

    var body: some View {
        let items = store.savedItems(active: active)
        Group {
            if items.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: active ? "play.circle" : "clock.arrow.circlepath").font(.largeTitle).foregroundStyle(.secondary)
                    Text(active ? "No games to resume" : "No completed games yet").font(.headline)
                }.padding().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(items) { item in
                    NavigationLink(value: item.route) {
                        VStack(alignment: .leading, spacing: 8) {
                            Label(item.title, systemImage: item.tournament ? "trophy" : "rectangle.split.2x1")
                                .font(.headline).fixedSize(horizontal: false, vertical: true)
                            HStack {
                                Text(LocalizedStringKey(item.status.capitalized))
                                Spacer()
                                if let date = ISO8601DateFormatter().date(from: item.updatedAt) {
                                    Text(date, style: .date)
                                }
                            }.font(.caption).foregroundStyle(.secondary)
                        }.padding(.vertical, 4)
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        if active {
                            Button(role: .destructive) { abandoning = item } label: {
                                Label("Abandon", systemImage: "stop.circle")
                            }
                        }
                    }
                    .contextMenu {
                        if active {
                            Button(role: .destructive) { abandoning = item } label: {
                                Label("Abandon", systemImage: "stop.circle")
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle(active ? "Resume Games" : "History")
        .confirmationDialog("Abandon this game?", isPresented: Binding(get: { abandoning != nil }, set: { if !$0 { abandoning = nil } }), titleVisibility: .visible) {
            Button("Abandon", role: .destructive) {
                if let item = abandoning {
                    store.send(item.tournament ? "abandonTournament" : "abandonSeries", entity: item.id)
                    abandoning = nil
                }
            }
        } message: { Text("Score history will be kept.") }
    }
}

struct MatchHistoryView: View {
    let matchID: String
    @EnvironmentObject private var store: AppStore

    var body: some View {
        Group {
            if let match = store.match(matchID) {
                List {
                    Section {
                        Text(match.title).font(.headline)
                        LabeledContent("Status") { Text(LocalizedStringKey(match.status.capitalized)) }
                        LabeledContent("Series score", value: "\(match.wins("A")) - \(match.wins("B"))")
                        LabeledContent("Total points", value: "\(match.total("A")) - \(match.total("B"))")
                        Text("Best of \(match.bestOf) · Target \(match.target)").foregroundStyle(.secondary)
                        if let winner = match.winnerName { Label("Winner: \(winner)", systemImage: "trophy") }
                    }
                    ForEach(match.games) { game in
                        Section("Game \(game.number)") {
                            LabeledContent(match.a.name, value: "\(game.score("A"))")
                            LabeledContent(match.b.name, value: "\(game.score("B"))")
                            if let winner = game.winner {
                                Text("Winner: \(winner == "A" ? match.a.name : match.b.name)").font(.headline)
                            }
                            DisclosureGroup("Score history (\(game.events.count))") {
                                ForEach(Array(game.events.enumerated()), id: \.element.id) { index, event in
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text("#\(index + 1) · \(event.side == "A" ? match.a.name : match.b.name) · \(event.points)")
                                            .font(.subheadline).monospacedDigit()
                                        Text(LocalizedStringKey(event.kind.capitalized)).font(.caption).foregroundStyle(.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
            } else { Text("Match not found") }
        }.navigationTitle("Game History").navigationBarTitleDisplayMode(.inline)
    }
}
