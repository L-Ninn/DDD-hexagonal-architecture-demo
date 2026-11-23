# Avstämning av tre källsystem

En liten Kotlin-mikrotjänst som hämtar kunduppgifter från exakt tre separata HTTP-API:er, jämför dem och sparar avvikelser i PostgreSQL. Ett schemalagt jobb driver avstämningen. `GET /api/felkoder` visar samtliga redan sparade felregistreringar och startar ingen ny jämförelse.

Demons verksamhetsantagande är att samma kund har ett gemensamt **kundnummer** i alla tre systemen, men ett eget lokalt `refId` i varje system. Endast **namn** och **e-post** jämförs. Detta är en föreslagen demonstrationsdomän som behöver valideras mot verksamheten inför riktig användning.

## Kör lokalt

Förutsättningar: JDK 21, Docker med Compose och internetåtkomst för första beroendehämtningen. Maven Wrapper följer med; Maven behöver inte installeras separat.

```bash
cp .env.example .env
docker compose up -d
```

Compose startar databasen `felkoder` och tre WireMock-servrar på port 8081, 8082 och 8083. De simulerar separata externa tjänster och är körbar demoinfrastruktur. PostgreSQL använder port 5432. Publicerade infrastrukturportar är bundna till localhost.

Läs in samma lokala konfiguration och starta mikrotjänsten i projektets rotkatalog:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

Applikationen använder port 8080. Flyway skapar tabellen `felkoder` i databasen med samma namn. Hibernate validerar schemat (`ddl-auto: validate`). Första jämförelsen körs efter fem sekunder; nästa körs 30 sekunder efter att den föregående avslutats. HTTP-anropen är sekventiella, med två sekunders anslutningstimeout och tre sekunders lästimeout per källa. Inga automatiska återförsök görs; nästa schemalagda körning gör ett nytt försök med ett nytt jämförelse-id.

```bash
curl http://localhost:8080/api/felkoder
```

Svaret är `200 OK` med en JSON-lista, eller `[]` när ingen avvikelse har sparats. Sorteringen är `timestamp` fallande och därefter `id` stigande. Ingen paginering används i denna lilla demo.

Bygg en körbar jar utan tester:

```bash
./mvnw clean package
java -jar target/avstamning-0.0.1-SNAPSHOT.jar
```

Projektet innehåller inga tester eller testberoenden, enligt önskemål. `maven.test.skip=true` är satt i POM. En lyckad kompilering bevisar inte HTTP-, databas- eller schemaläggningsbeteendet.

Stoppa infrastrukturen med `docker compose down`. Databasen bevaras i en namngiven volym mellan omstarter. Historiken visas därför även när en senare jämförelse är lika eller ofullständig. `docker compose down -v` raderar den lokala demodatabasen och dess historik.

## Jämförelseregler och demoscenarier

Fälten jämförs med exakt stränglikhet. Mellanslag, versaler och andra skillnader bevaras; inga värden trimmas eller normaliseras. Källsystem, lokala referenser och kundnummer används för identitet och spårbarhet, inte som jämförelsefält. Obligatoriska uppgifter måste finnas och innehålla annat än blanktecken.

Ingen källa är facit. Om ett fält skiljer sig sparas **en rad för varje involverat system**, även när två av tre har samma värde. Därmed anges ingen källa som den felaktiga. Alla rader från samma körning har samma `jamforelseId` och tidsstämpel. Kodvärdet `AVVIKANDE_FALT` bestäms av domänen. Själva namn- och e-postvärdena sparas inte i felregistret.

| Kundnummer | Underlag i demon | Utlåtande | Nya rader per körning |
| --- | --- | --- | --- |
| `KUND-100` | Alla har samma namn; C har en annan e-post än A och B | `OLIKA` | 3 rader med `falt=EPOST`, en per system |
| `KUND-200` | Alla tre har samma namn och e-post, men olika lokala id:n | `LIKA` | 0 |
| `KUND-300` | C svarar 404; A och B har dessutom olika namn | `OFULLSTANDIGT` | 0 |

Byt scenario genom att stoppa applikationen och ange en annan kundreferens innan nästa start:

```bash
export AVSTAMNING_AFFARSREFERENS=KUND-200
./mvnw spring-boot:run
```

Byt till `KUND-300` på samma sätt för saknat underlag. En stoppad simulerad källa, timeout, andra HTTP-fel, tomt svar, ogiltigt svar eller fel kundnummer ger också `OFULLSTANDIGT`. Ofullständigt underlag ger inga avvikelseposter, även om två tillgängliga källor skiljer sig. Orsaken loggas separat per källa. Tidigare felregistreringar raderas inte.

Varje körning loggar `jamforelseId` och utlåtande. `OLIKA` loggar även antalet sparade rader. Fel i lagringen loggas som en misslyckad körning, utan ett påstående om att avvikelser sparats. Namn och e-post skrivs inte till avstämningsloggarna.

## Konfiguration och riktiga API:er

Miljövariabler åsidosätter standardvärdena i `application.yml`:

| Variabel | Standard eller innebörd |
| --- | --- |
| `POSTGRES_USER` | `avstamning` |
| `POSTGRES_PASSWORD` | Måste anges; `.env.example` innehåller endast ett lokalt demolösenord |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/felkoder` |
| `SYSTEM_A_URL` | `http://localhost:8081` |
| `SYSTEM_B_URL` | `http://localhost:8082` |
| `SYSTEM_C_URL` | `http://localhost:8083` |
| `AVSTAMNING_AFFARSREFERENS` | `KUND-100` |
| `AVSTAMNING_FIXED_DELAY` | `PT30S` |
| `AVSTAMNING_INITIAL_DELAY` | `PT5S` |
| `HTTP_CONNECT_TIMEOUT` | `PT2S` |
| `HTTP_READ_TIMEOUT` | `PT3S` |
| `SERVER_PORT` | `8080` |

`POSTGRES_PORT` och `SYSTEM_A_PORT`/`SYSTEM_B_PORT`/`SYSTEM_C_PORT` i `.env` styr Compose-portarna. Om de ändras måste motsvarande `DATABASE_URL` och `SYSTEM_*_URL` också anges för applikationen. Använd externa miljövariabler eller en hemlighetstjänst för riktiga lösenord; `.env` versionshanteras inte.

Byt `SYSTEM_A_URL`, `SYSTEM_B_URL` och `SYSTEM_C_URL` till de riktiga API:ernas basadresser. De nuvarande kontrakten är:

| Källa | HTTP-anrop | Externa fält |
| --- | --- | --- |
| A | `GET /customers/{kundnummer}` | `id`, `customerNumber`, `name`, `email` |
| B | `GET /records?customerNumber={kundnummer}` | `recordId`, `customer.number`, `customer.displayName`, `customer.contact.emailAddress` |
| C | `GET /customer-data/{kundnummer}` | `local_reference`, `business_reference`, `full_name`, `email_address` |

Om riktiga API:er har andra kontrakt ändras respektive `System*Response`, `System*Client` och MapStruct-mapper i integrationen. Domänmodellen och applikationstjänsterna behåller sitt kontrakt. Lägg eventuell källspecifik autentisering på respektive HTTP-klient och läs hemligheter från miljön. GET-endpointen saknar autentisering i denna lokala demo.

## Struktur och gränser

```text
api/controller, api/dto             Inkommande HTTP-adapter och response-mappning
app/service                        Vanliga Kotlin-klasser för orkestrering och läsning
app/port                           Utgående kontrakt som applikationen äger
app/config                         Spring-wiring, käll-URL:er och timeouter
app/scheduler                      Tekniskt undantag i app-paketet: Spring @Scheduled
domain                             Ren Kotlin: underlag, jämförelseregler och utfall
integration/systema, systemb, systemc  Tre HTTP-klienter, externa DTO:er och mappers
integration/persistence            JPA-entitet, repository, mapper och adapter
```

Det avgränsade verksamhetsområdet är avstämning av kunduppgifter. Tjänsten äger avvikelseregistret men ingen kundmasterdata. MapStruct översätter alla tre externa formaten till `Kallunderlag` och även mellan domän, persistens och response-DTO. HTTP-DTO:er och JPA-entiteter passerar aldrig in i kärnan. Domänen använder varken Spring, JPA eller MapStruct. Samtliga faktiska tjänster har suffixet `Service`.

`AvstamningsService` hämtar alla tre underlagen utan databastransaktion, låter `Jamforelse` avgöra utfallet och sparar vid behov via `Avvikelselagring`. Spring-transaktionen ligger på persistensadaptern: alla felregistreringar för samma jämförelse sparas atomiskt. Tabellens unika villkor för `(jamforelse_id, falt, system_id)` hindrar dubbla registreringar inom en körning. Nya körningar får nya UUID:n och kan därför spara nya historikrader för samma kvarstående avvikelse.

Demotjänsten ska köras i **en instans**. Schemaläggaren använder en tråd och `fixedDelay`, så nästa körning startar efter föregående avslut. Ingen distribuerad låsning finns. Innan flera repliker används behövs ett medvetet beslut om ensam jobbägare eller distribuerad schemaläggning.

## Versionsval

Java 21, Spring Boot 3.5.7, Kotlin 1.9.25 (Boots dependency management), MapStruct 1.6.3 med kapt, Hibernate 6.6.33.Final, Flyway 11.7.2, PostgreSQL 17.6 och WireMock 3.13.2. Spring Boot hanterar Kotlin-, Hibernate-, Flyway- och JDBC-versionerna. Kotlin-kompilatorns Spring/JPA-plugins öppnar ramverksklasser och skapar JPA-konstruktorn; domänens dataklasser är immutabla. Endast MapStructs genererade implementationer är Java-kod.

Versions- och konfigurationsunderlag: [Spring Boot 3.5.7](https://spring.io/blog/2025/10/23/spring-boot-3-5-7-available-now/), [Spring Boots Kotlin-stöd](https://docs.spring.io/spring-boot/3.5/reference/features/kotlin.html), [Kotlin kapt med Maven](https://kotlinlang.org/docs/kapt.html), [MapStruct 1.6.3](https://mapstruct.org/documentation/stable/reference/html/), [PostgreSQL 17 constraints](https://www.postgresql.org/docs/17/ddl-constraints.html) och [WireMock i Docker](https://wiremock.org/docs/standalone/docker/).
