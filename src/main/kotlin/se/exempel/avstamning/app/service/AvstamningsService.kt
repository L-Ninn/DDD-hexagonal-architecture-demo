package se.exempel.avstamning.app.service

import se.exempel.avstamning.app.port.Avvikelselagring
import se.exempel.avstamning.app.port.Underlagshamtning
import se.exempel.avstamning.domain.Jamforelse
import se.exempel.avstamning.domain.Jamforelseresultat
import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.domain.RegistreradFelkod
import java.time.Clock
import java.util.UUID

class AvstamningsService(
    private val underlagshamtningar: List<Underlagshamtning>,
    private val avvikelselagring: Avvikelselagring,
    private val clock: Clock,
) {
    init {
        require(underlagshamtningar.size == Kallsystem.entries.size &&
            underlagshamtningar.map { it.systemId }.toSet() == Kallsystem.entries.toSet()) {
            "Exakt en hamtare for vart och ett av de tre systemen maste konfigureras"
        }
    }

    fun stamAv(affarsreferens: String): Jamforelseresultat {
        val jamforelse = Jamforelse(UUID.randomUUID(), affarsreferens, clock.instant())
        val resultat = jamforelse.jamfor(underlagshamtningar.map { it.hamta(affarsreferens) })
        if (resultat is Jamforelseresultat.Olika) {
            val felkoder = resultat.avvikelser.map { avvikelse ->
                RegistreradFelkod(
                    id = UUID.randomUUID(),
                    kod = avvikelse.kod,
                    systemId = avvikelse.systemId,
                    refId = avvikelse.refId,
                    jamforelseId = jamforelse.id,
                    affarsreferens = affarsreferens,
                    falt = avvikelse.falt,
                    timestamp = jamforelse.timestamp,
                )
            }
            avvikelselagring.spara(felkoder)
        }
        return resultat
    }
}
