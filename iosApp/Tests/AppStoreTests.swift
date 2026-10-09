import XCTest
@testable import Apuntes

@MainActor
final class AppStoreTests: XCTestCase {
    private func directory() -> URL {
        FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
    }

    func testSeriesPersistsAcrossRelaunchWithScoreEditsAndUndo() throws {
        let folder = directory()
        defer { try? FileManager.default.removeItem(at: folder) }
        let store = AppStore(directory: folder)
        let result = try XCTUnwrap(store.send("newSeries", values: ["names": ["Home", "Away"], "bestOf": 3, "target": 200]))
        let id = try XCTUnwrap(result.openedId)
        XCTAssertNotNil(store.send("score", entity: id, values: ["points": 40, "side": "A"]))
        let row = try XCTUnwrap(store.match(id)?.currentGame?.visibleRows.first)
        XCTAssertNotNil(store.send("edit", entity: id, values: ["points": 75, "side": "B", "rowId": row.id]))
        let restored = AppStore(directory: folder)
        XCTAssertNil(restored.loadError)
        XCTAssertEqual(restored.match(id)?.total("B"), 75)
        XCTAssertEqual(restored.savedItems(active: true).count, 1)
        XCTAssertNotNil(restored.send("undo", entity: id))
        XCTAssertEqual(restored.match(id)?.total("B"), 0)
        XCTAssertEqual(restored.match(id)?.currentGame?.events.count, 3)
    }

    func testSaveFailureDoesNotPublishUncommittedScore() throws {
        let parent = directory()
        defer { try? FileManager.default.removeItem(at: parent) }
        try FileManager.default.createDirectory(at: parent, withIntermediateDirectories: true)
        let fileAsDirectory = parent.appendingPathComponent("blocked")
        try Data("not a directory".utf8).write(to: fileAsDirectory)
        let store = AppStore(directory: fileAsDirectory)
        XCTAssertNil(store.send("newSeries", values: ["names": ["Home", "Away"]]))
        XCTAssertTrue(store.snapshot.matches.isEmpty)
        XCTAssertEqual(store.snapshot.revision, 0)
        XCTAssertNotNil(store.errorMessage)
    }

    func testCorruptSaveIsNeverOverwritten() throws {
        let folder = directory()
        defer { try? FileManager.default.removeItem(at: folder) }
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        let url = folder.appendingPathComponent("state.json")
        let corrupt = Data("invalid-json".utf8)
        try corrupt.write(to: url)
        let store = AppStore(directory: folder)
        XCTAssertNotNil(store.loadError)
        XCTAssertNil(store.send("newSeries", values: ["names": ["Home", "Away"]]))
        XCTAssertEqual(try Data(contentsOf: url), corrupt)
    }

    func testCompletedSeriesLeavesRecoveryAndAppearsInHistory() throws {
        let folder = directory()
        defer { try? FileManager.default.removeItem(at: folder) }
        let store = AppStore(directory: folder)
        let id = try XCTUnwrap(store.send("newSeries", values: ["names": ["Home", "Away"], "bestOf": 1])?.openedId)
        XCTAssertNotNil(store.send("score", entity: id, values: ["points": 200, "side": "A"]))
        XCTAssertTrue(store.savedItems(active: true).isEmpty)
        XCTAssertEqual(store.savedItems(active: false).count, 1)
        let restored = AppStore(directory: folder)
        XCTAssertEqual(restored.match(id)?.winnerName, "Home")
        XCTAssertNil(restored.send("score", entity: id, values: ["points": 1, "side": "B"]))
    }

    func testTournamentRoundAndByesPersist() throws {
        let folder = directory()
        defer { try? FileManager.default.removeItem(at: folder) }
        let store = AppStore(directory: folder)
        let id = try XCTUnwrap(store.send("newTournament", values: ["name": "Friday", "names": ["A", "B", "C", "D", "E"],
                                                                  "mode": "elimination", "bestOf": 1])?.openedId)
        let matches = store.matches(in: id)
        for match in matches {
            XCTAssertNotNil(store.send("openMatch", entity: match.id))
            XCTAssertNotNil(store.send("score", entity: match.id, values: ["points": 200]))
        }
        let restored = AppStore(directory: folder)
        XCTAssertEqual(restored.tournament(id)?.round, 2)
        XCTAssertEqual(restored.tournament(id)?.byes.count, 2)
        XCTAssertEqual(restored.matches(in: id).count, 3)
    }
}
