import SwiftUI

struct SeriesSetupView: View {
    @EnvironmentObject private var store: AppStore
    @Binding var path: [Route]
    @State private var teamA = ""
    @State private var teamB = ""
    @State private var bestOf = 3
    @State private var target = "200"

    var body: some View {
        Form {
            Section("Teams") {
                TextField("Team A", text: $teamA).accessibilityIdentifier("teamA")
                TextField("Team B", text: $teamB).accessibilityIdentifier("teamB")
            }
            MatchSettings(bestOf: $bestOf, target: $target)
            Section {
                Button {
                    guard let targetValue = Int(target.trimmingCharacters(in: .whitespacesAndNewlines)) else {
                        store.errorMessage = String(localized: "Enter a valid target score.")
                        return
                    }
                    if let id = store.send("newSeries", values: ["names": [teamA, teamB], "bestOf": bestOf, "target": targetValue])?.openedId {
                        path.removeLast()
                        path.append(.match(id))
                    }
                } label: { Label("Start Series", systemImage: "play.fill") }
                    .disabled(teamA.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || teamB.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityIdentifier("createSeries")
            }
        }.navigationTitle("New Series")
    }
}

struct MatchSettings: View {
    @Binding var bestOf: Int
    @Binding var target: String
    var body: some View {
        Section("Match Settings") {
            Picker("Best of", selection: $bestOf) {
                ForEach(Array(stride(from: 1, through: 199, by: 2)), id: \.self) { Text("\($0)").tag($0) }
            }.accessibilityIdentifier("bestOf")
            TextField("Target score", text: $target).keyboardType(.numberPad)
                .accessibilityIdentifier("targetScore")
            Picker("Preset", selection: $target) {
                Text("200").tag("200")
                Text("300").tag("300")
                Text("500").tag("500")
                if !["200", "300", "500"].contains(target) { Text("Custom").tag(target) }
            }.pickerStyle(.segmented)
        }
    }
}

private struct TeamDraft: Identifiable {
    let id = UUID()
    var name: String
}

struct TournamentSetupView: View {
    @EnvironmentObject private var store: AppStore
    @Binding var path: [Route]
    @State private var name = ""
    @State private var mode = "roundRobin"
    @State private var bestOf = 3
    @State private var target = "200"
    @State private var newTeam = ""
    @State private var teams: [TeamDraft] = []

    var body: some View {
        Form {
            Section("Tournament") {
                TextField("Tournament name", text: $name).accessibilityIdentifier("tournamentName")
                Picker("Format", selection: $mode) {
                    Text("Round Robin").tag("roundRobin")
                    Text("Elimination").tag("elimination")
                }.pickerStyle(.segmented).accessibilityIdentifier("tournamentFormat")
            }
            MatchSettings(bestOf: $bestOf, target: $target)
            Section("Teams (\(teams.count)/32)") {
                ForEach($teams) { $team in
                    TextField("Team name", text: $team.name)
                }.onDelete { teams.remove(atOffsets: $0) }
                if teams.count < 32 {
                    HStack {
                        TextField("Team name", text: $newTeam).onSubmit(addTeam).accessibilityIdentifier("newTeam")
                        Button(action: addTeam) { Image(systemName: "plus.circle.fill").font(.title2) }
                            .buttonStyle(.borderless).accessibilityLabel("Add Team").accessibilityIdentifier("addTeam")
                            .disabled(newTeam.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            }
            Section {
                Button {
                    guard let targetValue = Int(target.trimmingCharacters(in: .whitespacesAndNewlines)) else {
                        store.errorMessage = String(localized: "Enter a valid target score.")
                        return
                    }
                    if let id = store.send("newTournament", values: ["name": name, "names": teams.map(\.name), "mode": mode,
                                                                     "bestOf": bestOf, "target": targetValue])?.openedId {
                        path.removeLast()
                        path.append(.tournament(id))
                    }
                } label: { Label("Start Tournament", systemImage: "play.fill") }
                    .disabled(teams.count < 2 || name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .accessibilityIdentifier("createTournament")
            }
        }.navigationTitle("New Tournament")
    }

    private func addTeam() {
        let cleaned = newTeam.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalized = cleaned.lowercased().split(whereSeparator: \.isWhitespace).joined(separator: " ")
        guard !cleaned.isEmpty, cleaned.count <= 80, teams.count < 32 else { return }
        guard !teams.contains(where: { $0.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            .split(whereSeparator: \.isWhitespace).joined(separator: " ") == normalized }) else {
            store.errorMessage = String(localized: "Team names must be different.")
            return
        }
        teams.append(TeamDraft(name: cleaned))
        newTeam = ""
    }
}
