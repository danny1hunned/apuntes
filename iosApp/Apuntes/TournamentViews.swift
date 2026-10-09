import SwiftUI

struct TournamentView: View {
    let tournamentID: String
    @EnvironmentObject private var store: AppStore

    var body: some View {
        Group {
            if let tournament = store.tournament(tournamentID) {
                let matches = store.matches(in: tournamentID)
                List {
                    Section {
                        LabeledContent("Format", value: tournament.mode == "roundRobin" ? String(localized: "Round Robin") : String(localized: "Elimination"))
                        LabeledContent("Status") { Text(LocalizedStringKey(tournament.status.capitalized)) }
                        Text("Best of \(tournament.bestOf) · Target \(tournament.target)").foregroundStyle(.secondary)
                        if let champion = tournament.championName {
                            Label("Champion: \(champion)", systemImage: "trophy.fill").foregroundStyle(.primary).font(.headline)
                        }
                        NavigationLink(value: Route.standings(tournamentID)) { Label("Standings", systemImage: "list.number") }
                            .accessibilityIdentifier("standings")
                    }
                    ForEach(Array(Set(matches.map(\.round))).sorted(), id: \.self) { round in
                        Section(roundTitle(round, tournament: tournament)) {
                            ForEach(matches.filter { $0.round == round }) { match in
                                NavigationLink(value: match.isFinished ? Route.matchHistory(match.id) : Route.match(match.id)) {
                                    VStack(alignment: .leading, spacing: 6) {
                                        Text(match.title).font(.headline).fixedSize(horizontal: false, vertical: true)
                                        HStack {
                                            Text(LocalizedStringKey(match.status.capitalized))
                                            Spacer()
                                            Text("\(match.wins("A")) - \(match.wins("B"))").monospacedDigit()
                                        }.font(.subheadline).foregroundStyle(.secondary)
                                        if let winner = match.winnerName { Text("Winner: \(winner)").font(.caption) }
                                    }.padding(.vertical, 4)
                                }.accessibilityIdentifier("match-\(match.round)-\(match.position)")
                            }
                            ForEach(tournament.byes.filter { $0.round == round }.map(\.participantId), id: \.self) { id in
                                if let participant = tournament.participants.first(where: { $0.id == id }) {
                                    Label("\(participant.name) · Bye", systemImage: "arrow.right").foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                }.navigationTitle(tournament.name).navigationBarTitleDisplayMode(.inline)
            } else { Text("Tournament not found") }
        }
    }

    private func roundTitle(_ round: Int, tournament: Tournament) -> String {
        if tournament.mode == "roundRobin" {
            return round == 2 ? String(localized: "Championship") : String(localized: "Round Robin")
        }
        return String(localized: "Round \(round)")
    }
}

struct StandingsView: View {
    let tournamentID: String
    @EnvironmentObject private var store: AppStore

    var body: some View {
        Group {
            if let tournament = store.tournament(tournamentID) {
                List {
                    if let champion = tournament.championName {
                        Section { Label("Champion: \(champion)", systemImage: "trophy.fill").font(.headline) }
                    }
                    ForEach(Array(tournament.standings.enumerated()), id: \.element.id) { index, standing in
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(alignment: .top, spacing: 12) {
                                if let place = standing.placement, place <= 3 {
                                    Image(["GoldMedal", "SilverMedal", "BronzeMedal"][place - 1])
                                        .resizable().scaledToFit().frame(width: 32, height: 40).accessibilityHidden(true)
                                } else {
                                    Text("\(index + 1)").font(.headline.monospacedDigit()).frame(width: 32)
                                }
                                Text(standing.participant.name).font(.headline).fixedSize(horizontal: false, vertical: true)
                                Spacer(minLength: 0)
                            }
                            LazyVGrid(columns: [GridItem(.adaptive(minimum: 100), alignment: .leading)], alignment: .leading, spacing: 8) {
                                Text("W \(standing.wins)")
                                Text("L \(standing.losses)")
                                Text("Played \(standing.wins + standing.losses)")
                                Text("\(standing.winRate)%")
                                Text("Pts \(standing.wins * 3)")
                                Text("Against \(standing.pointsAgainst)")
                                Text("Diff \(standing.difference)")
                            }.font(.caption).foregroundStyle(.secondary)
                        }.padding(.vertical, 8)
                    }
                }
            } else { Text("Tournament not found") }
        }.navigationTitle("Standings").navigationBarTitleDisplayMode(.inline)
    }

}
