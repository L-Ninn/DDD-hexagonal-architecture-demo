package se.exempel.avstamning.integration.persistence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.UUID

interface QueRepository : JpaRepository<QueEntity, UUID> {
    fun findFirstByStatusOrderByTimestampAscIdAsc(status: QueStatus): QueEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByIdAndStatus(id: UUID, status: QueStatus): QueEntity?
}
