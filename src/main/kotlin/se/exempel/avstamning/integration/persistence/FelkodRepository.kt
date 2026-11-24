package se.exempel.avstamning.integration.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FelkodRepository : JpaRepository<FelkodEntity, UUID> {
    fun findAllByOrderByTidpunktDescIdAsc(): List<FelkodEntity>
}
