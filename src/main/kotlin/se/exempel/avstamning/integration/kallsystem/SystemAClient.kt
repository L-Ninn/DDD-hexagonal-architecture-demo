package se.exempel.avstamning.integration.kallsystem

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import org.springframework.web.client.RestClient
import se.exempel.avstamning.domain.Upptagsuppgifter

class SystemAClient(
    private val restClient: RestClient,
    private val mapper: SystemAMapper,
) {
    fun hamta(kallreferens: String): Upptagsuppgifter {
        val response = restClient.get()
            .uri("/upptag/{id}", kallreferens)
            .retrieve()
            .body(SystemAResponse::class.java)
        return mapper.tillUppgifter(checkNotNull(response))
    }
}

data class SystemAResponse(val id: String, val upptagsId: String, val person: SystemAPerson)

data class SystemAPerson(val personnummer: String, val namn: String)

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface SystemAMapper {
    @Mapping(target = "upptagsId", source = "upptagsId")
    @Mapping(target = "personId", source = "person.personnummer")
    @Mapping(target = "namn", source = "person.namn")
    fun tillUppgifter(response: SystemAResponse): Upptagsuppgifter
}
