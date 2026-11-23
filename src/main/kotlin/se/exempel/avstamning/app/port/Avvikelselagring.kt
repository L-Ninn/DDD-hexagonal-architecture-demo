package se.exempel.avstamning.app.port

import se.exempel.avstamning.domain.RegistreradFelkod

interface Avvikelselagring {
    fun spara(felkoder: List<RegistreradFelkod>)
}
