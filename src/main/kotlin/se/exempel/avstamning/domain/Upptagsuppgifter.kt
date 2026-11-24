package se.exempel.avstamning.domain

/** Det ett system vet om ett upptag och personen det gäller. */
data class Upptagsuppgifter(
    val upptagsId: String,
    val personId: String,
    val namn: String,
)

/** Svaren från de tre tjänsterna. */
data class Tjanstuppgifter(
    val tjanstX: Upptagsuppgifter,
    val tjanstY: Upptagsuppgifter,
    val tjanstZ: Upptagsuppgifter,
) {
    fun perTjanst(): Map<Upptagstjanst, Upptagsuppgifter> = mapOf(
        Upptagstjanst.TJANST_X to tjanstX,
        Upptagstjanst.TJANST_Y to tjanstY,
        Upptagstjanst.TJANST_Z to tjanstZ,
    )
}
