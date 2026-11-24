# Överlämning till Codex – upptagskontroll

Skriven av Claude Code 2026-11-24. Ändringarna är committade på `codex/avstamning-demo` ovanpå `1b2d8e9`; se `git show HEAD`.

## Uppdraget (användarens vision)

- Schemalagt jobb läser upp till 10 poster ur kötabellen `que`. Varje post har ett id (`kallreferens`) och ett `topic`.
- Topicen styr vilket av **tre källsystem** som anropas. Källsystemet ger ett **upptagsid**.
- Upptagsid:t används mot **tre andra tjänster** (X, Y, Z), parallellt.
- Uppgifterna slås ihop och kontrolleras: id:n och namn ska stämma. Upptag gäller **personer**.
- Avvikelser ger felkoder med **nummer 1–20** och **beskrivning**.
- Användaren valde senare bort all hantering av upptag som saknas: ett 404-svar är ett vanligt fel och ger `MISSLYCKAT`.
- Namngivning i stil med `UpptagKontroll` (användaren valde bort "kund").
- **Prioritet: enkel kod, Clean Code och DDD. Minimal loggning och felhantering – designen ska synas.**

## Designbeslut

- **Domän (`domain/`)** – ren Kotlin.
  - `UpptagKontroll.jamfor(kallsystemet, tjansterna)` äger reglerna. Källsystemet är facit, och avvikelsen registreras på tjänsten som avviker.
  - `Felkod` är en enum med `nummer` och `beskrivning`. Felkod 1–3 används: upptagsid, person och namn.
  - `Kallsystem` (med `topic`) och `Upptagstjanst` är enkla enums.
  - `Tjanstuppgifter` har tre namngivna fält istället för en lista. Inga värden är nullbara.
- **App (`app/`)**
  - `UpptagKontrollService` orkestrerar: hämta från källsystemet, hämta från tjänsterna, jämföra och slutföra.
  - Portarna är `Kontrollko` (köstatus), `Felkodlagring`, `Upptagshamtning` och `FelkodQueryPort`.
  - Servicen sparar felkoderna och markerar sedan uppdraget `KLART` i två separata transaktioner. Användaren ville skilja kö och felkoder åt. Om processen dör mellan stegen kan uppdraget köras igen och ge dubbla felkoder; det är accepterat i demon.
  - Ett fel per uppdrag fångas, loggas på en rad och ger `MISSLYCKAT`.
- **Integration**
  - `UpptagshamtningAdapter` visar de tre källsystemen med en `when` och de tre tjänstanropen explicit.
  - Källsystemen har olika JSON-format, och MapStruct mappar dem.
  - Tjänsterna antas ha samma kontrakt, så det finns en `UpptagstjanstClient` med tre instanser.
  - Ingen särskild 404-hantering; alla HTTP-fel går vidare som undantag.
- **Parallellism** – en executor med virtuella trådar används både för köposter och för tjänstanrop. Den kan nästlas utan risk för deadlock.
- **Persistens** – `QuePersistenceAdapter` hanterar bara kön och `FelkodPersistenceAdapter` bara felkoder. Mappningen mellan entity och domän är handskriven (enkla `fran`/`tillFelkod`).
- **Databas** – changeset `003-upptagskontroll.sql` droppar och återskapar `que` och `felkoder`. Det är acceptabelt för en demo.
- MapStruct för API-response togs bort och ersattes med `FelkodResponse.fran`.

## Verifierat

- `./mvnw clean package` lyckas med JDK 21 i en podman-container. Java finns inte installerat lokalt på Bazzite.
- Ett körtest från början till slut i en podman-pod med PostgreSQL och 6 WireMock-servrar gav exakt de förväntade felkoderna för de tre demoscenarierna i README. En extra köpost som saknas i källsystemet blev `MISSLYCKAT` utan felkoder.
- `docker compose`/`podman compose` finns inte på maskinen, så själva `compose.yml` är inte testkörd. Samma containrar startades manuellt.

## Kvar att göra / öppna frågor

- **Tester saknas.** Användarens profil (`~/.codex/AGENTS.md`) föreskriver TDD. Det mest naturliga första steget är enhetstester för `UpptagKontroll.jamfor`.
- Riktiga topics, källsystem och tjänstnamn är okända. `system-a/b/c` och `TJANST_X/Y/Z` är platshållare.
- Fler felkoder (4–20) kan läggas till i `Felkod` när fler kontroller behövs.
- Det finns inget lås på köposter, så demon förutsätter **en instans**. Med flera instanser behövs `FOR UPDATE SKIP LOCKED` eller motsvarande.
- Den gamla README-sektionen "AI-prompt för att återskapa projektet" togs bort eftersom den beskrev den tidigare kunddesignen. Den finns i git-historiken.
