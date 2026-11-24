# Upptagskontroll

En liten Kotlin-mikrotjänst som visar **DDD och hexagonal arkitektur**. Ett schemalagt jobb läser upptag (på personer) från en kötabell, hämtar upptaget från rätt källsystem, slår upp det i tre tjänster och registrerar felkoder när uppgifterna inte stämmer. `GET /api/felkoder` visar alla sparade felkoder.

Loggning och felhantering är medvetet minimala; fokus ligger på designen.

## Flödet

1. `UpptagKontrollScheduler` triggar `UpptagKontrollService.kontrolleraVantandeUppdrag()`.
2. Servicen hämtar upp till **10** väntande köposter (`antal-per-korning`). Varje post har en **källreferens** och ett **topic**.
3. Topicen avgör **källsystemet** (`system-a` → `SYSTEM_A` osv.). Källsystemet svarar med upptagets **upptagsid**, **personid** och **namn**.
4. Upptagsid:t används mot **tjänst X, Y och Z**, som anropas parallellt.
5. Domänobjektet `UpptagKontroll` jämför tjänsternas uppgifter med källsystemets (källsystemet är facit) och skapar felkoder.
6. Felkoderna sparas och köposten markeras `KLART`. Alla fel – även att ett system saknar upptaget (404) – ger `MISSLYCKAT`.

Köposterna körs parallellt på virtuella trådar.

## Felkoder

Felkoden registreras på tjänsten som avviker från källsystemet.

| Nr | Beskrivning |
| --- | --- |
| 1 | Tjänsten anger ett annat upptagsid än källsystemet |
| 2 | Tjänsten anger en annan person än källsystemet |
| 3 | Tjänsten anger ett annat namn än källsystemet |

Kodnummer 1–20 är reserverade (`Felkod` i domänen, check-constraint i databasen).

## Struktur

```text
domain                     Ren Kotlin: UpptagKontroll, Felkod, Kallsystem, Upptagstjanst, Upptagsuppgifter
app/port                   Kontrollko, Felkodlagring, Upptagshamtning, FelkodQueryPort – ägs av applikationen
app/service                UpptagKontrollService (orkestrering), FelkodService (läsning)
app/scheduler              Tunn @Scheduled-trigger
app/config                 Spring-wiring och konfiguration
integration                UpptagshamtningAdapter – visar exakt vilka anrop som görs
integration/kallsystem     SystemA/B/C-klienter: egna format, MapStruct till Upptagsuppgifter
integration/tjanst         UpptagstjanstClient – samma kontrakt för X, Y och Z
integration/persistence    JPA-entities, repositories och adapters för que och felkoder
api                        FelkodController och FelkodResponse
```

## Kör lokalt

Förutsättningar: JDK 21 och Docker Compose (eller motsvarande).

```bash
cp .env.example .env
docker compose up -d          # PostgreSQL + WireMock: källsystem 8081–8083, tjänster 8084–8086
set -a; source .env; set +a
./mvnw spring-boot:run
```

Lägg demouppdragen i kön:

```bash
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < demo/lagg-till-uppdrag.sql
curl http://localhost:8080/api/felkoder
```

| Köpost | Scenario | Förväntade felkoder |
| --- | --- | --- |
| `A-1` / `system-a` | Allt stämmer (U-1) | – |
| `B-2` / `system-b` | Y har annat namn, Z annan person (U-2) | 3 (Y), 2 (Z) |
| `C-3` / `system-c` | Y har annat upptagsid (U-3) | 1 (Y) |

## API-kontrakt (demo)

| System | Anrop | Fält |
| --- | --- | --- |
| System A | `GET /upptag/{kallreferens}` | `id`, `upptagsId`, `person.personnummer`, `person.namn` |
| System B | `GET /records?recordId={kallreferens}` | `recordId`, `uptake.id`, `subject.personalNumber`, `subject.fullName` |
| System C | `GET /upptagsdata/{kallreferens}` | `local_reference`, `upptags_id`, `person_id`, `full_name` |
| Tjänst X/Y/Z | `GET /upptag/{upptagsId}` | `upptagsId`, `personId`, `namn` |

Adresser och timeouts styrs i `application.yml` (prefix `upptagskontroll`).

## Databas

Liquibase-migrationen `003-upptagskontroll.sql` ersätter demons tidigare tabeller (`que`, `felkoder`). Befintlig demodata försvinner vid uppgradering.

Projektet saknar tester (`maven.test.skip=true`).
