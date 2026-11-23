package se.exempel.avstamning.integration.persistence

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import se.exempel.avstamning.domain.RegistreradFelkod

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface FelkodPersistenceMapper {
    fun tillEntity(felkod: RegistreradFelkod): FelkodEntity
    fun tillFelkod(entity: FelkodEntity): RegistreradFelkod
}
