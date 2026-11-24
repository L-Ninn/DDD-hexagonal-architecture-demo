# Upptagskontroll

Kotlin-demo av **DDD och hexagonal arkitektur**. Ett schemalagt jobb kontrollerar att upptag (på personer) stämmer mellan ett källsystem och tre tjänster. Avvikelser sparas som felkoder.

## Flöde

1. Jobbet hämtar upp till 10 väntande poster ur kötabellen `que` (`kallreferens` + `topic`).
2. Topicen väljer källsystem A, B eller C. Källsystemet ger upptagsid, personid och namn.
3. Tjänst X, Y och Z anropas parallellt med upptagsid:t.
4. `UpptagKontroll` jämför tjänsterna mot källsystemet (facit) och skapar felkoder.
5. Felkoderna sparas och posten blir `KLART`. Fel ger `MISSLYCKAT`.

| Felkod | Beskrivning |
| --- | --- |
| 1 | Tjänsten anger ett annat upptagsid än källsystemet |
| 2 | Tjänsten anger en annan person än källsystemet |
| 3 | Tjänsten anger ett annat namn än källsystemet |

## Struktur

```text
domain        UpptagKontroll, Felkod, Kallsystem, Upptagstjanst, Upptagsuppgifter
app           Portar, UpptagKontrollService, scheduler och Spring-konfiguration
integration   HTTP-klienter (källsystem och tjänster) och JPA-adaptrar
api           GET /api/felkoder
```

## Kör

Kräver JDK 21 och Docker Compose.

```bash
cp .env.example .env && docker compose up -d
set -a; source .env; set +a; ./mvnw spring-boot:run
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < demo/lagg-till-uppdrag.sql
curl localhost:8080/api/felkoder
```

Förväntat: `A-1` inga felkoder, `B-2` felkod 3 (Y) och 2 (Z), `C-3` felkod 1 (Y).

## Mer

- [OVERLAMNING.md](OVERLAMNING.md) – status och nästa steg
- [AI-PROMPT.md](AI-PROMPT.md) – prompt för att återskapa projektet som en fristående Spring Boot-app mot MySQL
