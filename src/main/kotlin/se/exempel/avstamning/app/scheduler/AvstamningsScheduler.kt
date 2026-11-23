package se.exempel.avstamning.app.scheduler

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import se.exempel.avstamning.app.config.AvstamningsProperties
import se.exempel.avstamning.app.service.AvstamningsService

@Component
class AvstamningsScheduler(
    private val avstamningsService: AvstamningsService,
    private val properties: AvstamningsProperties,
) {
    @Scheduled(
        fixedDelayString = "\${avstamning.fixed-delay}",
        initialDelayString = "\${avstamning.initial-delay}",
    )
    fun stamAv() {
        avstamningsService.stamAv(properties.affarsreferens)
    }
}
