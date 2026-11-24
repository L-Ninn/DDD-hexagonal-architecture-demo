package se.exempel.avstamning.app.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestClient
import se.exempel.avstamning.app.port.FelkodQueryPort
import se.exempel.avstamning.app.port.Felkodlagring
import se.exempel.avstamning.app.port.Kontrollko
import se.exempel.avstamning.app.port.Upptagshamtning
import se.exempel.avstamning.app.service.FelkodService
import se.exempel.avstamning.app.service.UpptagKontrollService
import se.exempel.avstamning.integration.UpptagshamtningAdapter
import se.exempel.avstamning.integration.kallsystem.SystemAClient
import se.exempel.avstamning.integration.kallsystem.SystemAMapper
import se.exempel.avstamning.integration.kallsystem.SystemBClient
import se.exempel.avstamning.integration.kallsystem.SystemBMapper
import se.exempel.avstamning.integration.kallsystem.SystemCClient
import se.exempel.avstamning.integration.kallsystem.SystemCMapper
import se.exempel.avstamning.integration.tjanst.UpptagstjanstClient
import se.exempel.avstamning.integration.tjanst.UpptagstjanstMapper
import java.net.URI
import java.time.Clock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(UpptagKontrollProperties::class)
class ApplicationConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    /** Virtuella trådar: varje uppdrag och varje tjänsteanrop får en egen tråd. */
    @Bean(destroyMethod = "close")
    fun executor(): ExecutorService = Executors.newVirtualThreadPerTaskExecutor()

    @Bean
    fun upptagKontrollService(
        kontrollko: Kontrollko,
        upptagshamtning: Upptagshamtning,
        felkodlagring: Felkodlagring,
        clock: Clock,
        executor: ExecutorService,
        properties: UpptagKontrollProperties,
    ) = UpptagKontrollService(kontrollko, upptagshamtning, felkodlagring, clock, executor, properties.antalPerKorning)

    @Bean
    fun felkodService(felkodQueryPort: FelkodQueryPort) = FelkodService(felkodQueryPort)

    @Bean
    fun upptagshamtning(
        builder: RestClient.Builder,
        properties: UpptagKontrollProperties,
        executor: ExecutorService,
        systemAMapper: SystemAMapper,
        systemBMapper: SystemBMapper,
        systemCMapper: SystemCMapper,
        tjanstMapper: UpptagstjanstMapper,
    ): Upptagshamtning {
        fun klient(url: URI) = restClient(builder, url, properties.http)
        val kallsystem = properties.kallsystem
        val tjanster = properties.tjanster
        return UpptagshamtningAdapter(
            systemA = SystemAClient(klient(kallsystem.systemAUrl), systemAMapper),
            systemB = SystemBClient(klient(kallsystem.systemBUrl), systemBMapper),
            systemC = SystemCClient(klient(kallsystem.systemCUrl), systemCMapper),
            tjanstX = UpptagstjanstClient(klient(tjanster.tjanstXUrl), tjanstMapper),
            tjanstY = UpptagstjanstClient(klient(tjanster.tjanstYUrl), tjanstMapper),
            tjanstZ = UpptagstjanstClient(klient(tjanster.tjanstZUrl), tjanstMapper),
            executor = executor,
        )
    }

    private fun restClient(builder: RestClient.Builder, baseUrl: URI, http: UpptagKontrollProperties.Http): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(http.connectTimeout)
            setReadTimeout(http.readTimeout)
        }
        return builder.clone().baseUrl(baseUrl.toString()).requestFactory(requestFactory).build()
    }
}
