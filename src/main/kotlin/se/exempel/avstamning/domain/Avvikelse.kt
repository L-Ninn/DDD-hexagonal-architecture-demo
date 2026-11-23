package se.exempel.avstamning.domain

enum class Jamforelsefalt {
    NAMN,
    EPOST;

    fun varde(underlag: Kallunderlag): String = when (this) {
        NAMN -> underlag.namn
        EPOST -> underlag.epost
    }
}

data class Avvikelse(
    val systemId: Kallsystem,
    val refId: String,
    val falt: Jamforelsefalt,
) {
    val kod: Felkod get() = Felkod.AVVIKANDE_FALT
}
