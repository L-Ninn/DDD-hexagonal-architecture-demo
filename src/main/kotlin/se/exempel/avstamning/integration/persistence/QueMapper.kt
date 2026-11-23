package se.exempel.avstamning.integration.persistence

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import se.exempel.avstamning.domain.Avstamningsuppdrag

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface QueMapper {
    fun tillUppdrag(entity: QueEntity): Avstamningsuppdrag
}
