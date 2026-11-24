package se.exempel.avstamning.api.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import se.exempel.avstamning.api.dto.FelkodResponse
import se.exempel.avstamning.app.service.FelkodService

@RestController
@RequestMapping("/api/felkoder")
class FelkodController(
    private val felkodService: FelkodService,
) {
    @GetMapping
    fun hamtaAlla(): List<FelkodResponse> = felkodService.hamtaAlla().map(FelkodResponse::fran)
}
