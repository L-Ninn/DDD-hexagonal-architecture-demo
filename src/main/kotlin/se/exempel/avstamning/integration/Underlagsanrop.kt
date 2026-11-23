package se.exempel.avstamning.integration

import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import se.exempel.avstamning.domain.Hamtningsresultat
import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.domain.Kallunderlag
import se.exempel.avstamning.domain.Underlagsproblem

internal fun hamtaUnderlag(
    systemId: Kallsystem,
    affarsreferens: String,
    anrop: () -> Kallunderlag?,
): Hamtningsresultat = try {
    val underlag = anrop()
    when {
        underlag == null -> Hamtningsresultat.Ofullstandigt(systemId, Underlagsproblem.SAKNAS)
        underlag.affarsreferens != affarsreferens ->
            Hamtningsresultat.Ofullstandigt(systemId, Underlagsproblem.FEL_AFFARSREFERENS)
        else -> Hamtningsresultat.Tillgangligt(underlag)
    }
} catch (exception: RestClientResponseException) {
    val orsak = if (exception.statusCode.value() == 404) Underlagsproblem.SAKNAS
    else Underlagsproblem.HTTP_FEL
    Hamtningsresultat.Ofullstandigt(systemId, orsak)
} catch (exception: RestClientException) {
    Hamtningsresultat.Ofullstandigt(systemId, Underlagsproblem.ANROPSFEL)
} catch (exception: IllegalArgumentException) {
    Hamtningsresultat.Ofullstandigt(systemId, Underlagsproblem.OGILTIGT_FORMAT)
}
