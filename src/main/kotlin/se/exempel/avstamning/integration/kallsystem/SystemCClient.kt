package se.exempel.avstamning.integration.kallsystem

import com.fasterxml.jackson.annotation.JsonProperty
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import org.springframework.web.client.RestClient
import se.exempel.avstamning.domain.Upptagsuppgifter

class SystemCClient(
    private val restClient: RestClient,
    private val mapper: SystemCMapper,
) {
    fun hamta(kallreferens: String): Upptagsuppgifter {
        val response = restClient.get()
            .uri("/upptagsdata/{localReference}", kallreferens)
            .retrieve()
            .body(SystemCResponse::class.java)
        return mapper.tillUppgifter(checkNotNull(response))
    }
}

data class SystemCResponse(
    @param:JsonProperty("local_reference") val localReference: String,
    @param:JsonProperty("upptags_id") val upptagsId: String,
    @param:JsonProperty("person_id") val personId: String,
    @param:JsonProperty("full_name") val fullName: String,
)

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface SystemCMapper {
    @Mapping(target = "upptagsId", source = "upptagsId")
    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "namn", source = "fullName")
    fun tillUppgifter(response: SystemCResponse): Upptagsuppgifter
}
