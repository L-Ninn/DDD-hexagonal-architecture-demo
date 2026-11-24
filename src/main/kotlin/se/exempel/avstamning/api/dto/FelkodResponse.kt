package se.exempel.avstamning.api.dto

import se.exempel.avstamning.domain.RegistreradFelkod
import java.time.Instant
import java.util.UUID

data class FelkodResponse(
    val id: UUID,
    val kodNummer: Int,
    val beskrivning: String,
    val tjanst: String,
    val uppdragId: UUID,
    val kallreferens: String,
    val upptagsId: String,
    val tidpunkt: Instant,
) {
    companion object {
        fun fran(felkod: RegistreradFelkod) = FelkodResponse(
            id = felkod.id,
            kodNummer = felkod.felkod.nummer,
            beskrivning = felkod.felkod.beskrivning,
            tjanst = felkod.tjanst.name,
            uppdragId = felkod.uppdragId,
            kallreferens = felkod.kallreferens,
            upptagsId = felkod.upptagsId,
            tidpunkt = felkod.tidpunkt,
        )
    }
}
