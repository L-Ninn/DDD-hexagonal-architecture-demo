package se.exempel.avstamning.integration.persistence

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import se.exempel.avstamning.app.port.FelkodQueryPort
import se.exempel.avstamning.app.port.Felkodlagring
import se.exempel.avstamning.domain.RegistreradFelkod

@Repository
class FelkodPersistenceAdapter(
    private val repository: FelkodRepository,
) : Felkodlagring, FelkodQueryPort {
    @Transactional
    override fun spara(felkoder: List<RegistreradFelkod>) {
        repository.saveAll(felkoder.map(FelkodEntity::fran))
    }

    @Transactional(readOnly = true)
    override fun hamtaAlla(): List<RegistreradFelkod> =
        repository.findAllByOrderByTidpunktDescIdAsc().map(FelkodEntity::tillFelkod)
}
