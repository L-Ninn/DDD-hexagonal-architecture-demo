package se.exempel.avstamning.integration.persistence

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface QueRepository : JpaRepository<QueEntity, UUID> {
    fun findByStatusOrderByTimestampAscIdAsc(status: QueStatus, limit: Limit): List<QueEntity>
}
