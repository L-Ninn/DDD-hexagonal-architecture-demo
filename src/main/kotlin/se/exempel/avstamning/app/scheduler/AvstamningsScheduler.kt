package se.exempel.avstamning.app.scheduler

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import se.exempel.avstamning.app.config.AvstamningsProperties
import se.exempel.avstamning.app.service.AvstamningsService
import se.exempel.avstamning.domain.Jamforelseresultat

@Component
class AvstamningsScheduler(
    private val avstamningsService: AvstamningsService,
    private val properties: AvstamningsProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        fixedDelayString = "\${avstamning.fixed-delay}",
        initialDelayString = "\${avstamning.initial-delay}",
    )
    fun stamAv() {
        try {
            val resultat = avstamningsService.stamAv(properties.affarsreferens)
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
                    resultat.problem.forEach {
                        logger.warn(
                            "jamforelseId={} systemId={} underlagsproblem={}",
                            resultat.jamforelse.id, it.systemId, it.orsak,
                        )
                    }
                }
            }
        } catch (exception: Exception) {
            logger.error("Avstämningen misslyckades; inget jämförelseutlåtande kunde slutföras", exception)
        }
    }
}
