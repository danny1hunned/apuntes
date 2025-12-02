package com.apuntes.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@OptIn(InternalSerializationApi::class)
@Serializable
data class StoredStanding(
    val teamName: String,
    val seriesWins: Int,
    val matchesPlayed: Int,
    val points: Int,
    val pointsAllowed: Int,
    val scoreDiff: Int
)
