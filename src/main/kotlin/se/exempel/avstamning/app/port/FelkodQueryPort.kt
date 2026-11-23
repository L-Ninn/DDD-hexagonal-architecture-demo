package se.exempel.avstamning.app.port

import se.exempel.avstamning.domain.RegistreradFelkod

interface FelkodQueryPort {
    fun hamtaAlla(): List<RegistreradFelkod>
}
