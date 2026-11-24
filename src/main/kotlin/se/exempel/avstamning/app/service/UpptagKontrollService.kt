package se.exempel.avstamning.app.service

import org.slf4j.LoggerFactory
import se.exempel.avstamning.app.port.Felkodlagring
import se.exempel.avstamning.app.port.Kontrollko
import se.exempel.avstamning.app.port.Upptagshamtning
import se.exempel.avstamning.domain.Kontrolluppdrag
import se.exempel.avstamning.domain.RegistreradFelkod
import se.exempel.avstamning.domain.UpptagKontroll
import java.time.Clock
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService

class UpptagKontrollService(
    private val kontrollko: Kontrollko,
    private val upptagshamtning: Upptagshamtning,
    private val felkodlagring: Felkodlagring,
    private val clock: Clock,
    private val executor: ExecutorService,
    private val antalPerKorning: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Hämtar nästa omgång väntande uppdrag ur kön och kontrollerar dem parallellt. */
    fun kontrolleraVantandeUppdrag() {
        kontrollko.hamtaVantande(antalPerKorning)
            .map { uppdrag -> executor.submit(Callable { kontrollera(uppdrag) }) }
            .forEach { it.get() }
    }

    private fun kontrollera(uppdrag: Kontrolluppdrag) {
        try {
            felkodlagring.spara(felkoder(uppdrag))
            kontrollko.markeraKlart(uppdrag.id)
        } catch (e: Exception) {
            log.warn("Uppdrag {} kunde inte kontrolleras", uppdrag.id, e)
            kontrollko.markeraMisslyckat(uppdrag.id)
        }
    }

    private fun felkoder(uppdrag: Kontrolluppdrag): List<RegistreradFelkod> {
        val kallsystemet = upptagshamtning.hamtaFranKallsystem(uppdrag.kallsystem, uppdrag.kallreferens)
        val tjansterna = upptagshamtning.hamtaFranTjanster(kallsystemet.upptagsId)
        return UpptagKontroll(uppdrag, clock.instant()).jamfor(kallsystemet, tjansterna)
    }
}
