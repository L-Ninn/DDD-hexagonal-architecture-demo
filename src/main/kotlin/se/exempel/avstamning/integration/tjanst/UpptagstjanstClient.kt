package se.exempel.avstamning.integration.tjanst

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import org.springframework.web.client.RestClient
import se.exempel.avstamning.domain.Upptagsuppgifter

/** De tre tjänsterna har samma API-kontrakt; en instans per tjänst. */
class UpptagstjanstClient(
    private val restClient: RestClient,
    private val mapper: UpptagstjanstMapper,
) {
    fun hamta(upptagsId: String): Upptagsuppgifter {
        val response = restClient.get()
            .uri("/upptag/{upptagsId}", upptagsId)
            .retrieve()
            .body(UpptagstjanstResponse::class.java)
        return mapper.tillUppgifter(checkNotNull(response))
    }
}

data class UpptagstjanstResponse(val upptagsId: String, val personId: String, val namn: String)

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface UpptagstjanstMapper {
    fun tillUppgifter(response: UpptagstjanstResponse): Upptagsuppgifter
}
