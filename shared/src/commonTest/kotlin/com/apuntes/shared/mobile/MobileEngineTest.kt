package com.apuntes.shared.mobile

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MobileEngineTest {
    private val engine = MobileEngine()
    private var state = MobileState()
    private var serial = 0

    private fun command(action: String, entity: String = "", names: List<String> = emptyList(),
                        mode: String = "roundRobin", bestOf: Int = 1, target: Int = 200,
                        side: String = "A", points: Long = 0, row: String = ""): MobileResult {
        val command = MobileCommand("id-${++serial}", state.revision, action, "2026-10-08T12:00:00Z",
            entityId = entity, name = "Tournament", names = names, mode = mode, bestOf = bestOf,
            target = target, side = side, points = points, rowId = row)
        val result = Json.decodeFromString<MobileResult>(engine.apply(Json.encodeToString(state), Json.encodeToString(command)))
        result.state?.let { state = it }
        return result
    }

    private fun ok(result: MobileResult): MobileResult {
        assertNull(result.error, result.error)
        assertNotNull(result.state)
        return result
    }

    private fun finish(id: String, side: String = "A") {
        ok(command("openMatch", id))
        ok(command("score", id, side = side, points = 200))
    }

    @Test fun seriesAwardsGamesExactlyOnceAndKeepsHistory() {
        val id = ok(command("newSeries", names = listOf("A", "B"), bestOf = 3)).openedId!!
        assertEquals("game", ok(command("score", id, points = 200)).notice?.kind)
        assertEquals(2, state.matches.single().games.size)
        assertEquals(0, state.matches.single().games.last().rows.size)
        val notice = ok(command("score", id, points = 250)).notice!!
        assertEquals("series", notice.kind)
        assertEquals(2L, notice.scoreA)
        assertEquals(0L, notice.scoreB)
        assertEquals("completed", state.matches.single().status)
        assertEquals(450, state.matches.single().total("A"))
        assertNotNull(command("score", id, points = 1).error)
        assertEquals(2, state.matches.single().wins("A"))
    }

    @Test fun scoreEditingMovingDeletingAndUndoPreserveAudit() {
        val id = ok(command("newSeries", names = listOf("A", "B"))).openedId!!
        ok(command("score", id, points = 30))
        val row = state.matches.single().games.single().rows.single().id
        ok(command("edit", id, side = "B", points = 60, row = row))
        assertEquals(60, state.matches.single().total("B"))
        ok(command("score", id, points = 20))
        ok(command("undo", id))
        assertEquals(0, state.matches.single().total("A"))
        ok(command("reverse", id, row = row))
        assertEquals(0, state.matches.single().total("B"))
        assertEquals(5, state.matches.single().games.single().events.size)
        assertNotNull(command("edit", id, points = 10, row = row).error)
    }

    @Test fun rejectsStaleCommandsWithoutChangingState() {
        val id = ok(command("newSeries", names = listOf("A", "B"))).openedId!!
        val stale = MobileCommand("duplicate", 0, "score", "now", entityId = id, points = 10)
        val result = Json.decodeFromString<MobileResult>(engine.apply(Json.encodeToString(state), Json.encodeToString(stale)))
        assertNotNull(result.error)
        assertNull(result.state)
    }

    @Test fun validatesNamesAndNumericLimits() {
        assertNotNull(command("newSeries", names = listOf(" a  b ", "A B")).error)
        assertNotNull(command("newSeries", names = listOf("A", "B"), bestOf = 2).error)
        assertNotNull(command("newSeries", names = listOf("A", "B"), target = 0).error)
        val id = ok(command("newSeries", names = listOf("A", "B"))).openedId!!
        assertNotNull(command("score", id, points = 0).error)
        assertNotNull(command("score", id, points = 10001).error)
        assertNotNull(command("score", id, side = "C", points = 1).error)
        assertEquals(0, state.matches.single().total("A"))
    }

    @Test fun roundRobinSchedulesEveryPairAndSeparateChampionship() {
        ok(command("newTournament", names = listOf("A", "B", "C", "D")))
        assertEquals(6, state.matches.size)
        state.matches.map { it.id }.forEach { finish(it) }
        assertEquals(7, state.matches.size)
        val final = state.matches.last()
        assertEquals("championship", final.kind)
        assertEquals("A", final.a.name)
        assertEquals("B", final.b.name)
        ok(command("openMatch", final.id))
        val notice = ok(command("score", final.id, side = "B", points = 200)).notice!!
        assertEquals("champion", notice.kind)
        assertEquals("B", notice.winnerName)
        assertEquals("completed", state.tournaments.single().status)
        assertEquals(final.b.id, state.tournaments.single().championId)
        assertEquals("A", state.tournaments.single().standings.first().participant.name)
        assertEquals(1, state.tournaments.single().standings.first { it.participant.name == "B" }.placement)
    }

    @Test fun threeTeamEliminationGivesFirstLoserSecondChance() {
        ok(command("newTournament", names = listOf("A", "B", "C"), mode = "elimination"))
        finish(state.matches.single().id)
        assertEquals("C", state.matches.last().a.name)
        assertEquals("B", state.matches.last().b.name)
        finish(state.matches.last().id, "B")
        assertEquals("A", state.matches.last().a.name)
        assertEquals("B", state.matches.last().b.name)
        finish(state.matches.last().id)
        val tournament = state.tournaments.single()
        assertEquals("completed", tournament.status)
        assertEquals(tournament.participants[2].id, tournament.thirdId)
        assertEquals(3, state.matches.size)
    }

    @Test fun oddEliminationByesDoNotCountAsWinsOrMatches() {
        ok(command("newTournament", names = listOf("A", "B", "C", "D", "E"), mode = "elimination"))
        assertEquals(2, state.matches.size)
        assertEquals(1, state.tournaments.single().byes.size)
        while (state.tournaments.single().status == "active") {
            finish(state.matches.first { it.status != "completed" }.id)
        }
        assertEquals(4, state.matches.size)
        val e = state.tournaments.single().standings.first { it.participant.name == "E" }
        assertEquals(0, e.wins)
        assertEquals(1, e.losses)
    }

    @Test fun everySupportedEliminationSizeCompletesWithoutDuplicatePairings() {
        for (count in 2..32) {
            state = MobileState()
            ok(command("newTournament", names = (1..count).map { "Team $it" }, mode = "elimination"))
            var played = 0
            while (state.tournaments.single().status == "active") {
                assertTrue(played++ < 64)
                finish(state.matches.first { it.status != "completed" }.id)
            }
            assertEquals(if (count == 3) 3 else count - 1, state.matches.size)
            assertEquals(state.matches.size, state.matches.map { it.id }.distinct().size)
        }
    }

    @Test fun abandonmentRetainsScoresAndBlocksFurtherEdits() {
        val id = ok(command("newSeries", names = listOf("A", "B"))).openedId!!
        ok(command("score", id, points = 75))
        ok(command("abandonSeries", id))
        assertEquals(75, state.matches.single().total("A"))
        assertNotNull(command("score", id, points = 10).error)
        val tournamentId = ok(command("newTournament", names = listOf("A", "B", "C"))).openedId!!
        ok(command("abandonTournament", tournamentId))
        assertTrue(state.matches.filter { it.tournamentId == tournamentId }.all { it.status == "abandoned" })
    }

    @Test fun restorationRoundTripsActiveAndCompletedGamesAndRejectsUnknownSchemas() {
        val id = ok(command("newSeries", names = listOf("A", "B"), bestOf = 3)).openedId!!
        ok(command("score", id, points = 200))
        ok(command("score", id, side = "B", points = 40))
        val restored = Json.decodeFromString<MobileResult>(engine.restore(Json.encodeToString(state)))
        assertEquals(state, restored.state)
        assertNotNull(Json.decodeFromString<MobileResult>(engine.restore("{\"schemaVersion\":99}")).error)
        assertNotNull(Json.decodeFromString<MobileResult>(engine.restore("broken")).error)
    }
}
