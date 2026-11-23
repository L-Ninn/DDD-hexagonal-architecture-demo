package se.exempel.avstamning.api.dto

import se.exempel.avstamning.domain.Felkod
import se.exempel.avstamning.domain.Jamforelsefalt
import se.exempel.avstamning.domain.Kallsystem
import java.time.Instant
import java.util.UUID

data class FelkodResponse(
    val id: UUID,
    val kod: Felkod,
    val systemId: Kallsystem,
    val refId: String,
    val jamforelseId: UUID,
    val affarsreferens: String,
    val falt: Jamforelsefalt,
    val timestamp: Instant,
)
