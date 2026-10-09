import SwiftUI

struct MatchView: View {
    let matchID: String
    @EnvironmentObject private var store: AppStore
    @State private var points = ""
    @State private var editingRow: ScoreRow?
    @FocusState private var scoreFocused: Bool

    var body: some View {
        Group {
            if let match = store.match(matchID) {
                VStack(spacing: 0) {
                    ScrollView {
                        VStack(spacing: 0) {
                            scoreboard(match)
                            Divider()
                            HStack(alignment: .top, spacing: 16) {
                                scoreColumn(match, side: "A")
                                scoreColumn(match, side: "B")
                            }.padding()
                        }
                    }
                    if match.isActive {
                        input(match)
                    } else {
                        VStack(spacing: 12) {
                            Text(match.status == "completed" ? "Series Complete" : "Abandoned").font(.headline)
                            if let winner = match.winnerName { Text("Winner: \(winner)") }
                            NavigationLink(value: Route.matchHistory(matchID)) { Label("Game History", systemImage: "clock") }
                            if let tournamentID = match.tournamentId {
                                NavigationLink(value: Route.tournament(tournamentID)) { Label("Tournament", systemImage: "trophy") }
                            }
                        }.padding()
                    }
                }
                .navigationTitle(match.kind == "championship" ? "Championship" : "Scoreboard")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        NavigationLink(value: Route.matchHistory(matchID)) {
                            Image(systemName: "clock.arrow.circlepath")
                        }.accessibilityLabel("Game History")
                    }
                }
            } else { Text("Match not found") }
        }
        .task {
            if store.match(matchID)?.status == "draft" { store.send("openMatch", entity: matchID) }
        }
        .sheet(item: $editingRow, onDismiss: { store.presentPendingNotice() }) { row in
            if let match = store.match(matchID) {
                EditScoreView(match: match, row: row)
            }
        }
    }

    private func scoreboard(_ match: SeriesMatch) -> some View {
        VStack(spacing: 12) {
            Text("Best of \(match.bestOf) · Target \(match.target)").font(.subheadline).foregroundStyle(.secondary)
            HStack(alignment: .top, spacing: 12) {
                teamHeader(match.a.name, score: match.currentGame?.score("A") ?? 0, wins: match.wins("A"), side: "A")
                Text("\(match.wins("A")) - \(match.wins("B"))").font(.headline.monospacedDigit())
                    .frame(minWidth: 54).padding(.top, 8).accessibilityIdentifier("seriesScore")
                teamHeader(match.b.name, score: match.currentGame?.score("B") ?? 0, wins: match.wins("B"), side: "B")
            }
        }.padding().background(Color(.secondarySystemBackground))
    }

    private func teamHeader(_ name: String, score: Int64, wins: Int, side: String) -> some View {
        VStack(spacing: 8) {
            Text(name).font(.headline).multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
            Text("\(score)").font(.system(size: 38, weight: .bold, design: .rounded)).monospacedDigit()
                .minimumScaleFactor(0.5).lineLimit(1).accessibilityIdentifier("score\(side)")
            Text("\(wins) games won").font(.caption).foregroundStyle(.secondary)
        }.frame(maxWidth: .infinity)
    }

    private func scoreColumn(_ match: SeriesMatch, side: String) -> some View {
        let rows = match.currentGame?.visibleRows.filter { $0.side == side } ?? []
        return LazyVStack(spacing: 8) {
            ForEach(rows) { row in
                Button { editingRow = row } label: {
                    HStack {
                        Text("\(row.points)").font(.title3.monospacedDigit())
                        Spacer(minLength: 4)
                        if match.isActive { Image(systemName: "pencil").font(.caption) }
                    }.padding(12).frame(maxWidth: .infinity)
                        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 8))
                }.buttonStyle(.plain).disabled(!match.isActive)
                    .accessibilityLabel("\(side == "A" ? match.a.name : match.b.name): \(row.points)")
                    .accessibilityIdentifier("scoreRow-\(row.id)")
            }
        }.frame(maxWidth: .infinity, alignment: .top)
    }

    private func input(_ match: SeriesMatch) -> some View {
        VStack(spacing: 12) {
            HStack {
                TextField("Round score", text: $points).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
                    .focused($scoreFocused).accessibilityIdentifier("roundScore")
                Button {
                    store.send("undo", entity: matchID)
                } label: { Image(systemName: "arrow.uturn.backward") }
                    .frame(width: 44, height: 44).accessibilityLabel("Undo last score").accessibilityIdentifier("undoScore")
                    .disabled(match.currentGame?.visibleRows.isEmpty != false)
                if scoreFocused {
                    Button { scoreFocused = false } label: { Image(systemName: "keyboard.chevron.compact.down") }
                        .frame(width: 44, height: 44).accessibilityLabel("Dismiss keyboard")
                }
            }
            HStack(spacing: 12) {
                scoreButton(match.a.name, side: "A")
                scoreButton(match.b.name, side: "B")
            }
        }.padding().background(.regularMaterial)
    }

    private func scoreButton(_ name: String, side: String) -> some View {
        Button {
            guard let value = Int64(points.trimmingCharacters(in: .whitespacesAndNewlines)) else {
                store.errorMessage = String(localized: "Enter a valid score.")
                return
            }
            if store.send("score", entity: matchID, values: ["side": side, "points": value]) != nil {
                points = ""
                scoreFocused = false
            }
        } label: {
            Text(name).multilineTextAlignment(.center).frame(maxWidth: .infinity, minHeight: 32)
                .fixedSize(horizontal: false, vertical: true)
        }.buttonStyle(.borderedProminent).disabled(points.isEmpty).accessibilityIdentifier("addScore\(side)")
    }
}

struct EditScoreView: View {
    let match: SeriesMatch
    let row: ScoreRow
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss
    @State private var points: String
    @State private var side: String
    @State private var confirmDelete = false

    init(match: SeriesMatch, row: ScoreRow) {
        self.match = match
        self.row = row
        _points = State(initialValue: String(row.points))
        _side = State(initialValue: row.side)
    }

    var body: some View {
        NavigationStack {
            Form {
                Picker("Team", selection: $side) {
                    Text(match.a.name).tag("A")
                    Text(match.b.name).tag("B")
                }
                TextField("Score", text: $points).keyboardType(.numberPad).accessibilityIdentifier("editScoreValue")
                Button(role: .destructive) { confirmDelete = true } label: { Label("Delete Score", systemImage: "trash") }
            }.navigationTitle("Edit Score").navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Save") {
                            guard let value = Int64(points) else { return }
                            if store.send("edit", entity: match.id, values: ["rowId": row.id, "side": side, "points": value], deferNotice: true) != nil { dismiss() }
                        }.disabled(Int64(points).map { !(1...10000).contains($0) } ?? true)
                    }
                }
                .confirmationDialog("Delete this score?", isPresented: $confirmDelete) {
                    Button("Delete Score", role: .destructive) {
                        if store.send("reverse", entity: match.id, values: ["rowId": row.id], deferNotice: true) != nil { dismiss() }
                    }
                }
                .alert("Could not complete action", isPresented: Binding(get: { store.errorMessage != nil }, set: { if !$0 { store.errorMessage = nil } })) {
                    Button("OK", role: .cancel) { store.errorMessage = nil }
                } message: { Text(store.errorMessage ?? "") }
        }
    }
}
