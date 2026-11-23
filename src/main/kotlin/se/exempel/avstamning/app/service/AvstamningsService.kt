package se.exempel.avstamning.app.service

import org.slf4j.LoggerFactory
import se.exempel.avstamning.app.port.Avstamningsko
import se.exempel.avstamning.app.port.Underlagshamtning
import se.exempel.avstamning.domain.Avstamningsuppdrag
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
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        require(underlagshamtningar.size == Kallsystem.entries.size &&
            underlagshamtningar.map { it.systemId }.toSet() == Kallsystem.entries.toSet()) {
            "Exakt en hamtare for vart och ett av de tre systemen maste konfigureras"
        }
    }

    fun stamAv() {
        try {
            val uppdrag = avstamningsko.hamtaNasta() ?: return
            val resultat = jamforOchSpara(uppdrag)
            loggaResultat(resultat)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn("Avstämningen avbröts; uppdraget ligger kvar i kön", exception)
        } catch (exception: Exception) {
            logger.error("Avstämningen misslyckades; inget jämförelseutlåtande kunde slutföras", exception)
        }
    }

    private fun jamforOchSpara(uppdrag: Avstamningsuppdrag): Jamforelseresultat {
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
