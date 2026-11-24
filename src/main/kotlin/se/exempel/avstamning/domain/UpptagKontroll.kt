package se.exempel.avstamning.domain

import java.time.Instant
import java.util.UUID

/**
 * Kontrollerar ett upptag. Källsystemets uppgifter är facit:
 * varje avvikelse registreras på den tjänst som avviker.
 */
class UpptagKontroll(
    private val uppdrag: Kontrolluppdrag,
    private val tidpunkt: Instant,
) {
    fun jamfor(kallsystemet: Upptagsuppgifter, tjansterna: Tjanstuppgifter): List<RegistreradFelkod> =
        tjansterna.perTjanst().flatMap { (tjanst, uppgifter) ->
            avvikelser(uppgifter, facit = kallsystemet).map { registrera(it, tjanst, kallsystemet.upptagsId) }
        }

    private fun avvikelser(uppgifter: Upptagsuppgifter, facit: Upptagsuppgifter): List<Felkod> = buildList {
        if (uppgifter.upptagsId != facit.upptagsId) add(Felkod.UPPTAGSID_AVVIKER)
        if (uppgifter.personId != facit.personId) add(Felkod.PERSONID_AVVIKER)
        if (uppgifter.namn != facit.namn) add(Felkod.NAMN_AVVIKER)
    }

    private fun registrera(felkod: Felkod, tjanst: Upptagstjanst, upptagsId: String) = RegistreradFelkod(
        id = UUID.randomUUID(),
        felkod = felkod,
        tjanst = tjanst,
        uppdragId = uppdrag.id,
        kallreferens = uppdrag.kallreferens,
        upptagsId = upptagsId,
        tidpunkt = tidpunkt,
    )
}
