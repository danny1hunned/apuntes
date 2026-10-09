import XCTest

final class ApuntesUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--ui-testing", "--reset-test-state", "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
    }

    private func fill(_ identifier: String, _ text: String) {
        let field = app.textFields[identifier]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText(text)
    }

    private func capture(_ name: String) {
        let screenshot = app.screenshot()
        XCTAssertGreaterThan(screenshot.pngRepresentation.count, 10000)
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testScoringUndoRecoveryAndScreenshots() {
        capture("Home")
        app.buttons["startSeries"].tap()
        fill("teamA", "Home")
        fill("teamB", "Away")
        app.swipeUp()
        app.buttons["createSeries"].tap()
        fill("roundScore", "40")
        app.buttons["addScoreA"].tap()
        XCTAssertEqual(app.staticTexts["scoreA"].label, "40")
        capture("Scoreboard")
        app.buttons["undoScore"].tap()
        XCTAssertEqual(app.staticTexts["scoreA"].label, "0")
        fill("roundScore", "75")
        app.buttons["addScoreB"].tap()
        app.terminate()
        app.launchArguments.removeAll { $0 == "--reset-test-state" }
        app.launch()
        app.buttons["resumeGames"].tap()
        app.buttons.containing(.staticText, identifier: "Home vs Away").firstMatch.tap()
        XCTAssertEqual(app.staticTexts["scoreB"].label, "75")
        capture("Recovered series")
    }

    func testTournamentSetupAndStandings() {
        app.buttons["startTournament"].tap()
        fill("tournamentName", "Friday Tournament")
        for name in ["Alpha", "Bravo", "Charlie"] {
            app.swipeUp()
            fill("newTeam", name)
            app.buttons["addTeam"].tap()
        }
        app.swipeUp()
        app.buttons["createTournament"].tap()
        XCTAssertTrue(app.buttons["match-1-1"].waitForExistence(timeout: 10))
        capture("Round Robin")
        app.buttons["standings"].tap()
        XCTAssertTrue(app.staticTexts["Alpha"].waitForExistence(timeout: 10))
        capture("Standings")
    }

    func testLongNamesAndLargeTextRemainUsable() {
        app.terminate()
        app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"]
        app.launch()
        app.buttons["startSeries"].tap()
        fill("teamA", "The Northside Championship Team")
        fill("teamB", "The Southside Championship Team")
        app.swipeUp()
        app.buttons["createSeries"].tap()
        capture("Large text scoreboard")
        let score = app.textFields["roundScore"]
        XCTAssertTrue(score.exists)
        XCTAssertTrue(score.isHittable)
    }
}
