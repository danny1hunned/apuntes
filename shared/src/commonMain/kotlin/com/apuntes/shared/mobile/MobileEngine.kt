package com.apuntes.shared.mobile

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** JSON boundary keeps Swift persistence and UI independent of Kotlin collection bridging. */
class MobileEngine {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun emptyState(): String = json.encodeToString(MobileState())

    fun restore(stateJson: String): String = respond {
        MobileResult(state = decodeState(stateJson))
    }

    fun apply(stateJson: String, commandJson: String): String = respond {
        val state = decodeState(stateJson)
        val command = json.decodeFromString<MobileCommand>(commandJson)
        require(command.revision == state.revision) { "This game changed. Please try again." }
        require(command.id.isNotBlank() && command.timestamp.isNotBlank()) { "Invalid command." }
        var notice: MobileNotice? = null
        var openedId: String? = null
        when (command.action) {
            "newSeries" -> {
                validateSetup(command, 2, 2)
                require(state.matches.none { it.id == command.id }) { "Series already exists." }
                val players = participants(command)
                state.matches += MobileMatch(
                    id = command.id, a = players[0], b = players[1], bestOf = command.bestOf,
                    target = command.target, status = "active", games = mutableListOf(MobileGame(1)),
                    createdAt = command.timestamp
                )
                openedId = command.id
            }
            "newTournament" -> {
                validateSetup(command, 2, 32)
                require(command.mode in listOf("roundRobin", "elimination")) { "Unknown tournament format." }
                require(state.tournaments.none { it.id == command.id }) { "Tournament already exists." }
                val tournament = MobileTournament(
                    id = command.id, name = validName(command.name), mode = command.mode,
                    bestOf = command.bestOf, target = command.target,
                    participants = participants(command), createdAt = command.timestamp
                )
                state.tournaments += tournament
                if (tournament.mode == "roundRobin") {
                    var position = 1
                    tournament.participants.forEachIndexed { i, a ->
                        tournament.participants.drop(i + 1).forEach { b ->
                            schedule(state, tournament, a.id, b.id, "roundRobin", 1, position++, command.timestamp)
                        }
                    }
                } else if (tournament.participants.size == 3) {
                    schedule(state, tournament, tournament.participants[0].id, tournament.participants[1].id,
                        "elimination", 1, 1, command.timestamp)
                } else {
                    eliminationRound(state, tournament, tournament.participants, 1, command.timestamp)
                }
                updateStandings(state, tournament)
                openedId = command.id
            }
            "openMatch" -> {
                val match = match(state, command.entityId)
                require(match.status in listOf("draft", "active", "completed", "abandoned")) { "Invalid match." }
                if (match.status == "draft") {
                    require(activeTournament(state, match).status == "active") { "Tournament is not active." }
                    match.status = "active"
                    match.games += MobileGame(1)
                    match.updatedAt = command.timestamp
                }
                openedId = match.id
            }
            "score", "edit", "reverse", "undo" -> {
                val match = match(state, command.entityId)
                require(match.status == "active") { "This series is no longer active." }
                if (match.tournamentId != null) activeTournament(state, match)
                val game = match.games.lastOrNull() ?: error("No active game.")
                require(game.winner == null) { "This game has already finished." }
                require(game.events.none { it.id == command.id }) { "Score already recorded." }
                val row = when (command.action) {
                    "score" -> {
                        validateScore(command)
                        MobileScore(command.id, command.side, command.points).also { game.rows += it }
                    }
                    "edit" -> {
                        validateScore(command)
                        liveRow(game, command.rowId).also { it.side = command.side; it.points = command.points }
                    }
                    "reverse" -> liveRow(game, command.rowId).also { it.reversed = true }
                    else -> (game.rows.lastOrNull { !it.reversed } ?: error("No score to undo.")).also { it.reversed = true }
                }
                game.events += MobileScoreEvent(command.id, command.action, row.side, row.points, row.id, command.timestamp)
                match.updatedAt = command.timestamp
                val a = game.score("A")
                val b = game.score("B")
                val winner = when {
                    a >= match.target && a > b -> "A"
                    b >= match.target && b > a -> "B"
                    else -> null
                }
                if (winner != null) {
                    game.winner = winner
                    val finished = match.wins(winner) >= match.bestOf / 2 + 1
                    if (finished) {
                        match.winner = winner
                        match.status = "completed"
                    } else {
                        match.games += MobileGame(game.number + 1)
                    }
                    notice = MobileNotice(
                        if (finished) "series" else "game", if (winner == "A") match.a.name else match.b.name,
                        if (finished) match.wins("A").toLong() else a,
                        if (finished) match.wins("B").toLong() else b
                    )
                }
                match.tournamentId?.let { tournamentId ->
                    val tournament = state.tournaments.first { it.id == tournamentId }
                    tournament.updatedAt = command.timestamp
                    advance(state, tournament, command.timestamp)
                    updateStandings(state, tournament)
                    if (tournament.status == "completed") notice = notice?.copy(kind = "champion")
                }
            }
            "abandonSeries" -> {
                val match = match(state, command.entityId)
                require(match.tournamentId == null) { "Abandon the tournament instead." }
                require(match.status == "active") { "Only active series can be abandoned." }
                match.status = "abandoned"
                match.updatedAt = command.timestamp
            }
            "abandonTournament" -> {
                val tournament = state.tournaments.firstOrNull { it.id == command.entityId } ?: error("Tournament not found.")
                require(tournament.status == "active") { "Only active tournaments can be abandoned." }
                tournament.status = "abandoned"
                tournament.updatedAt = command.timestamp
                state.matches.filter { it.tournamentId == tournament.id && it.status in listOf("active", "draft") }.forEach {
                    it.status = "abandoned"
                    it.updatedAt = command.timestamp
                }
            }
            else -> error("Unknown action.")
        }
        state.revision++
        MobileResult(state, notice = notice, openedId = openedId)
    }

    private fun decodeState(value: String): MobileState {
        val state = json.decodeFromString<MobileState>(value)
        require(state.schemaVersion == 1) { "This save needs a newer version of Apuntes." }
        require(state.revision >= 0) { "Invalid saved revision." }
        require(state.matches.map { it.id }.distinct().size == state.matches.size) { "Duplicate saved matches." }
        require(state.tournaments.map { it.id }.distinct().size == state.tournaments.size) { "Duplicate saved tournaments." }
        return state
    }

    private inline fun respond(block: () -> MobileResult): String = try {
        json.encodeToString(block())
    } catch (error: Exception) {
        json.encodeToString(MobileResult(error = error.message ?: "Unable to update the game."))
    }

    private fun validName(value: String): String {
        val name = value.trim()
        require(name.isNotEmpty() && name.length <= 80) { "Names must contain 1 to 80 characters." }
        return name
    }

    private fun validateSetup(command: MobileCommand, min: Int, max: Int) {
        require(command.names.size in min..max) { "Choose $min to $max teams." }
        val names = command.names.map { validName(it).lowercase().replace(Regex("\\s+"), " ") }
        require(names.distinct().size == names.size) { "Team names must be different." }
        require(command.bestOf in 1..199 && command.bestOf % 2 == 1) { "Best-of must be odd, from 1 to 199." }
        require(command.target in 1..100_000) { "Target score must be from 1 to 100000." }
    }

    private fun validateScore(command: MobileCommand) {
        require(command.side == "A" || command.side == "B") { "Choose a team." }
        require(command.points in 1..10_000) { "Score must be from 1 to 10000." }
    }

    private fun participants(command: MobileCommand): List<MobileParticipant> =
        command.names.mapIndexed { index, name -> MobileParticipant("${command.id}:team:${index + 1}", name.trim(), index + 1) }

    private fun match(state: MobileState, id: String): MobileMatch = state.matches.firstOrNull { it.id == id } ?: error("Match not found.")
    private fun liveRow(game: MobileGame, id: String): MobileScore =
        game.rows.firstOrNull { it.id == id && !it.reversed } ?: error("This score is no longer editable.")

    private fun activeTournament(state: MobileState, match: MobileMatch): MobileTournament {
        val tournament = state.tournaments.firstOrNull { it.id == match.tournamentId } ?: error("Tournament not found.")
        require(tournament.status == "active") { "This tournament is no longer active." }
        return tournament
    }

    private fun schedule(state: MobileState, tournament: MobileTournament, a: String, b: String,
                         kind: String, round: Int, position: Int, now: String) {
        val id = "${tournament.id}:$kind:$round:$position"
        if (state.matches.any { it.id == id }) return
        require(a != b) { "A team cannot play itself." }
        state.matches += MobileMatch(id, tournament.id, tournament.participants.first { it.id == a },
            tournament.participants.first { it.id == b }, tournament.bestOf, tournament.target,
            kind, round, position, createdAt = now)
    }

    private fun eliminationRound(state: MobileState, tournament: MobileTournament,
                                 players: List<MobileParticipant>, round: Int, now: String) {
        players.chunked(2).forEachIndexed { index, pair ->
            if (pair.size == 1) tournament.byes += MobileBye(pair[0].id, round)
            else schedule(state, tournament, pair[0].id, pair[1].id, "elimination", round, index + 1, now)
        }
        tournament.round = round
    }

    private fun advance(state: MobileState, tournament: MobileTournament, now: String) {
        if (tournament.status != "active") return
        val matches = state.matches.filter { it.tournamentId == tournament.id }
        if (tournament.mode == "roundRobin") {
            val prelim = matches.filter { it.kind == "roundRobin" }
            if (prelim.any { it.status != "completed" }) return
            val final = matches.firstOrNull { it.kind == "championship" }
            if (final == null) {
                val ranked = rank(tournament, prelim)
                schedule(state, tournament, ranked[0].participant.id, ranked[1].participant.id, "championship", 2, 1, now)
                tournament.round = 2
            } else if (final.status == "completed") complete(tournament, final)
        } else if (tournament.participants.size == 3) {
            val first = matches.first { it.round == 1 }
            if (first.status != "completed") return
            val second = matches.firstOrNull { it.round == 2 }
            if (second == null) {
                schedule(state, tournament, tournament.participants[2].id, first.loserId()!!, "elimination", 2, 1, now)
                tournament.round = 2
                return
            }
            if (second.status != "completed") return
            val final = matches.firstOrNull { it.round == 3 }
            if (final == null) {
                schedule(state, tournament, first.winnerId()!!, second.winnerId()!!, "elimination", 3, 1, now)
                tournament.round = 3
            } else if (final.status == "completed") {
                tournament.thirdId = second.loserId()
                complete(tournament, final)
            }
        } else {
            val round = matches.maxOf { it.round }
            val current = matches.filter { it.round == round }
            if (current.any { it.status != "completed" }) return
            val advancing = current.mapNotNull { it.winnerId() } + tournament.byes.filter { it.round == round }.map { it.participantId }
            if (advancing.size == 1) complete(tournament, current.single())
            else eliminationRound(state, tournament, tournament.participants.filter { it.id in advancing }, round + 1, now)
        }
    }

    private fun complete(tournament: MobileTournament, final: MobileMatch) {
        tournament.status = "completed"
        tournament.championId = final.winnerId()
        tournament.runnerUpId = final.loserId()
    }

    private fun rank(tournament: MobileTournament, matches: List<MobileMatch>): List<MobileStanding> {
        val rows = tournament.participants.associate { it.id to MobileStanding(it) }
        matches.filter { it.status == "completed" }.forEach { match ->
            val a = rows.getValue(match.a.id)
            val b = rows.getValue(match.b.id)
            a.pointsFor += match.total("A"); a.pointsAgainst += match.total("B")
            b.pointsFor += match.total("B"); b.pointsAgainst += match.total("A")
            if (match.winner == "A") { a.wins++; b.losses++ } else { b.wins++; a.losses++ }
        }
        val fallback = compareByDescending<MobileStanding> { it.pointsFor - it.pointsAgainst }
            .thenByDescending { it.pointsFor }.thenBy { it.participant.seed }
        return rows.values.groupBy { it.wins }.entries.sortedByDescending { it.key }.flatMap { (_, tied) ->
            if (tied.size == 2) tied.sortedWith { a, b ->
                val headToHead = matches.firstOrNull { it.status == "completed" &&
                    setOf(it.a.id, it.b.id) == setOf(a.participant.id, b.participant.id) }
                when (headToHead?.winnerId()) {
                    a.participant.id -> -1
                    b.participant.id -> 1
                    else -> fallback.compare(a, b)
                }
            } else tied.sortedWith(fallback)
        }
    }

    private fun updateStandings(state: MobileState, tournament: MobileTournament) {
        val matches = state.matches.filter { it.tournamentId == tournament.id }
        val ranked = rank(tournament, if (tournament.mode == "roundRobin")
            matches.filter { it.kind == "roundRobin" } else matches)
        ranked.forEach { row ->
            row.placement = when (row.participant.id) {
                tournament.championId -> 1
                tournament.runnerUpId -> 2
                tournament.thirdId -> 3
                else -> null
            }
        }
        tournament.standings = if (tournament.mode == "elimination") {
            ranked.sortedWith(compareBy<MobileStanding> { it.placement ?: Int.MAX_VALUE }
                .thenByDescending { it.wins }.thenBy { it.participant.name.lowercase() })
        } else ranked
    }
}
