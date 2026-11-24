package se.exempel.avstamning.app.port

import se.exempel.avstamning.domain.RegistreradFelkod

interface Felkodlagring {
    fun spara(felkoder: List<RegistreradFelkod>)
}
