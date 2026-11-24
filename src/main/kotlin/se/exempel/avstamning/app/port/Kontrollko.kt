package se.exempel.avstamning.app.port

import se.exempel.avstamning.domain.Kontrolluppdrag
import java.util.UUID

interface Kontrollko {
    fun hamtaVantande(antal: Int): List<Kontrolluppdrag>

    fun markeraKlart(uppdragId: UUID)

    fun markeraMisslyckat(uppdragId: UUID)
}
