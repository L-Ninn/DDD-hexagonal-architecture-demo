package se.exempel.avstamning.app.scheduler

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import se.exempel.avstamning.app.service.UpptagKontrollService

@Component
class UpptagKontrollScheduler(
    private val upptagKontrollService: UpptagKontrollService,
) {
    @Scheduled(
        fixedDelayString = "\${upptagskontroll.fixed-delay}",
        initialDelayString = "\${upptagskontroll.initial-delay}",
    )
    fun kontrolleraVantandeUppdrag() {
        upptagKontrollService.kontrolleraVantandeUppdrag()
    }
}
