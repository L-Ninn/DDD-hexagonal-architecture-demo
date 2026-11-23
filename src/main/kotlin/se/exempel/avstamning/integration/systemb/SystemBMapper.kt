package se.exempel.avstamning.integration.systemb

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import se.exempel.avstamning.domain.Kallunderlag

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface SystemBMapper {
    @Mapping(target = "systemId", constant = "SYSTEM_B")
    @Mapping(target = "refId", source = "recordId")
    @Mapping(target = "affarsreferens", source = "customer.number")
    @Mapping(target = "namn", source = "customer.displayName")
    @Mapping(target = "epost", source = "customer.contact.emailAddress")
    fun tillUnderlag(response: SystemBResponse): Kallunderlag
}
