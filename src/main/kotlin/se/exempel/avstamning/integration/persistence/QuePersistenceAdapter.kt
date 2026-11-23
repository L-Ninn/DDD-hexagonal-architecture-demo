package se.exempel.avstamning.integration.persistence

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import se.exempel.avstamning.app.port.Avstamningsko
import se.exempel.avstamning.app.port.Avvikelselagring
import se.exempel.avstamning.domain.Avstamningsuppdrag
import se.exempel.avstamning.domain.RegistreradFelkod
import java.util.UUID

@Repository
class QuePersistenceAdapter(
    private val repository: QueRepository,
    private val mapper: QueMapper,
    private val avvikelselagring: Avvikelselagring,
) : Avstamningsko {
    @Transactional(readOnly = true)
    override fun hamtaNasta(): Avstamningsuppdrag? =
        repository.findFirstByStatusOrderByTimestampAscIdAsc(QueStatus.VANTAR)?.let(mapper::tillUppdrag)

    @Transactional
    override fun slutfor(uppdragId: UUID, felkoder: List<RegistreradFelkod>) {
        val entity = hamtaVantande(uppdragId)
        avvikelselagring.spara(felkoder)
        entity.status = QueStatus.KLART
    }

    @Transactional
    override fun markeraMisslyckat(uppdragId: UUID) {
        hamtaVantande(uppdragId).status = QueStatus.MISSLYCKAT
    }

    private fun hamtaVantande(uppdragId: UUID): QueEntity =
        checkNotNull(repository.findByIdAndStatus(uppdragId, QueStatus.VANTAR)) {
            "Uppdraget $uppdragId saknas eller är redan avslutat"
        }
}
