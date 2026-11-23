package se.exempel.avstamning.app.port

import se.exempel.avstamning.domain.Hamtningsresultat
import se.exempel.avstamning.domain.Kallsystem

interface Underlagshamtning {
    val systemId: Kallsystem
    fun hamta(affarsreferens: String): Hamtningsresultat
}
