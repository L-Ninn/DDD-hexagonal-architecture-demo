package se.exempel.avstamning.domain

import java.time.Instant
import java.util.UUID

data class Jamforelse(
    val id: UUID,
    val affarsreferens: String,
    val timestamp: Instant,
) {
    init {
        require(affarsreferens.isNotBlank()) { "Affarsreferens maste finnas" }
    }

    fun jamfor(resultat: List<Hamtningsresultat>): Jamforelseresultat {
        require(resultat.size == Kallsystem.entries.size &&
            resultat.map { it.systemId }.toSet() == Kallsystem.entries.toSet()) {
            "Jamforelsen kraver ett hamtningsresultat fran vart och ett av de tre systemen"
        }

        val problem = resultat.filterIsInstance<Hamtningsresultat.Ofullstandigt>()
        if (problem.isNotEmpty()) return Jamforelseresultat.Ofullstandigt(this, problem)

        val underlag = resultat.map {
            when (it) {
                is Hamtningsresultat.Tillgangligt -> it.underlag
                is Hamtningsresultat.Ofullstandigt -> error("Ofullstandigt underlag hanteras ovan")
            }
        }
        require(underlag.all { it.affarsreferens == affarsreferens }) {
            "Alla underlag maste avse jamforelsens affarsreferens"
        }

        val avvikelser = Jamforelsefalt.entries.flatMap { falt ->
            if (underlag.map(falt::varde).distinct().size == 1) {
                emptyList()
            } else {
                underlag.map { Avvikelse(it.systemId, it.refId, falt) }
            }
        }
        return if (avvikelser.isEmpty()) Jamforelseresultat.Lika(this)
        else Jamforelseresultat.Olika(this, avvikelser)
    }
}
