package se.exempel.avstamning.integration.systemb

import org.springframework.web.client.RestClient
import se.exempel.avstamning.app.port.Underlagshamtning
import se.exempel.avstamning.domain.Hamtningsresultat
import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.integration.hamtaUnderlag

class SystemBClient(
    private val restClient: RestClient,
    private val mapper: SystemBMapper,
) : Underlagshamtning {
    override val systemId = Kallsystem.SYSTEM_B

    override fun hamta(affarsreferens: String): Hamtningsresultat =
        hamtaUnderlag(systemId, affarsreferens) {
            restClient.get()
                .uri { it.path("/records").queryParam("customerNumber", affarsreferens).build() }
                .retrieve()
                .body(SystemBResponse::class.java)
                ?.let(mapper::tillUnderlag)
        }
}
