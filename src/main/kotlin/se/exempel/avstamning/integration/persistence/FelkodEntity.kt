package se.exempel.avstamning.integration.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import se.exempel.avstamning.domain.Felkod
import se.exempel.avstamning.domain.Jamforelsefalt
import se.exempel.avstamning.domain.Kallsystem
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "felkoder")
class FelkodEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    var kod: Felkod,

    @Enumerated(EnumType.STRING)
    @Column(name = "system_id", nullable = false, updatable = false, length = 20)
    var systemId: Kallsystem,

    @Column(name = "ref_id", nullable = false, updatable = false, columnDefinition = "text")
    var refId: String,

    @Column(name = "jamforelse_id", nullable = false, updatable = false)
    var jamforelseId: UUID,

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    var affarsreferens: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    var falt: Jamforelsefalt,

    @Column(nullable = false, updatable = false, columnDefinition = "timestamptz")
    var timestamp: Instant,
)
