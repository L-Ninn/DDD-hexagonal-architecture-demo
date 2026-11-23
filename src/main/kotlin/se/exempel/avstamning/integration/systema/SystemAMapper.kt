package se.exempel.avstamning.integration.systema

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import se.exempel.avstamning.domain.Kallunderlag

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface SystemAMapper {
    @Mapping(target = "systemId", constant = "SYSTEM_A")
    @Mapping(target = "refId", source = "id")
    @Mapping(target = "affarsreferens", source = "customerNumber")
    @Mapping(target = "namn", source = "name")
    @Mapping(target = "epost", source = "email")
    fun tillUnderlag(response: SystemAResponse): Kallunderlag
}
