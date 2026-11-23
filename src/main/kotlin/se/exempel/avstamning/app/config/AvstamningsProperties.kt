package se.exempel.avstamning.app.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.net.URI
import java.time.Duration

@Validated
@ConfigurationProperties("avstamning")
data class AvstamningsProperties(
    @field:NotBlank val affarsreferens: String,
    val fixedDelay: Duration,
    val initialDelay: Duration,
    val http: Http,
    val kallor: Kallor,
) {
    init {
        require(!fixedDelay.isNegative && !fixedDelay.isZero) { "fixed-delay måste vara positiv" }
        require(!initialDelay.isNegative) { "initial-delay får inte vara negativ" }
    }

    data class Http(val connectTimeout: Duration, val readTimeout: Duration) {
        init {
            require(connectTimeout.toMillis() in 1..Int.MAX_VALUE.toLong()) {
                "connect-timeout måste vara 1..${Int.MAX_VALUE} millisekunder"
            }
            require(readTimeout.toMillis() in 1..Int.MAX_VALUE.toLong()) {
                "read-timeout måste vara 1..${Int.MAX_VALUE} millisekunder"
            }
        }
    }

    data class Kallor(val systemAUrl: URI, val systemBUrl: URI, val systemCUrl: URI) {
        init {
            listOf(systemAUrl, systemBUrl, systemCUrl).forEach {
                require(it.scheme in setOf("http", "https") && it.host != null &&
                    it.query == null && it.fragment == null && it.userInfo == null) {
                    "Källornas URL:er måste vara HTTP(S)-basadresser utan inloggning, query eller fragment"
                }
            }
        }
    }
}
