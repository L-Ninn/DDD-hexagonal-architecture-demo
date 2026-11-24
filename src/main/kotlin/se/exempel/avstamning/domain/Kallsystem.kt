package se.exempel.avstamning.domain

/** Systemet där upptaget finns registrerat. Köpostens topic avgör vilket. */
enum class Kallsystem(val topic: String) {
    SYSTEM_A("system-a"),
    SYSTEM_B("system-b"),
    SYSTEM_C("system-c");

    companion object {
        fun franTopic(topic: String): Kallsystem = entries.single { it.topic == topic }
    }
}

/** Tjänsterna som slås upp med upptagsid:t från källsystemet. */
enum class Upptagstjanst { TJANST_X, TJANST_Y, TJANST_Z }
