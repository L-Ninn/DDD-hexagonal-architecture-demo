package se.exempel.avstamning.app.service

import se.exempel.avstamning.app.port.FelkodQueryPort
import se.exempel.avstamning.domain.RegistreradFelkod

class FelkodService(private val felkodQueryPort: FelkodQueryPort) {
    fun hamtaAlla(): List<RegistreradFelkod> = felkodQueryPort.hamtaAlla()
}
