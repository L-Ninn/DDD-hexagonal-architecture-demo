package se.exempel.avstamning.api.dto

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import se.exempel.avstamning.domain.RegistreradFelkod

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface FelkodResponseMapper {
    fun tillResponse(felkod: RegistreradFelkod): FelkodResponse
}
