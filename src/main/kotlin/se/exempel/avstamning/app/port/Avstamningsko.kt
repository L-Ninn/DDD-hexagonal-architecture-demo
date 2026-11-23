package se.exempel.avstamning.app.port

import se.exempel.avstamning.domain.Avstamningsuppdrag
import se.exempel.avstamning.domain.RegistreradFelkod
import java.util.UUID

interface Avstamningsko {
    fun hamtaNasta(): Avstamningsuppdrag?

    /** Felkoderna och uppdragets slutförande ska sparas atomiskt. */
    fun slutfor(uppdragId: UUID, felkoder: List<RegistreradFelkod>)

    fun markeraMisslyckat(uppdragId: UUID)
}
