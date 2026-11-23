package se.exempel.avstamning.domain

import java.time.Instant
import java.util.UUID

data class RegistreradFelkod(
    val id: UUID,
    val kod: Felkod,
    val systemId: Kallsystem,
    val refId: String,
    val jamforelseId: UUID,
    val affarsreferens: String,
    val falt: Jamforelsefalt,
    val timestamp: Instant,
)
