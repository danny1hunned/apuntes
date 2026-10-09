import Foundation

struct AppSnapshot: Codable {
    var schemaVersion = 1
    var revision: Int64 = 0
    var matches: [SeriesMatch] = []
    var tournaments: [Tournament] = []
}

struct Participant: Codable, Identifiable {
    let id: String
    let name: String
    let seed: Int
}

struct ScoreRow: Codable, Identifiable {
    let id: String
    let side: String
    let points: Int64
    let reversed: Bool
}

struct ScoreEvent: Codable, Identifiable {
    let id: String
    let kind: String
    let side: String
    let points: Int64
    let rowId: String
    let createdAt: String
}

struct ScoredGame: Codable, Identifiable {
    let number: Int
    let rows: [ScoreRow]
    let events: [ScoreEvent]
    let winner: String?
    var id: Int { number }
    var visibleRows: [ScoreRow] { rows.filter { !$0.reversed } }
    func score(_ side: String) -> Int64 {
        visibleRows.filter { $0.side == side }.reduce(0) { $0 + $1.points }
    }
}

struct SeriesMatch: Codable, Identifiable {
    let id: String
    let tournamentId: String?
    let a: Participant
    let b: Participant
    let bestOf: Int
    let target: Int
    let kind: String
    let round: Int
    let position: Int
    let status: String
    let games: [ScoredGame]
    let winner: String?
    let createdAt: String
    let updatedAt: String
    var isActive: Bool { status == "active" }
    var isFinished: Bool { status == "completed" || status == "abandoned" }
    var title: String { "\(a.name) vs \(b.name)" }
    var currentGame: ScoredGame? { games.last }
    var winnerName: String? { winner.map { $0 == "A" ? a.name : b.name } }
    func wins(_ side: String) -> Int { games.filter { $0.winner == side }.count }
    func total(_ side: String) -> Int64 { games.reduce(0) { $0 + $1.score(side) } }
}

struct Bye: Codable {
    let participantId: String
    let round: Int
}

struct Standing: Codable, Identifiable {
    let participant: Participant
    let wins: Int
    let losses: Int
    let pointsFor: Int64
    let pointsAgainst: Int64
    let placement: Int?
    var id: String { participant.id }
    var winRate: Int { wins + losses == 0 ? 0 : wins * 100 / (wins + losses) }
    var difference: Int64 { pointsFor - pointsAgainst }
}

struct Tournament: Codable, Identifiable {
    let id: String
    let name: String
    let mode: String
    let bestOf: Int
    let target: Int
    let participants: [Participant]
    let status: String
    let round: Int
    let championId: String?
    let runnerUpId: String?
    let thirdId: String?
    let byes: [Bye]
    let standings: [Standing]
    let createdAt: String
    let updatedAt: String
    var championName: String? { participants.first { $0.id == championId }?.name }
}

struct GameNotice: Decodable, Identifiable {
    let kind: String
    let winnerName: String
    let scoreA: Int64
    let scoreB: Int64
    var id: String { "\(kind)-\(winnerName)-\(scoreA)-\(scoreB)" }
}

struct EngineResult: Decodable {
    let state: AppSnapshot?
    let error: String?
    let notice: GameNotice?
    let openedId: String?
}

enum Route: Hashable {
    case seriesSetup, tournamentSetup, recovery, history
    case match(String), tournament(String), standings(String), matchHistory(String)
}

struct SavedItem: Identifiable {
    let id: String
    let title: String
    let status: String
    let updatedAt: String
    let route: Route
    let tournament: Bool
}
