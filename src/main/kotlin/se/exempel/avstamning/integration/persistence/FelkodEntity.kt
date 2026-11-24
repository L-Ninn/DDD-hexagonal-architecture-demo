package se.exempel.avstamning.integration.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import se.exempel.avstamning.domain.Felkod
import se.exempel.avstamning.domain.RegistreradFelkod
import se.exempel.avstamning.domain.Upptagstjanst
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "felkoder")
class FelkodEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "kod_nummer", nullable = false, updatable = false)
    var kodNummer: Int,

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    var beskrivning: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    var tjanst: Upptagstjanst,

    @Column(name = "uppdrag_id", nullable = false, updatable = false)
    var uppdragId: UUID,

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    var kallreferens: String,

    @Column(name = "upptags_id", nullable = false, updatable = false, columnDefinition = "text")
    var upptagsId: String,

    @Column(nullable = false, updatable = false, columnDefinition = "timestamptz")
    var tidpunkt: Instant,
) {
    fun tillFelkod() = RegistreradFelkod(
        id = id,
        felkod = Felkod.franNummer(kodNummer),
        tjanst = tjanst,
        uppdragId = uppdragId,
        kallreferens = kallreferens,
        upptagsId = upptagsId,
        tidpunkt = tidpunkt,
    )

    companion object {
        fun fran(felkod: RegistreradFelkod) = FelkodEntity(
            id = felkod.id,
            kodNummer = felkod.felkod.nummer,
            beskrivning = felkod.felkod.beskrivning,
            tjanst = felkod.tjanst,
            uppdragId = felkod.uppdragId,
            kallreferens = felkod.kallreferens,
            upptagsId = felkod.upptagsId,
            tidpunkt = felkod.tidpunkt,
        )
    }
}
