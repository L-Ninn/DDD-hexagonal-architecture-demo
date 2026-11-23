package se.exempel.avstamning.integration.systemc

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import se.exempel.avstamning.domain.Kallunderlag

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface SystemCMapper {
    @Mapping(target = "systemId", constant = "SYSTEM_C")
    @Mapping(target = "refId", source = "localReference")
    @Mapping(target = "affarsreferens", source = "businessReference")
    @Mapping(target = "namn", source = "fullName")
    @Mapping(target = "epost", source = "emailAddress")
    fun tillUnderlag(response: SystemCResponse): Kallunderlag
}
