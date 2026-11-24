package se.exempel.avstamning.integration.persistence

import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import se.exempel.avstamning.app.port.Kontrollko
import se.exempel.avstamning.domain.Kontrolluppdrag
import java.util.UUID

@Repository
class QuePersistenceAdapter(
    private val repository: QueRepository,
) : Kontrollko {
    @Transactional(readOnly = true)
    override fun hamtaVantande(antal: Int): List<Kontrolluppdrag> =
        repository.findByStatusOrderByTimestampAscIdAsc(QueStatus.VANTAR, Limit.of(antal))
            .map(QueEntity::tillUppdrag)

    @Transactional
    override fun markeraKlart(uppdragId: UUID) {
        andraStatus(uppdragId, QueStatus.KLART)
    }

    @Transactional
    override fun markeraMisslyckat(uppdragId: UUID) {
        andraStatus(uppdragId, QueStatus.MISSLYCKAT)
    }

    private fun andraStatus(uppdragId: UUID, status: QueStatus) {
        repository.findById(uppdragId).orElseThrow().status = status
    }
}
