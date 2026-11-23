package se.exempel.avstamning.domain

import java.util.UUID

data class Avstamningsuppdrag(
    val id: UUID,
    val affarsreferens: String,
) {
    init {
        require(affarsreferens.isNotBlank()) { "Uppdragets affärsreferens måste finnas" }
    }
}
