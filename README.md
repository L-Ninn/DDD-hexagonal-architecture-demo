# Avstämning av tre källsystem

En liten Kotlin-mikrotjänst som hämtar kunduppgifter från exakt tre separata HTTP-API:er, jämför dem och sparar avvikelser i PostgreSQL. Ett schemalagt jobb läser nästa kunduppdrag från tabellen `que` och driver avstämningen. `GET /api/felkoder` visar samtliga redan sparade felregistreringar och startar ingen ny jämförelse.

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

Applikationen använder port 8080. Liquibase skapar tabellerna `felkoder` och `que` i databasen `felkoder`. Hibernate validerar schemat (`ddl-auto: validate`). Workern triggar `AvstamningsService.stamAv()` utan argument efter fem sekunder och därefter 30 sekunder efter föregående avslut. Servicen läser högst ett väntande kunduppdrag per körning. Är kön tom görs inga HTTP-anrop.

När applikationen har startat kan du lägga ett uppdrag i kön från en annan terminal:

```bash
docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
INSERT INTO que (affarsreferens) VALUES ('KUND-100');
SQL
```

Kundreferensen kommer från köposten. Servicen startar de tre HTTP-anropen parallellt i en separat trådpool med tre trådar och inväntar samtliga svar innan domänen jämför dem. Varje klient har två sekunders anslutningstimeout och tre sekunders lästimeout. Inga automatiska återförsök görs inom ett HTTP-anrop.
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

## Databasmigrationer med Liquibase

`src/main/resources/db/changelog/db.changelog-master.yaml` inkluderar migrationerna i explicit ordning. Den första filen, `changes/001-skapa-felkoder.sql`, skapar felregistret. `changes/002-skapa-que.sql` lägger till kön med constraints och index, även för databaser som redan har första changesetet. Liquibase kör nya changesets vid applikationsstart och registrerar dem i `databasechangelog`; `databasechangeloglock` samordnar migrationerna.

Lägg framtida ändringar i nya SQL-filer med egna changeset-id:n och inkludera dem sist i master-changelogen. Ändra inte redan körda changesets. Hibernate ansvarar för persistens och schemavalidering, medan Liquibase ansvarar för schemaändringarna.

Den första changelogen är avsedd för en tom databas. Om du redan har startat den tidigare Flyway-versionen känner Liquibase inte till dess migrationshistorik. Använd då en ny demodatabas eller gör en kontrollerad baslinjemarkering av enbart `001-skapa-felkoder.sql` efter att ha verifierat att det befintliga schemat motsvarar den migrationen. Markera inte det nya changesetet för `que` som kört; det måste appliceras av Liquibase. Ingen automatisk baslinjemarkering eller radering av befintliga data görs.

## Jämförelseregler och demoscenarier

Fälten jämförs med exakt stränglikhet. Mellanslag, versaler och andra skillnader bevaras; inga värden trimmas eller normaliseras. Källsystem, lokala referenser och kundnummer används för identitet och spårbarhet, inte som jämförelsefält. Obligatoriska uppgifter måste finnas och innehålla annat än blanktecken.

Ingen källa är facit. Om ett fält skiljer sig sparas **en rad för varje involverat system**, även när två av tre har samma värde. Därmed anges ingen källa som den felaktiga. Alla felrader från samma köuppdrag har samma `jamforelseId` och tidsstämpel. `jamforelseId` är köpostens `id`, så resultatet går att koppla till uppdraget. Kodvärdet `AVVIKANDE_FALT` bestäms av domänen. Själva namn- och e-postvärdena sparas inte i felregistret.

| Kundnummer | Underlag i demon | Utlåtande | Nya rader per körning |
| --- | --- | --- | --- |
| `KUND-100` | Alla har samma namn; C har en annan e-post än A och B | `OLIKA` | 3 rader med `falt=EPOST`, en per system |
| `KUND-200` | Alla tre har samma namn och e-post, men olika lokala id:n | `LIKA` | 0 |
| `KUND-300` | C svarar 404; A och B har dessutom olika namn | `OFULLSTANDIGT` | 0 |

Välj kund genom värdet i `INSERT INTO que`, eller köa alla tre demoscenarier:

```bash
docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < demo/lagg-till-uppdrag.sql
```

Varje insert skapar ett nytt uppdrag. De behandlas ett i taget, sorterade på `timestamp` och sedan `id`. Varje köpost innehåller `id`, `affarsreferens`, `status` och `timestamp`. Id, status och tidsstämpel får standardvärden i databasen.

| Status i `que` | Innebörd |
| --- | --- |
| `VANTAR` | Väntar på nästa körning |
| `KLART` | Jämförelsen är slutförd, oavsett om resultatet är `LIKA` eller `OLIKA` |
| `MISSLYCKAT` | Underlaget är ofullständigt och inga felkoder har sparats |

`KLART` och `MISSLYCKAT` väljs inte igen. För ett nytt försök lägger du en ny köpost för samma kund. En stoppad simulerad källa, timeout, HTTP-fel, tomt svar, ogiltigt svar eller fel kundnummer ger `OFULLSTANDIGT`. Även om två tillgängliga källor skiljer sig sparas då inga avvikelser. Tidigare felregistreringar raderas inte.

Om databaslagringen misslyckas rullas både felkoderna och statusändringen tillbaka. Uppdraget ligger då kvar som `VANTAR` för nästa körning. Ett oväntat fel före slutförandet eller ett avbrott under hämtningen lämnar också uppdraget väntande. Ett kvarstående tekniskt fel kan därför blockera senare uppdrag i denna enkla kö.

Servicen returnerar domänens jämförelseresultat, eller `null` om kön är tom. Köstatus och sparade felkoder visar utfallet när jobbet körs schemalagt. Servicen har ingen egen resultatloggning. Oväntade fel går vidare till Spring-schedulerns vanliga felhantering, som loggar felet och låter nästa schemalagda körning ske.

Läs köstatus via databasen:

```bash
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
SELECT id, affarsreferens, status, timestamp FROM que ORDER BY timestamp, id;
SQL
```

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
app/config                         Spring-wiring, trådpool, käll-URL:er och timeouter
app/scheduler                      Tunn Spring @Scheduled-trigger som bara anropar Service
domain                             Ren Kotlin: underlag, jämförelseregler och utfall
integration/systema, systemb, systemc  Tre HTTP-klienter, externa DTO:er och mappers
integration/persistence            JPA-entities, repositories, mappers och adapters för felkoder och que
```

Det avgränsade verksamhetsområdet är avstämning av kunduppgifter. Tjänsten äger avvikelseregistret men ingen kundmasterdata. MapStruct översätter alla tre externa formaten till `Kallunderlag` och även mellan domän, persistens och response-DTO. HTTP-DTO:er och JPA-entiteter passerar aldrig in i kärnan. Domänen använder varken Spring, JPA eller MapStruct. Samtliga faktiska tjänster har suffixet `Service`.

`AvstamningsScheduler` gör endast ett argumentlöst anrop till `AvstamningsService`. Servicen läser ett `Avstamningsuppdrag` via porten `Avstamningsko` och använder dess affärsreferens för alla tre källanrop. `QueMapper` översätter köpostens entity till domänobjektet. Ingen kundreferens finns i workern eller applikationskonfigurationen.

Servicen orkestrerar parallell hämtning och väntar på alla tre resultaten. Domänobjektet `Jamforelse` ansvarar för jämförelsereglerna; `Avvikelse` bestämmer felkoden. Trådpoolens livscykel hanteras av Spring. Servicen fångar inte `InterruptedException`: avbrottet går vidare till anroparen och ingen jämförelse görs av ett delvis hämtat underlag.

Köläsningen sker i en kort transaktion som avslutas före HTTP-anropen. `QuePersistenceAdapter.slutfor` låser den väntande köposten och sparar felkoder samt statusen `KLART` i en gemensam kort transaktion. Där anropas `Avvikelselagring`, vars transaktion ansluter till den pågående. Därmed kan köposten inte markeras klar utan att felkoderna sparats, eller tvärtom. Ofullständigt underlag uppdaterar endast statusen till `MISSLYCKAT`. Inga databaslås eller databastransaktioner hålls öppna under HTTP-anropen.

Tabellens unika villkor för `(jamforelse_id, falt, system_id)` hindrar dubbla felregistreringar inom samma uppdrag. Ett nytt uppdrag får ett nytt UUID och kan spara ny historik för samma kvarstående avvikelse.

Demotjänsten ska köras i **en instans**. Schemaläggaren använder en tråd och `fixedDelay`, så nästa jobb startar efter föregående avslut. De tre HTTP-anropen inom ett jobb körs parallellt. Köposten reserveras inte före hämtningen, så flera repliker kan göra samma externa anrop. Innan flera repliker används behövs reservation av köposter eller distribuerad schemaläggning.

## Versionsval

Java 21, Spring Boot 3.5.7, Kotlin 1.9.25 (Boots dependency management), MapStruct 1.6.3 med kapt, Hibernate 6.6.33.Final, Liquibase 4.31.1, PostgreSQL 17.6 och WireMock 3.13.2. Spring Boot hanterar Kotlin-, Hibernate-, Liquibase- och JDBC-versionerna. Kotlin-kompilatorns Spring/JPA-plugins öppnar ramverksklasser och skapar JPA-konstruktorn; domänens dataklasser är immutabla. Endast MapStructs genererade implementationer är Java-kod.

Versions- och konfigurationsunderlag: [Spring Boot 3.5.7](https://spring.io/blog/2025/10/23/spring-boot-3-5-7-available-now/), [Spring Boots Kotlin-stöd](https://docs.spring.io/spring-boot/3.5/reference/features/kotlin.html), [Liquibase med Spring Boot](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html#howto.data-initialization.migration-tool.liquibase), [Kotlin kapt med Maven](https://kotlinlang.org/docs/kapt.html), [MapStruct 1.6.3](https://mapstruct.org/documentation/stable/reference/html/), [PostgreSQL 17 constraints](https://www.postgresql.org/docs/17/ddl-constraints.html) och [WireMock i Docker](https://wiremock.org/docs/standalone/docker/).
