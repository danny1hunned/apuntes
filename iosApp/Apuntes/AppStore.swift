import Foundation
import Combine
import sharedKit

@MainActor
final class AppStore: ObservableObject {
    @Published private(set) var snapshot = AppSnapshot()
    @Published private(set) var loadError: String?
    @Published var errorMessage: String?
    @Published var notice: GameNotice?
    private let engine = MobileEngine()
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()
    private let fileURL: URL
    private var stateJSON: String
    private var pendingNotice: GameNotice?

    init(directory: URL? = nil) {
        let root = directory ?? FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("Apuntes", isDirectory: true)
        fileURL = root.appendingPathComponent("state.json")
        stateJSON = engine.emptyState()
        reload()
    }

    func reload() {
        do {
            if FileManager.default.fileExists(atPath: fileURL.path) {
                let saved = try String(contentsOf: fileURL, encoding: .utf8)
                let result = try decode(engine.restore(stateJson: saved))
                guard let restored = result.state else { throw StoreError.message(NSLocalizedString(result.error ?? "Unable to open saved games.", comment: "")) }
                snapshot = restored
                stateJSON = saved
            }
            loadError = nil
        } catch {
            // Never replace an unreadable save with an empty state.
            loadError = error.localizedDescription
        }
    }

    @discardableResult
    func send(_ action: String, entity: String = "", values: [String: Any] = [:], deferNotice: Bool = false) -> EngineResult? {
        guard loadError == nil else { return nil }
        do {
            var command = values
            command["id"] = UUID().uuidString
            command["revision"] = snapshot.revision
            command["action"] = action
            command["timestamp"] = ISO8601DateFormatter().string(from: Date())
            command["entityId"] = entity
            let data = try JSONSerialization.data(withJSONObject: command)
            let result = try decode(engine.apply(stateJson: stateJSON, commandJson: String(decoding: data, as: UTF8.self)))
            guard let next = result.state else { throw StoreError.message(NSLocalizedString(result.error ?? "Unable to update the game.", comment: "")) }
            let encoded = try encoder.encode(next)
            try FileManager.default.createDirectory(at: fileURL.deletingLastPathComponent(), withIntermediateDirectories: true)
            try encoded.write(to: fileURL, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
            // Publish only after the complete scoring/progression transaction has reached disk.
            stateJSON = String(decoding: encoded, as: UTF8.self)
            snapshot = next
            if deferNotice { pendingNotice = result.notice }
            else { notice = result.notice }
            return result
        } catch {
            errorMessage = error.localizedDescription
            return nil
        }
    }

    func presentPendingNotice() {
        notice = pendingNotice
        pendingNotice = nil
    }

    func match(_ id: String) -> SeriesMatch? { snapshot.matches.first { $0.id == id } }
    func tournament(_ id: String) -> Tournament? { snapshot.tournaments.first { $0.id == id } }
    func matches(in tournamentID: String) -> [SeriesMatch] {
        snapshot.matches.filter { $0.tournamentId == tournamentID }
            .sorted { ($0.round, $0.position) < ($1.round, $1.position) }
    }

    func savedItems(active: Bool) -> [SavedItem] {
        let series = snapshot.matches.filter { $0.tournamentId == nil && ($0.status == "active") == active }
            .map { SavedItem(id: $0.id, title: $0.title, status: $0.status, updatedAt: $0.updatedAt,
                             route: active ? .match($0.id) : .matchHistory($0.id), tournament: false) }
        let tournaments = snapshot.tournaments.filter { ($0.status == "active") == active }
            .map { SavedItem(id: $0.id, title: $0.name, status: $0.status, updatedAt: $0.updatedAt,
                             route: .tournament($0.id), tournament: true) }
        return (series + tournaments).sorted { ($0.updatedAt, $0.id) > ($1.updatedAt, $1.id) }
    }

    private func decode(_ value: String) throws -> EngineResult {
        try decoder.decode(EngineResult.self, from: Data(value.utf8))
    }
}

enum StoreError: LocalizedError {
    case message(String)
    var errorDescription: String? { switch self { case .message(let value): return value } }
}
