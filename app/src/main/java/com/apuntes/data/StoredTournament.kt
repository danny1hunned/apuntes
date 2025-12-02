package com.apuntes.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
@Serializable
data class StoredTournament(
    val name: String,
    val timestamp: String,
    val standings: List<StoredStanding>,
    val matches: List<StoredMatch>
)
