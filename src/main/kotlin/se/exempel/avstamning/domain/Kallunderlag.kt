package se.exempel.avstamning.domain

enum class Kallsystem { SYSTEM_A, SYSTEM_B, SYSTEM_C }

data class Kallunderlag(
    val systemId: Kallsystem,
    val refId: String,
    val affarsreferens: String,
    val namn: String,
    val epost: String,
) {
    init {
        require(refId.isNotBlank()) { "Kallans lokala referens maste finnas" }
        require(affarsreferens.isNotBlank()) { "Gemensam affarsreferens maste finnas" }
        require(namn.isNotBlank()) { "Namn maste finnas" }
        require(epost.isNotBlank()) { "E-post maste finnas" }
    }
}

sealed interface Hamtningsresultat {
    val systemId: Kallsystem

    data class Tillgangligt(val underlag: Kallunderlag) : Hamtningsresultat {
        override val systemId: Kallsystem get() = underlag.systemId
    }

    data class Ofullstandigt(
        override val systemId: Kallsystem,
        val orsak: Underlagsproblem,
    ) : Hamtningsresultat
}

enum class Underlagsproblem {
    SAKNAS,
    HTTP_FEL,
    ANROPSFEL,
    OGILTIGT_FORMAT,
    FEL_AFFARSREFERENS,
}
