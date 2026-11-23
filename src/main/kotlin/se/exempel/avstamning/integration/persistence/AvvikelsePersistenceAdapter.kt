package se.exempel.avstamning.integration.persistence

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import se.exempel.avstamning.app.port.Avvikelselagring
import se.exempel.avstamning.app.port.FelkodQueryPort
import se.exempel.avstamning.domain.RegistreradFelkod

@Repository
class AvvikelsePersistenceAdapter(
    private val repository: FelkodRepository,
    private val mapper: FelkodPersistenceMapper,
) : Avvikelselagring, FelkodQueryPort {
    @Transactional
    override fun spara(felkoder: List<RegistreradFelkod>) {
        repository.saveAllAndFlush(felkoder.map(mapper::tillEntity))
    }

    @Transactional(readOnly = true)
    override fun hamtaAlla(): List<RegistreradFelkod> =
        repository.findAllByOrderByTimestampDescIdAsc().map(mapper::tillFelkod)
}
