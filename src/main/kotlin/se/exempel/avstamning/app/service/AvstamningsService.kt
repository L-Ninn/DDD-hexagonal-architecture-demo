package se.exempel.avstamning.app.service

import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        require(underlagshamtningar.size == Kallsystem.entries.size &&
            underlagshamtningar.map { it.systemId }.toSet() == Kallsystem.entries.toSet()) {
            "Exakt en hamtare for vart och ett av de tre systemen maste konfigureras"
        }
    }

    fun stamAv(affarsreferens: String) {
        try {
            val resultat = jamforOchSpara(affarsreferens)
            loggaResultat(resultat)
        } catch (exception: Exception) {
            logger.error("Avstämningen misslyckades; inget jämförelseutlåtande kunde slutföras", exception)
        }
    }

    private fun jamforOchSpara(affarsreferens: String): Jamforelseresultat {
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

    private fun loggaResultat(resultat: Jamforelseresultat) {
        when (resultat) {
            is Jamforelseresultat.Lika ->
                logger.info("jamforelseId={} utlatande=LIKA", resultat.jamforelse.id)
            is Jamforelseresultat.Olika ->
                logger.info(
                    "jamforelseId={} utlatande=OLIKA sparadeFelkoder={}",
                    resultat.jamforelse.id, resultat.avvikelser.size,
                )
            is Jamforelseresultat.Ofullstandigt -> {
                logger.warn("jamforelseId={} utlatande=OFULLSTANDIGT", resultat.jamforelse.id)
                resultat.problem.forEach { problem ->
                    logger.warn(
                        "jamforelseId={} systemId={} underlagsproblem={}",
                        resultat.jamforelse.id, problem.systemId, problem.orsak,
                    )
                }
            }
        }
    }
}
