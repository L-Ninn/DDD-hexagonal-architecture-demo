package se.exempel.avstamning.domain

import java.time.Instant
import java.util.UUID

/** Kodnummer 1–20 är reserverade för upptagskontrollen. */
enum class Felkod(val nummer: Int, val beskrivning: String) {
    UPPTAGSID_AVVIKER(1, "Tjänsten anger ett annat upptagsid än källsystemet"),
    PERSONID_AVVIKER(2, "Tjänsten anger en annan person än källsystemet"),
    NAMN_AVVIKER(3, "Tjänsten anger ett annat namn än källsystemet");

    companion object {
        fun franNummer(nummer: Int): Felkod = entries.single { it.nummer == nummer }
    }
}

data class RegistreradFelkod(
    val id: UUID,
    val felkod: Felkod,
    val tjanst: Upptagstjanst,
    val uppdragId: UUID,
    val kallreferens: String,
    val upptagsId: String,
    val tidpunkt: Instant,
)
