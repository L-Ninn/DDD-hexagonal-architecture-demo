package se.exempel.avstamning.app.service

import se.exempel.avstamning.app.port.Avstamningsko
import se.exempel.avstamning.app.port.Underlagshamtning
import se.exempel.avstamning.domain.Jamforelse
import se.exempel.avstamning.domain.Jamforelseresultat
import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.domain.RegistreradFelkod
import java.time.Clock
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService

class AvstamningsService(
    private val underlagshamtningar: List<Underlagshamtning>,
    private val avstamningsko: Avstamningsko,
    private val clock: Clock,
    private val underlagsExecutor: ExecutorService,
) {
    init {
        require(underlagshamtningar.size == Kallsystem.entries.size &&
            underlagshamtningar.map { it.systemId }.toSet() == Kallsystem.entries.toSet()) {
            "Exakt en hamtare for vart och ett av de tre systemen maste konfigureras"
        }
    }

    fun stamAv(): Jamforelseresultat? {
        val uppdrag = avstamningsko.hamtaNasta() ?: return null
        val jamforelse = Jamforelse(uppdrag.id, uppdrag.affarsreferens, clock.instant())
        val hamtningar = underlagsExecutor.invokeAll(
            underlagshamtningar.map { hamtare -> Callable { hamtare.hamta(uppdrag.affarsreferens) } },
        )
        val resultat = jamforelse.jamfor(hamtningar.map { it.get() })
        if (resultat is Jamforelseresultat.Ofullstandigt) {
            avstamningsko.markeraMisslyckat(uppdrag.id)
            return resultat
        }

        val felkoder = if (resultat is Jamforelseresultat.Olika) {
            resultat.avvikelser.map { avvikelse ->
                RegistreradFelkod(
                    id = UUID.randomUUID(),
                    kod = avvikelse.kod,
                    systemId = avvikelse.systemId,
                    refId = avvikelse.refId,
                    jamforelseId = jamforelse.id,
                    affarsreferens = uppdrag.affarsreferens,
                    falt = avvikelse.falt,
                    timestamp = jamforelse.timestamp,
                )
            }
        } else emptyList()
        avstamningsko.slutfor(uppdrag.id, felkoder)
        return resultat
    }
}
