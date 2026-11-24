# AI-prompt: återskapa Upptagskontroll

Kopiera allt under linjen till AI-modellen. Prompten innehåller hela koden ordagrant, så att även en mindre modell kan återskapa projektet steg för steg.

---

Du ska skapa ett Kotlin/Spring Boot-projekt som heter **Upptagskontroll**. Följ stegen i ordning. Skapa varje fil med **exakt** det innehåll som anges. Hitta inte på egna klasser, namn eller beroenden.

## Vad programmet gör

1. Ett schemalagt jobb hämtar upp till 10 väntande poster ur tabellen `que`. Varje post har `kallreferens` och `topic`.
2. `topic` väljer källsystem: `system-a` → System A, `system-b` → System B, `system-c` → System C.
3. Källsystemet svarar med upptagsid, personid och namn.
4. Tjänst X, Y och Z anropas parallellt med upptagsid:t.
5. Domänklassen `UpptagKontroll` jämför varje tjänst mot källsystemet. Felkod 1 = annat upptagsid, 2 = annan person, 3 = annat namn.
6. Felkoderna sparas i tabellen `felkoder` och posten blir `KLART`. Alla fel ger `MISSLYCKAT`.
7. `GET /api/felkoder` visar alla felkoder.

## Regler

- Kotlin, Java 21, Spring Boot 3.5.7, Maven, PostgreSQL, Liquibase, MapStruct 1.6.3 (via kapt).
- Hexagonal arkitektur: `domain` får inte importera Spring, JPA eller MapStruct.
- Inga tester. Minimal loggning och felhantering.
- Svenska namn utan å, ä, ö i koden.
- Alla Kotlin-filer ligger under `src/main/kotlin/se/exempel/avstamning/`.
- Efter steg 2, 4 och 6: kör `./mvnw -q clean package`. Rätta kompileringsfel innan du går vidare. Ändra inte designen för att få det att kompilera – jämför i stället din fil med texten här.

## Steg 1 – Byggfiler

Skapa `pom.xml` exakt så här. Skapa Maven Wrapper med `mvn -N wrapper:wrapper -Dmaven=3.9.11` (eller installera Maven 3.9 och använd `mvn` i stället för `./mvnw`).

**`pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.7</version>
        <relativePath/>
    </parent>
    <groupId>se.exempel</groupId>
    <artifactId>avstamning</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>Avstamning</name>
    <description>Upptagskontroll mot kallsystem och tre tjanster</description>
    <properties>
        <java.version>21</java.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <maven.test.skip>true</maven.test.skip>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.module</groupId>
            <artifactId>jackson-module-kotlin</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-reflect</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.liquibase</groupId>
            <artifactId>liquibase-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
    <build>
        <sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <configuration>
                    <jvmTarget>${java.version}</jvmTarget>
                    <javaParameters>true</javaParameters>
                    <args>
                        <arg>-Xjsr305=strict</arg>
                    </args>
                    <compilerPlugins>
                        <plugin>spring</plugin>
                        <plugin>jpa</plugin>
                        <plugin>all-open</plugin>
                    </compilerPlugins>
                    <pluginOptions>
                        <option>all-open:annotation=jakarta.persistence.Entity</option>
                        <option>all-open:annotation=jakarta.persistence.MappedSuperclass</option>
                        <option>all-open:annotation=jakarta.persistence.Embeddable</option>
                    </pluginOptions>
                </configuration>
                <executions>
                    <execution>
                        <id>kapt</id>
                        <phase>generate-sources</phase>
                        <goals><goal>kapt</goal></goals>
                        <configuration>
                            <annotationProcessorPaths>
                                <annotationProcessorPath>
                                    <groupId>org.mapstruct</groupId>
                                    <artifactId>mapstruct-processor</artifactId>
                                    <version>${mapstruct.version}</version>
                                </annotationProcessorPath>
                            </annotationProcessorPaths>
                        </configuration>
                    </execution>
                    <execution>
                        <id>compile</id>
                        <phase>compile</phase>
                        <goals><goal>compile</goal></goals>
                    </execution>
                </executions>
                <dependencies>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-maven-allopen</artifactId>
                        <version>${kotlin.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-maven-noarg</artifactId>
                        <version>${kotlin.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration><proc>none</proc></configuration>
                <executions>
                    <execution><id>default-compile</id><phase>none</phase></execution>
                    <execution><id>default-testCompile</id><phase>none</phase></execution>
                    <execution>
                        <id>java-compile</id>
                        <phase>compile</phase>
                        <goals><goal>compile</goal></goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## Steg 2 – Domän (ren Kotlin, inga ramverksimporter)


**`src/main/kotlin/se/exempel/avstamning/domain/Kallsystem.kt`**

```kotlin
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
```

**`src/main/kotlin/se/exempel/avstamning/domain/Upptagsuppgifter.kt`**

```kotlin
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
```

**`src/main/kotlin/se/exempel/avstamning/domain/Kontrolluppdrag.kt`**

```kotlin
package se.exempel.avstamning.domain

import java.util.UUID

/** En köpost: kontrollera upptaget som har [kallreferens] i [kallsystem]. */
data class Kontrolluppdrag(
    val id: UUID,
    val kallreferens: String,
    val kallsystem: Kallsystem,
)
```

**`src/main/kotlin/se/exempel/avstamning/domain/Felkod.kt`**

```kotlin
package se.exempel.avstamning.domain

import java.time.Instant
import java.util.UUID

/** Kodnummer 1–20 är reserverade för upptagskontrollen. */
enum class Felkod(val nummer: Int, val beskrivning: String) {
    UPPTAGSID_AVVIKER(1, "Tjänsten anger ett annat upptagsid än källsystemet"),
    PERSONID_AVVIKER(2, "Tjänsten anger en annan person än källsystemet"),
    NAMN_AVVIKER(3, "Tjänsten anger ett annat namn än källsystemet");

    companion object {
        fun franNummer(nummer: Int): Felkod = entries.single { it.nummer == nummer }
    }
}

data class RegistreradFelkod(
    val id: UUID,
    val felkod: Felkod,
    val tjanst: Upptagstjanst,
    val uppdragId: UUID,
    val kallreferens: String,
    val upptagsId: String,
    val tidpunkt: Instant,
)
```

**`src/main/kotlin/se/exempel/avstamning/domain/UpptagKontroll.kt`**

```kotlin
package se.exempel.avstamning.domain

import java.time.Instant
import java.util.UUID

/**
 * Kontrollerar ett upptag. Källsystemets uppgifter är facit:
 * varje avvikelse registreras på den tjänst som avviker.
 */
class UpptagKontroll(
    private val uppdrag: Kontrolluppdrag,
    private val tidpunkt: Instant,
) {
    fun jamfor(kallsystemet: Upptagsuppgifter, tjansterna: Tjanstuppgifter): List<RegistreradFelkod> =
        tjansterna.perTjanst().flatMap { (tjanst, uppgifter) ->
            avvikelser(uppgifter, facit = kallsystemet).map { registrera(it, tjanst, kallsystemet.upptagsId) }
        }

    private fun avvikelser(uppgifter: Upptagsuppgifter, facit: Upptagsuppgifter): List<Felkod> = buildList {
        if (uppgifter.upptagsId != facit.upptagsId) add(Felkod.UPPTAGSID_AVVIKER)
        if (uppgifter.personId != facit.personId) add(Felkod.PERSONID_AVVIKER)
        if (uppgifter.namn != facit.namn) add(Felkod.NAMN_AVVIKER)
    }

    private fun registrera(felkod: Felkod, tjanst: Upptagstjanst, upptagsId: String) = RegistreradFelkod(
        id = UUID.randomUUID(),
        felkod = felkod,
        tjanst = tjanst,
        uppdragId = uppdrag.id,
        kallreferens = uppdrag.kallreferens,
        upptagsId = upptagsId,
        tidpunkt = tidpunkt,
    )
}
```

## Steg 3 – Portar och applikationstjänster


**`src/main/kotlin/se/exempel/avstamning/app/port/Kontrollko.kt`**

```kotlin
package se.exempel.avstamning.app.port

import se.exempel.avstamning.domain.Kontrolluppdrag
import java.util.UUID

interface Kontrollko {
    fun hamtaVantande(antal: Int): List<Kontrolluppdrag>

    fun markeraKlart(uppdragId: UUID)

    fun markeraMisslyckat(uppdragId: UUID)
}
```

**`src/main/kotlin/se/exempel/avstamning/app/port/Felkodlagring.kt`**

```kotlin
package se.exempel.avstamning.app.port

import se.exempel.avstamning.domain.RegistreradFelkod

interface Felkodlagring {
    fun spara(felkoder: List<RegistreradFelkod>)
}
```

**`src/main/kotlin/se/exempel/avstamning/app/port/Upptagshamtning.kt`**

```kotlin
package se.exempel.avstamning.app.port

import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.domain.Tjanstuppgifter
import se.exempel.avstamning.domain.Upptagsuppgifter

interface Upptagshamtning {
    fun hamtaFranKallsystem(kallsystem: Kallsystem, kallreferens: String): Upptagsuppgifter

    fun hamtaFranTjanster(upptagsId: String): Tjanstuppgifter
}
```

**`src/main/kotlin/se/exempel/avstamning/app/port/FelkodQueryPort.kt`**

```kotlin
package se.exempel.avstamning.app.port

import se.exempel.avstamning.domain.RegistreradFelkod

interface FelkodQueryPort {
    fun hamtaAlla(): List<RegistreradFelkod>
}
```

**`src/main/kotlin/se/exempel/avstamning/app/service/UpptagKontrollService.kt`**

```kotlin
package se.exempel.avstamning.app.service

import org.slf4j.LoggerFactory
import se.exempel.avstamning.app.port.Felkodlagring
import se.exempel.avstamning.app.port.Kontrollko
import se.exempel.avstamning.app.port.Upptagshamtning
import se.exempel.avstamning.domain.Kontrolluppdrag
import se.exempel.avstamning.domain.RegistreradFelkod
import se.exempel.avstamning.domain.UpptagKontroll
import java.time.Clock
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService

class UpptagKontrollService(
    private val kontrollko: Kontrollko,
    private val upptagshamtning: Upptagshamtning,
    private val felkodlagring: Felkodlagring,
    private val clock: Clock,
    private val executor: ExecutorService,
    private val antalPerKorning: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Hämtar nästa omgång väntande uppdrag ur kön och kontrollerar dem parallellt. */
    fun kontrolleraVantandeUppdrag() {
        kontrollko.hamtaVantande(antalPerKorning)
            .map { uppdrag -> executor.submit(Callable { kontrollera(uppdrag) }) }
            .forEach { it.get() }
    }

    private fun kontrollera(uppdrag: Kontrolluppdrag) {
        try {
            felkodlagring.spara(felkoder(uppdrag))
            kontrollko.markeraKlart(uppdrag.id)
        } catch (e: Exception) {
            log.warn("Uppdrag {} kunde inte kontrolleras", uppdrag.id, e)
            kontrollko.markeraMisslyckat(uppdrag.id)
        }
    }

    private fun felkoder(uppdrag: Kontrolluppdrag): List<RegistreradFelkod> {
        val kallsystemet = upptagshamtning.hamtaFranKallsystem(uppdrag.kallsystem, uppdrag.kallreferens)
        val tjansterna = upptagshamtning.hamtaFranTjanster(kallsystemet.upptagsId)
        return UpptagKontroll(uppdrag, clock.instant()).jamfor(kallsystemet, tjansterna)
    }
}
```

**`src/main/kotlin/se/exempel/avstamning/app/service/FelkodService.kt`**

```kotlin
package se.exempel.avstamning.app.service

import se.exempel.avstamning.app.port.FelkodQueryPort
import se.exempel.avstamning.domain.RegistreradFelkod

class FelkodService(private val felkodQueryPort: FelkodQueryPort) {
    fun hamtaAlla(): List<RegistreradFelkod> = felkodQueryPort.hamtaAlla()
}
```

**`src/main/kotlin/se/exempel/avstamning/app/scheduler/UpptagKontrollScheduler.kt`**

```kotlin
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
```

## Steg 4 – HTTP-integration


**`src/main/kotlin/se/exempel/avstamning/integration/UpptagshamtningAdapter.kt`**

```kotlin
package se.exempel.avstamning.integration

import se.exempel.avstamning.app.port.Upptagshamtning
import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.domain.Tjanstuppgifter
import se.exempel.avstamning.domain.Upptagsuppgifter
import se.exempel.avstamning.integration.kallsystem.SystemAClient
import se.exempel.avstamning.integration.kallsystem.SystemBClient
import se.exempel.avstamning.integration.kallsystem.SystemCClient
import se.exempel.avstamning.integration.tjanst.UpptagstjanstClient
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService

class UpptagshamtningAdapter(
    private val systemA: SystemAClient,
    private val systemB: SystemBClient,
    private val systemC: SystemCClient,
    private val tjanstX: UpptagstjanstClient,
    private val tjanstY: UpptagstjanstClient,
    private val tjanstZ: UpptagstjanstClient,
    private val executor: ExecutorService,
) : Upptagshamtning {

    override fun hamtaFranKallsystem(kallsystem: Kallsystem, kallreferens: String): Upptagsuppgifter =
        when (kallsystem) {
            Kallsystem.SYSTEM_A -> systemA.hamta(kallreferens)
            Kallsystem.SYSTEM_B -> systemB.hamta(kallreferens)
            Kallsystem.SYSTEM_C -> systemC.hamta(kallreferens)
        }

    /** Alla tre anrop startas innan vi väntar på svaren. */
    override fun hamtaFranTjanster(upptagsId: String): Tjanstuppgifter {
        val x = executor.submit(Callable { tjanstX.hamta(upptagsId) })
        val y = executor.submit(Callable { tjanstY.hamta(upptagsId) })
        val z = executor.submit(Callable { tjanstZ.hamta(upptagsId) })
        return Tjanstuppgifter(tjanstX = x.get(), tjanstY = y.get(), tjanstZ = z.get())
    }
}
```

**`src/main/kotlin/se/exempel/avstamning/integration/kallsystem/SystemAClient.kt`**

```kotlin
package se.exempel.avstamning.integration.kallsystem

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import org.springframework.web.client.RestClient
import se.exempel.avstamning.domain.Upptagsuppgifter

class SystemAClient(
    private val restClient: RestClient,
    private val mapper: SystemAMapper,
) {
    fun hamta(kallreferens: String): Upptagsuppgifter {
        val response = restClient.get()
            .uri("/upptag/{id}", kallreferens)
            .retrieve()
            .body(SystemAResponse::class.java)
        return mapper.tillUppgifter(checkNotNull(response))
    }
}

data class SystemAResponse(val id: String, val upptagsId: String, val person: SystemAPerson)

data class SystemAPerson(val personnummer: String, val namn: String)

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface SystemAMapper {
    @Mapping(target = "upptagsId", source = "upptagsId")
    @Mapping(target = "personId", source = "person.personnummer")
    @Mapping(target = "namn", source = "person.namn")
    fun tillUppgifter(response: SystemAResponse): Upptagsuppgifter
}
```

**`src/main/kotlin/se/exempel/avstamning/integration/kallsystem/SystemBClient.kt`**

```kotlin
package se.exempel.avstamning.integration.kallsystem

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import org.springframework.web.client.RestClient
import se.exempel.avstamning.domain.Upptagsuppgifter

class SystemBClient(
    private val restClient: RestClient,
    private val mapper: SystemBMapper,
) {
    fun hamta(kallreferens: String): Upptagsuppgifter {
        val response = restClient.get()
            .uri { it.path("/records").queryParam("recordId", kallreferens).build() }
            .retrieve()
            .body(SystemBResponse::class.java)
        return mapper.tillUppgifter(checkNotNull(response))
    }
}

data class SystemBResponse(val recordId: String, val uptake: SystemBUptake, val subject: SystemBSubject)

data class SystemBUptake(val id: String)

data class SystemBSubject(val personalNumber: String, val fullName: String)

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface SystemBMapper {
    @Mapping(target = "upptagsId", source = "uptake.id")
    @Mapping(target = "personId", source = "subject.personalNumber")
    @Mapping(target = "namn", source = "subject.fullName")
    fun tillUppgifter(response: SystemBResponse): Upptagsuppgifter
}
```

**`src/main/kotlin/se/exempel/avstamning/integration/kallsystem/SystemCClient.kt`**

```kotlin
package se.exempel.avstamning.integration.kallsystem

import com.fasterxml.jackson.annotation.JsonProperty
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import org.springframework.web.client.RestClient
import se.exempel.avstamning.domain.Upptagsuppgifter

class SystemCClient(
    private val restClient: RestClient,
    private val mapper: SystemCMapper,
) {
    fun hamta(kallreferens: String): Upptagsuppgifter {
        val response = restClient.get()
            .uri("/upptagsdata/{localReference}", kallreferens)
            .retrieve()
            .body(SystemCResponse::class.java)
        return mapper.tillUppgifter(checkNotNull(response))
    }
}

data class SystemCResponse(
    @param:JsonProperty("local_reference") val localReference: String,
    @param:JsonProperty("upptags_id") val upptagsId: String,
    @param:JsonProperty("person_id") val personId: String,
    @param:JsonProperty("full_name") val fullName: String,
)

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface SystemCMapper {
    @Mapping(target = "upptagsId", source = "upptagsId")
    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "namn", source = "fullName")
    fun tillUppgifter(response: SystemCResponse): Upptagsuppgifter
}
```

**`src/main/kotlin/se/exempel/avstamning/integration/tjanst/UpptagstjanstClient.kt`**

```kotlin
package se.exempel.avstamning.integration.tjanst

import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy
import org.springframework.web.client.RestClient
import se.exempel.avstamning.domain.Upptagsuppgifter

/** De tre tjänsterna har samma API-kontrakt; en instans per tjänst. */
class UpptagstjanstClient(
    private val restClient: RestClient,
    private val mapper: UpptagstjanstMapper,
) {
    fun hamta(upptagsId: String): Upptagsuppgifter {
        val response = restClient.get()
            .uri("/upptag/{upptagsId}", upptagsId)
            .retrieve()
            .body(UpptagstjanstResponse::class.java)
        return mapper.tillUppgifter(checkNotNull(response))
    }
}

data class UpptagstjanstResponse(val upptagsId: String, val personId: String, val namn: String)

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface UpptagstjanstMapper {
    fun tillUppgifter(response: UpptagstjanstResponse): Upptagsuppgifter
}
```

## Steg 5 – Persistens


**`src/main/kotlin/se/exempel/avstamning/integration/persistence/QueEntity.kt`**

```kotlin
package se.exempel.avstamning.integration.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import se.exempel.avstamning.domain.Kallsystem
import se.exempel.avstamning.domain.Kontrolluppdrag
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "que")
class QueEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID,

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    var kallreferens: String,

    @Column(nullable = false, updatable = false, length = 40)
    var topic: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: QueStatus,

    @Column(nullable = false, updatable = false, columnDefinition = "timestamptz")
    var timestamp: Instant,
) {
    fun tillUppdrag() = Kontrolluppdrag(id, kallreferens, Kallsystem.franTopic(topic))
}

enum class QueStatus { VANTAR, KLART, MISSLYCKAT }
```

**`src/main/kotlin/se/exempel/avstamning/integration/persistence/QueRepository.kt`**

```kotlin
package se.exempel.avstamning.integration.persistence

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface QueRepository : JpaRepository<QueEntity, UUID> {
    fun findByStatusOrderByTimestampAscIdAsc(status: QueStatus, limit: Limit): List<QueEntity>
}
```

**`src/main/kotlin/se/exempel/avstamning/integration/persistence/QuePersistenceAdapter.kt`**

```kotlin
package se.exempel.avstamning.integration.persistence

import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import se.exempel.avstamning.app.port.Kontrollko
import se.exempel.avstamning.domain.Kontrolluppdrag
import java.util.UUID

@Repository
class QuePersistenceAdapter(
    private val repository: QueRepository,
) : Kontrollko {
    @Transactional(readOnly = true)
    override fun hamtaVantande(antal: Int): List<Kontrolluppdrag> =
        repository.findByStatusOrderByTimestampAscIdAsc(QueStatus.VANTAR, Limit.of(antal))
            .map(QueEntity::tillUppdrag)

    @Transactional
    override fun markeraKlart(uppdragId: UUID) {
        andraStatus(uppdragId, QueStatus.KLART)
    }

    @Transactional
    override fun markeraMisslyckat(uppdragId: UUID) {
        andraStatus(uppdragId, QueStatus.MISSLYCKAT)
    }

    private fun andraStatus(uppdragId: UUID, status: QueStatus) {
        repository.findById(uppdragId).orElseThrow().status = status
    }
}
```

**`src/main/kotlin/se/exempel/avstamning/integration/persistence/FelkodEntity.kt`**

```kotlin
package se.exempel.avstamning.integration.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import se.exempel.avstamning.domain.Felkod
import se.exempel.avstamning.domain.RegistreradFelkod
import se.exempel.avstamning.domain.Upptagstjanst
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "felkoder")
class FelkodEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "kod_nummer", nullable = false, updatable = false)
    var kodNummer: Int,

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    var beskrivning: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    var tjanst: Upptagstjanst,

    @Column(name = "uppdrag_id", nullable = false, updatable = false)
    var uppdragId: UUID,

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    var kallreferens: String,

    @Column(name = "upptags_id", nullable = false, updatable = false, columnDefinition = "text")
    var upptagsId: String,

    @Column(nullable = false, updatable = false, columnDefinition = "timestamptz")
    var tidpunkt: Instant,
) {
    fun tillFelkod() = RegistreradFelkod(
        id = id,
        felkod = Felkod.franNummer(kodNummer),
        tjanst = tjanst,
        uppdragId = uppdragId,
        kallreferens = kallreferens,
        upptagsId = upptagsId,
        tidpunkt = tidpunkt,
    )

    companion object {
        fun fran(felkod: RegistreradFelkod) = FelkodEntity(
            id = felkod.id,
            kodNummer = felkod.felkod.nummer,
            beskrivning = felkod.felkod.beskrivning,
            tjanst = felkod.tjanst,
            uppdragId = felkod.uppdragId,
            kallreferens = felkod.kallreferens,
            upptagsId = felkod.upptagsId,
            tidpunkt = felkod.tidpunkt,
        )
    }
}
```

**`src/main/kotlin/se/exempel/avstamning/integration/persistence/FelkodRepository.kt`**

```kotlin
package se.exempel.avstamning.integration.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FelkodRepository : JpaRepository<FelkodEntity, UUID> {
    fun findAllByOrderByTidpunktDescIdAsc(): List<FelkodEntity>
}
```

**`src/main/kotlin/se/exempel/avstamning/integration/persistence/FelkodPersistenceAdapter.kt`**

```kotlin
package se.exempel.avstamning.integration.persistence

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import se.exempel.avstamning.app.port.FelkodQueryPort
import se.exempel.avstamning.app.port.Felkodlagring
import se.exempel.avstamning.domain.RegistreradFelkod

@Repository
class FelkodPersistenceAdapter(
    private val repository: FelkodRepository,
) : Felkodlagring, FelkodQueryPort {
    @Transactional
    override fun spara(felkoder: List<RegistreradFelkod>) {
        repository.saveAll(felkoder.map(FelkodEntity::fran))
    }

    @Transactional(readOnly = true)
    override fun hamtaAlla(): List<RegistreradFelkod> =
        repository.findAllByOrderByTidpunktDescIdAsc().map(FelkodEntity::tillFelkod)
}
```

## Steg 6 – API, konfiguration och start


**`src/main/kotlin/se/exempel/avstamning/api/controller/FelkodController.kt`**

```kotlin
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
```

**`src/main/kotlin/se/exempel/avstamning/api/dto/FelkodResponse.kt`**

```kotlin
package se.exempel.avstamning.api.dto

import se.exempel.avstamning.domain.RegistreradFelkod
import java.time.Instant
import java.util.UUID

data class FelkodResponse(
    val id: UUID,
    val kodNummer: Int,
    val beskrivning: String,
    val tjanst: String,
    val uppdragId: UUID,
    val kallreferens: String,
    val upptagsId: String,
    val tidpunkt: Instant,
) {
    companion object {
        fun fran(felkod: RegistreradFelkod) = FelkodResponse(
            id = felkod.id,
            kodNummer = felkod.felkod.nummer,
            beskrivning = felkod.felkod.beskrivning,
            tjanst = felkod.tjanst.name,
            uppdragId = felkod.uppdragId,
            kallreferens = felkod.kallreferens,
            upptagsId = felkod.upptagsId,
            tidpunkt = felkod.tidpunkt,
        )
    }
}
```

**`src/main/kotlin/se/exempel/avstamning/app/config/UpptagKontrollProperties.kt`**

```kotlin
package se.exempel.avstamning.app.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties("upptagskontroll")
data class UpptagKontrollProperties(
    val fixedDelay: Duration,
    val initialDelay: Duration,
    val antalPerKorning: Int,
    val http: Http,
    val kallsystem: Kallsystemadresser,
    val tjanster: Tjanstadresser,
) {
    data class Http(val connectTimeout: Duration, val readTimeout: Duration)

    data class Kallsystemadresser(val systemAUrl: URI, val systemBUrl: URI, val systemCUrl: URI)

    data class Tjanstadresser(val tjanstXUrl: URI, val tjanstYUrl: URI, val tjanstZUrl: URI)
}
```

**`src/main/kotlin/se/exempel/avstamning/app/config/ApplicationConfig.kt`**

```kotlin
package se.exempel.avstamning.app.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestClient
import se.exempel.avstamning.app.port.FelkodQueryPort
import se.exempel.avstamning.app.port.Felkodlagring
import se.exempel.avstamning.app.port.Kontrollko
import se.exempel.avstamning.app.port.Upptagshamtning
import se.exempel.avstamning.app.service.FelkodService
import se.exempel.avstamning.app.service.UpptagKontrollService
import se.exempel.avstamning.integration.UpptagshamtningAdapter
import se.exempel.avstamning.integration.kallsystem.SystemAClient
import se.exempel.avstamning.integration.kallsystem.SystemAMapper
import se.exempel.avstamning.integration.kallsystem.SystemBClient
import se.exempel.avstamning.integration.kallsystem.SystemBMapper
import se.exempel.avstamning.integration.kallsystem.SystemCClient
import se.exempel.avstamning.integration.kallsystem.SystemCMapper
import se.exempel.avstamning.integration.tjanst.UpptagstjanstClient
import se.exempel.avstamning.integration.tjanst.UpptagstjanstMapper
import java.net.URI
import java.time.Clock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(UpptagKontrollProperties::class)
class ApplicationConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    /** Virtuella trådar: varje uppdrag och varje tjänsteanrop får en egen tråd. */
    @Bean(destroyMethod = "close")
    fun executor(): ExecutorService = Executors.newVirtualThreadPerTaskExecutor()

    @Bean
    fun upptagKontrollService(
        kontrollko: Kontrollko,
        upptagshamtning: Upptagshamtning,
        felkodlagring: Felkodlagring,
        clock: Clock,
        executor: ExecutorService,
        properties: UpptagKontrollProperties,
    ) = UpptagKontrollService(kontrollko, upptagshamtning, felkodlagring, clock, executor, properties.antalPerKorning)

    @Bean
    fun felkodService(felkodQueryPort: FelkodQueryPort) = FelkodService(felkodQueryPort)

    @Bean
    fun upptagshamtning(
        builder: RestClient.Builder,
        properties: UpptagKontrollProperties,
        executor: ExecutorService,
        systemAMapper: SystemAMapper,
        systemBMapper: SystemBMapper,
        systemCMapper: SystemCMapper,
        tjanstMapper: UpptagstjanstMapper,
    ): Upptagshamtning {
        fun klient(url: URI) = restClient(builder, url, properties.http)
        val kallsystem = properties.kallsystem
        val tjanster = properties.tjanster
        return UpptagshamtningAdapter(
            systemA = SystemAClient(klient(kallsystem.systemAUrl), systemAMapper),
            systemB = SystemBClient(klient(kallsystem.systemBUrl), systemBMapper),
            systemC = SystemCClient(klient(kallsystem.systemCUrl), systemCMapper),
            tjanstX = UpptagstjanstClient(klient(tjanster.tjanstXUrl), tjanstMapper),
            tjanstY = UpptagstjanstClient(klient(tjanster.tjanstYUrl), tjanstMapper),
            tjanstZ = UpptagstjanstClient(klient(tjanster.tjanstZUrl), tjanstMapper),
            executor = executor,
        )
    }

    private fun restClient(builder: RestClient.Builder, baseUrl: URI, http: UpptagKontrollProperties.Http): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(http.connectTimeout)
            setReadTimeout(http.readTimeout)
        }
        return builder.clone().baseUrl(baseUrl.toString()).requestFactory(requestFactory).build()
    }
}
```

**`src/main/kotlin/se/exempel/avstamning/AvstamningsApplication.kt`**

```kotlin
package se.exempel.avstamning

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AvstamningsApplication

fun main(args: Array<String>) {
    runApplication<AvstamningsApplication>(*args)
}
```

## Steg 7 – Resurser och databas


**`src/main/resources/application.yml`**

```yaml
spring:
  application:
    name: avstamning
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/felkoder}
    username: ${POSTGRES_USER:avstamning}
    password: ${POSTGRES_PASSWORD}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
  task:
    scheduling:
      pool:
        size: 1

server:
  port: ${SERVER_PORT:8080}

upptagskontroll:
  fixed-delay: ${UPPTAGSKONTROLL_FIXED_DELAY:PT30S}
  initial-delay: ${UPPTAGSKONTROLL_INITIAL_DELAY:PT5S}
  antal-per-korning: ${UPPTAGSKONTROLL_ANTAL_PER_KORNING:10}
  http:
    connect-timeout: ${HTTP_CONNECT_TIMEOUT:PT2S}
    read-timeout: ${HTTP_READ_TIMEOUT:PT3S}
  kallsystem:
    system-a-url: ${SYSTEM_A_URL:http://localhost:8081}
    system-b-url: ${SYSTEM_B_URL:http://localhost:8082}
    system-c-url: ${SYSTEM_C_URL:http://localhost:8083}
  tjanster:
    tjanst-x-url: ${TJANST_X_URL:http://localhost:8084}
    tjanst-y-url: ${TJANST_Y_URL:http://localhost:8085}
    tjanst-z-url: ${TJANST_Z_URL:http://localhost:8086}
```

**`src/main/resources/db/changelog/db.changelog-master.yaml`**

```yaml
databaseChangeLog:
  - include:
      file: changes/001-skapa-tabeller.sql
      relativeToChangelogFile: true
```

**`src/main/resources/db/changelog/changes/001-skapa-tabeller.sql`**

```sql
--liquibase formatted sql

--changeset upptagskontroll:001-skapa-tabeller
CREATE TABLE que (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kallreferens TEXT NOT NULL,
    topic VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VANTAR',
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT que_topic CHECK (topic IN ('system-a', 'system-b', 'system-c')),
    CONSTRAINT que_status CHECK (status IN ('VANTAR', 'KLART', 'MISSLYCKAT'))
);

CREATE INDEX que_status_timestamp_id_idx ON que (status, timestamp, id);

CREATE TABLE felkoder (
    id UUID PRIMARY KEY,
    kod_nummer INTEGER NOT NULL,
    beskrivning TEXT NOT NULL,
    tjanst VARCHAR(20) NOT NULL,
    uppdrag_id UUID NOT NULL REFERENCES que (id),
    kallreferens TEXT NOT NULL,
    upptags_id TEXT NOT NULL,
    tidpunkt TIMESTAMPTZ NOT NULL,
    CONSTRAINT felkoder_kod_nummer CHECK (kod_nummer BETWEEN 1 AND 20),
    CONSTRAINT felkoder_tjanst CHECK (tjanst IN ('TJANST_X', 'TJANST_Y', 'TJANST_Z'))
);

CREATE INDEX felkoder_tidpunkt_id_idx ON felkoder (tidpunkt DESC, id ASC);
```

## Steg 8 – Lokal demomiljö

Skapa `.gitignore` med raderna `target/`, `.idea/`, `.env`.

**`compose.yml`**

```yaml
services:
  postgres:
    image: postgres:17.6-alpine
    environment:
      POSTGRES_DB: felkoder
      POSTGRES_USER: ${POSTGRES_USER:-avstamning}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Kopiera .env.example till .env}
    ports:
      - "127.0.0.1:${POSTGRES_PORT:-5432}:5432"
    volumes:
      - felkoder-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d felkoder"]
      interval: 5s
      timeout: 3s
      retries: 10

  system-a:
    image: wiremock/wiremock:3.13.2
    ports:
      - "127.0.0.1:${SYSTEM_A_PORT:-8081}:8080"
    volumes:
      - ./demo/system-a/mappings:/home/wiremock/mappings:ro

  system-b:
    image: wiremock/wiremock:3.13.2
    ports:
      - "127.0.0.1:${SYSTEM_B_PORT:-8082}:8080"
    volumes:
      - ./demo/system-b/mappings:/home/wiremock/mappings:ro

  system-c:
    image: wiremock/wiremock:3.13.2
    ports:
      - "127.0.0.1:${SYSTEM_C_PORT:-8083}:8080"
    volumes:
      - ./demo/system-c/mappings:/home/wiremock/mappings:ro

  tjanst-x:
    image: wiremock/wiremock:3.13.2
    ports:
      - "127.0.0.1:${TJANST_X_PORT:-8084}:8080"
    volumes:
      - ./demo/tjanst-x/mappings:/home/wiremock/mappings:ro

  tjanst-y:
    image: wiremock/wiremock:3.13.2
    ports:
      - "127.0.0.1:${TJANST_Y_PORT:-8085}:8080"
    volumes:
      - ./demo/tjanst-y/mappings:/home/wiremock/mappings:ro

  tjanst-z:
    image: wiremock/wiremock:3.13.2
    ports:
      - "127.0.0.1:${TJANST_Z_PORT:-8086}:8080"
    volumes:
      - ./demo/tjanst-z/mappings:/home/wiremock/mappings:ro

volumes:
  felkoder-data:
```

**`.env.example`**

```bash
# Enbart lokala demovarden. Kopiera till .env och byt vid behov.
POSTGRES_USER=avstamning
POSTGRES_PASSWORD=lokalt-demo-losenord
POSTGRES_PORT=5432
SYSTEM_A_PORT=8081
SYSTEM_B_PORT=8082
SYSTEM_C_PORT=8083
TJANST_X_PORT=8084
TJANST_Y_PORT=8085
TJANST_Z_PORT=8086
```

**`demo/lagg-till-uppdrag.sql`**

```sql
-- U-1: allt stämmer               -> inga felkoder
-- U-2: namn och person avviker     -> felkod 3 (tjänst Y) och 2 (tjänst Z)
-- U-3: annat upptagsid             -> felkod 1 (tjänst Y)
INSERT INTO que (kallreferens, topic)
VALUES ('A-1', 'system-a'), ('B-2', 'system-b'), ('C-3', 'system-c');
```

**`demo/system-a/mappings/upptag.json`**

```json
{"mappings":[{"request":{"method":"GET","urlPath":"/upptag/A-1"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"id":"A-1","upptagsId":"U-1","person":{"personnummer":"19800101-1111","namn":"Anna Andersson"}}}}]}
```

**`demo/system-b/mappings/upptag.json`**

```json
{"mappings":[{"request":{"method":"GET","urlPath":"/records","queryParameters":{"recordId":{"equalTo":"B-2"}}},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"recordId":"B-2","uptake":{"id":"U-2"},"subject":{"personalNumber":"19750505-2222","fullName":"Bo Berg"}}}}]}
```

**`demo/system-c/mappings/upptag.json`**

```json
{"mappings":[{"request":{"method":"GET","urlPath":"/upptagsdata/C-3"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"local_reference":"C-3","upptags_id":"U-3","person_id":"19900909-3333","full_name":"Cia Carlsson"}}}]}
```

**`demo/tjanst-x/mappings/upptag.json`**

```json
{"mappings":[{"request":{"method":"GET","urlPath":"/upptag/U-1"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"upptagsId":"U-1","personId":"19800101-1111","namn":"Anna Andersson"}}},{"request":{"method":"GET","urlPath":"/upptag/U-2"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"upptagsId":"U-2","personId":"19750505-2222","namn":"Bo Berg"}}},{"request":{"method":"GET","urlPath":"/upptag/U-3"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"upptagsId":"U-3","personId":"19900909-3333","namn":"Cia Carlsson"}}}]}
```

**`demo/tjanst-y/mappings/upptag.json`**

```json
{"mappings":[{"request":{"method":"GET","urlPath":"/upptag/U-1"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"upptagsId":"U-1","personId":"19800101-1111","namn":"Anna Andersson"}}},{"request":{"method":"GET","urlPath":"/upptag/U-2"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"upptagsId":"U-2","personId":"19750505-2222","namn":"Bo Bergström"}}},{"request":{"method":"GET","urlPath":"/upptag/U-3"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"upptagsId":"U-33","personId":"19900909-3333","namn":"Cia Carlsson"}}}]}
```

**`demo/tjanst-z/mappings/upptag.json`**

```json
{"mappings":[{"request":{"method":"GET","urlPath":"/upptag/U-1"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"upptagsId":"U-1","personId":"19800101-1111","namn":"Anna Andersson"}}},{"request":{"method":"GET","urlPath":"/upptag/U-2"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"upptagsId":"U-2","personId":"19750505-9999","namn":"Bo Berg"}}},{"request":{"method":"GET","urlPath":"/upptag/U-3"},"response":{"status":200,"headers":{"Content-Type":"application/json"},"jsonBody":{"upptagsId":"U-3","personId":"19900909-3333","namn":"Cia Carlsson"}}}]}
```

## Steg 9 – Kontrollera att allt fungerar

```bash
./mvnw clean package
cp .env.example .env && docker compose up -d
set -a; source .env; set +a; ./mvnw spring-boot:run
# i en ny terminal:
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < demo/lagg-till-uppdrag.sql
sleep 10; curl localhost:8080/api/felkoder
```

Rätt resultat är exakt tre felkoder:

| kallreferens | tjanst | kodNummer |
| --- | --- | --- |
| B-2 | TJANST_Y | 3 |
| B-2 | TJANST_Z | 2 |
| C-3 | TJANST_Y | 1 |

Alla tre poster i `que` ska ha status `KLART`.
