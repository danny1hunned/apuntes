// FILE: StoredMatch.kt
package com.apuntes.data

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

/**
 * One completed series (match) in a tournament.
 * Points represent full-series totals, not per round.
 * Stored in CHRONO order (finish order).
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class StoredMatch(
    val aName: String,
    val bName: String,
    val aWins: Int,
    val bWins: Int,
    val bestOf: Int,
    val targetScore: Int,
    val pointsA: Int,
    val pointsB: Int
)
