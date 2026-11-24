package se.exempel.avstamning.domain

import java.util.UUID

/** En köpost: kontrollera upptaget som har [kallreferens] i [kallsystem]. */
data class Kontrolluppdrag(
    val id: UUID,
    val kallreferens: String,
    val kallsystem: Kallsystem,
)
