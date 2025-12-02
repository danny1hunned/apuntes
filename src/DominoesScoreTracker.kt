import java.lang.IllegalArgumentException

// --- Data Structures ---

/**
 * Represents a player in the game.
 */
data class Player(val name: String)

/**
 * Represents a team, containing two players, scores, and games won in the series.
 * totalScore now tracks points for the current *game* in the series.
 */
data class Team(
    val name: String,
    val player1: Player,
    val player2: Player,
    var totalScore: Int = 0, // Score accumulated in the current game
    var gamesWon: Int = 0 // Games won in the overall series
) {
    override fun toString(): String {
        return "$name (${player1.name} & ${player2.name}) | Current Game Score: $totalScore | Series Wins: $gamesWon"
    }
}

// --- Main Game Logic ---

/**
 * Manages the state and flow of the dominoes series tracking application.
 */
class DominoesTracker {
    private lateinit var teamA: Team
    private lateinit var teamB: Team
    private var bestOf: Int = 0
    private var gameTargetScore: Int = 0 // New property for the score required to win a single game

    // Calculates the number of wins required to secure the series
    private val targetWins: Int
        get() = (bestOf / 2) + 1

    /**
     * Helper function to read user input, prompting until a non-blank value is received.
     */
    private fun readInput(prompt: String, defaultValue: String? = null): String {
        print("$prompt${if (defaultValue != null) " (Default: $defaultValue)" else ""}: ")
        val input = readLine()?.trim()
        return if (input.isNullOrBlank() && defaultValue != null) defaultValue else input ?: ""
    }

    /**
     * Sets up one team, including its name and the names of its two players.
     */
    private fun setupTeam(defaultName: String, playerPrefix: String): Team {
        val teamName = readInput("Enter Team Name", defaultName)

        println("Setting up players for $teamName...")
        val player1Name = readInput("Enter Name for Player 1 of $teamName", "${playerPrefix}1")
        val player2Name = readInput("Enter Name for Player 2 of $teamName", "${playerPrefix}2")

        val p1 = Player(player1Name)
        val p2 = Player(player2Name)

        return Team(teamName, p1, p2)
    }

    /**
     * Prompts the user to set the series format (e.g., Best-of-3, Best-of-5).
     */
    private fun setupSeries() {
        println("\n--- Series Setup (Overall Tournament) ---")
        while (bestOf <= 0 || bestOf % 2 == 0) {
            val input = readInput("Enter the 'Best-Out-Of' number (e.g., 3, 5, 7)", "3")
            try {
                bestOf = input.toInt()
                if (bestOf <= 0) {
                    println("The series must be a positive number.")
                } else if (bestOf % 2 == 0) {
                    println("The series must be an ODD number (e.g., Best of 3, Best of 5) to avoid a tie.")
                }
            } catch (e: NumberFormatException) {
                println("Invalid input. Please enter a valid odd number.")
            }
        }
        println("Series set: Best out of $bestOf. The first team to win $targetWins games wins the series!")
    }

    /**
     * Prompts the user to set the target score for winning a single game.
     */
    private fun setupGameScoreTarget() {
        println("\n--- Game Target Score Setup (Single Match) ---")
        // Define the valid target scores a user can choose from
        val validTargets = listOf(200, 300, 400, 500)

        while (gameTargetScore <= 0) {
            // Prompt the user, showing the available options
            val input = readInput("Enter the score needed to win a single game (${validTargets.joinToString(", ")})", "200")
            try {
                val score = input.toInt()
                if (validTargets.contains(score)) {
                    gameTargetScore = score
                    println("Game target score set to $gameTargetScore points. Reaching this score wins the current game.")
                } else {
                    println("Invalid score. Please choose from: ${validTargets.joinToString(", ")}.")
                }
            } catch (e: NumberFormatException) {
                println("Invalid input. Please enter a number.")
            }
        }
    }

    /**
     * Prompts for the winning team and the score they earned for the current round/hand.
     * Returns true if a team won the current game in this round, false otherwise.
     */
    private fun playGameRound(): Boolean {
        println("\n--- Round Score Input (Scoring Team Only) ---")

        var scoringTeam: Team? = null
        var nonScoringTeam: Team? = null

        // 1. Determine which team is scoring this round
        while (scoringTeam == null) {
            val prompt = "Which team is scoring this round? (Enter 'A' for ${teamA.name}, 'B' for ${teamB.name})"
            val input = readInput(prompt).uppercase()

            when (input) {
                "A" -> {
                    scoringTeam = teamA
                    nonScoringTeam = teamB
                }
                "B" -> {
                    scoringTeam = teamB
                    nonScoringTeam = teamA
                }
                else -> println("Invalid input. Please enter 'A' or 'B'.")
            }
        }

        // 2. Get the score earned (pips left on the losing side's hands)
        var roundScore = -1
        while (roundScore < 0) {
            val input = readInput("Enter points scored by ${scoringTeam!!.name} (Pips left on opponent's hand)")
            try {
                roundScore = input.toInt()
                if (roundScore < 0) throw IllegalArgumentException()
            } catch (e: Exception) {
                println("Invalid input. Please enter a non-negative number.")
            }
        }

        // 3. Update total scores (only the scoring team gets points)
        scoringTeam!!.totalScore += roundScore

        println("\n--- Round Summary ---")
        println("${scoringTeam.name} scored $roundScore points. New Total: ${scoringTeam.totalScore}/${gameTargetScore}")
        println("${nonScoringTeam!!.name} scored 0 points. New Total: ${nonScoringTeam.totalScore}/${gameTargetScore}")

        // Check for Game Winner (reaching the target score)
        // Since only one team scored, the game winner check is simplified.
        val teamAWonGame = teamA.totalScore >= gameTargetScore
        val teamBWonGame = teamB.totalScore >= gameTargetScore

        if (teamAWonGame || teamBWonGame) {
            // Determine the final game winner (who has the higher score after this round)
            val winner = if (teamA.totalScore > teamB.totalScore) {
                teamA
            } else if (teamB.totalScore > teamA.totalScore) {
                teamB
            } else {
                // If scores are exactly equal and both crossed, the win goes to the team that scored this round (or higher score this round)
                scoringTeam
            }

            // The winner must have reached the target score
            if (winner.totalScore >= gameTargetScore) {
                winner.gamesWon++
                val loserScore = if (winner == teamA) teamB.totalScore else teamA.totalScore
                println("\n*** GAME WINNER: ${winner.name}! Final Score: ${winner.totalScore} vs $loserScore ***")

                // Reset points for the start of the next game in the series
                teamA.totalScore = 0
                teamB.totalScore = 0
                println("Current game points reset to 0 to begin the next game in the series.")
                return true
            }
        }

        // If no game win, continue to the next round
        println("Game continues. Next round...")
        return false
    }

    /**
     * Displays the current status of the series.
     */
    private fun displayStatus() {
        println("\n=============================================")
        println("          Current Series Status")
        println("=============================================")
        println("Target Wins to win Series: $targetWins (Best of $bestOf)")
        println("Target Score to win Game: $gameTargetScore")
        println(teamA)
        println(teamB)
        println("=============================================")
    }

    /**
     * Initiates the game setup and main loop.
     */
    fun start() {
        println("=== Dominoes Series Score Tracker ===")

        // 1. Setup Teams
        println("\n--- Team Setup ---")
        teamA = setupTeam("Visitors", "PlayerV")
        teamB = setupTeam("Away", "PlayerA")

        // 2. Setup Series & Game Score Target
        setupSeries()
        setupGameScoreTarget() // New call to set the game score target

        displayStatus()

        // 3. Main Game Loop (Series Loop)
        // Continues until one team reaches the target number of wins
        while (teamA.gamesWon < targetWins && teamB.gamesWon < targetWins) {
            println("\n[ New Game Started - Game ${teamA.gamesWon + teamB.gamesWon + 1} ]")

            // Inner Game Loop (Round Loop)
            // Continues until one team wins the current game by reaching the target score
            var gameWon = false
            while (!gameWon) {
                gameWon = playGameRound() // Track points and check for game winner

                // Only display status if the game is still active
                if (!gameWon) {
                    displayStatus()
                }
            }

            // Series Winner Check (Runs only after a game has concluded)
            if (teamA.gamesWon == targetWins) {
                println("\n*** SERIES WINNER: ${teamA.name} has won the series ($targetWins to ${teamB.gamesWon})! ***")
            } else if (teamB.gamesWon == targetWins) {
                println("\n*** SERIES WINNER: ${teamB.name} has won the series ($targetWins to ${teamA.gamesWon})! ***")
            } else {
                // Prompt user to continue to the next game in the series
                readInput("\nPress ENTER to continue to the next GAME in the series...")
            }
        }
    }
}

/**
 * Main function to start the application.
 */
fun main() {
    DominoesTracker().start()
}
