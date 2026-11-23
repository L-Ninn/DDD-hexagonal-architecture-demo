package se.exempel.avstamning.integration.systemc

import org.springframework.web.client.RestClient
import se.exempel.avstamning.app.port.Underlagshamtning
import se.exempel.avstamning.domain.Hamtningsresultat
import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.integration.hamtaUnderlag

class SystemCClient(
    private val restClient: RestClient,
    private val mapper: SystemCMapper,
) : Underlagshamtning {
    override val systemId = Kallsystem.SYSTEM_C

    override fun hamta(affarsreferens: String): Hamtningsresultat =
        hamtaUnderlag(systemId, affarsreferens) {
            restClient.get()
                .uri("/customer-data/{businessReference}", affarsreferens)
                .retrieve()
                .body(SystemCResponse::class.java)
                ?.let(mapper::tillUnderlag)
        }
}
