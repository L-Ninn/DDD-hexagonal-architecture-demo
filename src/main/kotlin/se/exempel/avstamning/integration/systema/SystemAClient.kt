package se.exempel.avstamning.integration.systema

import org.springframework.web.client.RestClient
import se.exempel.avstamning.app.port.Underlagshamtning
import se.exempel.avstamning.domain.Hamtningsresultat
import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.integration.hamtaUnderlag

class SystemAClient(
    private val restClient: RestClient,
    private val mapper: SystemAMapper,
) : Underlagshamtning {
    override val systemId = Kallsystem.SYSTEM_A

    override fun hamta(affarsreferens: String): Hamtningsresultat =
        hamtaUnderlag(systemId, affarsreferens) {
            restClient.get()
                .uri("/customers/{customerNumber}", affarsreferens)
                .retrieve()
                .body(SystemAResponse::class.java)
                ?.let(mapper::tillUnderlag)
        }
}
