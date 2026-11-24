package se.exempel.avstamning.app.port

import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.domain.Tjanstuppgifter
import se.exempel.avstamning.domain.Upptagsuppgifter

interface Upptagshamtning {
    fun hamtaFranKallsystem(kallsystem: Kallsystem, kallreferens: String): Upptagsuppgifter

    fun hamtaFranTjanster(upptagsId: String): Tjanstuppgifter
}
