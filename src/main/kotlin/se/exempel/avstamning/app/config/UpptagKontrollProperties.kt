package se.exempel.avstamning.app.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties("upptagskontroll")
data class UpptagKontrollProperties(
    val fixedDelay: Duration,
    val initialDelay: Duration,
    val antalPerKorning: Int,
    val http: Http,
    val kallsystem: Kallsystemadresser,
    val tjanster: Tjanstadresser,
) {
    data class Http(val connectTimeout: Duration, val readTimeout: Duration)

    data class Kallsystemadresser(val systemAUrl: URI, val systemBUrl: URI, val systemCUrl: URI)

    data class Tjanstadresser(val tjanstXUrl: URI, val tjanstYUrl: URI, val tjanstZUrl: URI)
}
