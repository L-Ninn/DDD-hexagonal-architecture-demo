package se.exempel.avstamning.integration.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.domain.Kontrolluppdrag
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "que")
class QueEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID,

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    var kallreferens: String,

    @Column(nullable = false, updatable = false, length = 40)
    var topic: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: QueStatus,

    @Column(nullable = false, updatable = false, columnDefinition = "timestamptz")
    var timestamp: Instant,
) {
    fun tillUppdrag() = Kontrolluppdrag(id, kallreferens, Kallsystem.franTopic(topic))
}

enum class QueStatus { VANTAR, KLART, MISSLYCKAT }
