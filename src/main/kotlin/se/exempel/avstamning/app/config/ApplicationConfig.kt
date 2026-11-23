package se.exempel.avstamning.app.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestClient
import se.exempel.avstamning.app.port.Avvikelselagring
import se.exempel.avstamning.app.port.FelkodQueryPort
import se.exempel.avstamning.app.port.Underlagshamtning
import se.exempel.avstamning.app.service.AvstamningsService
import se.exempel.avstamning.app.service.FelkodService
import se.exempel.avstamning.integration.systema.SystemAClient
import se.exempel.avstamning.integration.systema.SystemAMapper
import se.exempel.avstamning.integration.systemb.SystemBClient
import se.exempel.avstamning.integration.systemb.SystemBMapper
import se.exempel.avstamning.integration.systemc.SystemCClient
import se.exempel.avstamning.integration.systemc.SystemCMapper
import java.net.URI
import java.time.Clock

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(AvstamningsProperties::class)
class ApplicationConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun avstamningsService(
        underlagshamtningar: List<Underlagshamtning>,
        avvikelselagring: Avvikelselagring,
        clock: Clock,
    ) = AvstamningsService(underlagshamtningar, avvikelselagring, clock)

    @Bean
    fun felkodService(felkodQueryPort: FelkodQueryPort) = FelkodService(felkodQueryPort)

    @Bean
    fun systemAClient(builder: RestClient.Builder, properties: AvstamningsProperties, mapper: SystemAMapper) =
        SystemAClient(restClient(builder, properties.kallor.systemAUrl, properties.http), mapper)

    @Bean
    fun systemBClient(builder: RestClient.Builder, properties: AvstamningsProperties, mapper: SystemBMapper) =
        SystemBClient(restClient(builder, properties.kallor.systemBUrl, properties.http), mapper)

    @Bean
    fun systemCClient(builder: RestClient.Builder, properties: AvstamningsProperties, mapper: SystemCMapper) =
        SystemCClient(restClient(builder, properties.kallor.systemCUrl, properties.http), mapper)

    private fun restClient(builder: RestClient.Builder, baseUrl: URI, http: AvstamningsProperties.Http): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(http.connectTimeout)
            setReadTimeout(http.readTimeout)
        }
        return builder.clone().baseUrl(baseUrl.toString()).requestFactory(requestFactory).build()
    }
}
