package se.exempel.avstamning.integration.kallsystem

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import org.springframework.web.client.RestClient
import se.exempel.avstamning.domain.Upptagsuppgifter

class SystemBClient(
    private val restClient: RestClient,
    private val mapper: SystemBMapper,
) {
    fun hamta(kallreferens: String): Upptagsuppgifter {
        val response = restClient.get()
            .uri { it.path("/records").queryParam("recordId", kallreferens).build() }
            .retrieve()
            .body(SystemBResponse::class.java)
        return mapper.tillUppgifter(checkNotNull(response))
    }
}

data class SystemBResponse(val recordId: String, val uptake: SystemBUptake, val subject: SystemBSubject)

data class SystemBUptake(val id: String)

data class SystemBSubject(val personalNumber: String, val fullName: String)

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface SystemBMapper {
    @Mapping(target = "upptagsId", source = "uptake.id")
    @Mapping(target = "personId", source = "subject.personalNumber")
    @Mapping(target = "namn", source = "subject.fullName")
    fun tillUppgifter(response: SystemBResponse): Upptagsuppgifter
}
