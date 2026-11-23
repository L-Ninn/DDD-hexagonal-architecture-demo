package se.exempel.avstamning.domain

sealed interface Jamforelseresultat {
    val jamforelse: Jamforelse

    data class Lika(override val jamforelse: Jamforelse) : Jamforelseresultat

    data class Olika(
        override val jamforelse: Jamforelse,
        val avvikelser: List<Avvikelse>,
    ) : Jamforelseresultat

    data class Ofullstandigt(
        override val jamforelse: Jamforelse,
        val problem: List<Hamtningsresultat.Ofullstandigt>,
    ) : Jamforelseresultat
}
