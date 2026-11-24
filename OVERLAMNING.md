# Överlämning

**Status:** Upptagskontrollen fungerar från början till slut. Den bygger med `./mvnw clean package` (JDK 21) och har körts mot PostgreSQL och WireMock med förväntade felkoder. Branch: `codex/avstamning-demo`.

**Användarens prioritet:** enkel kod, Clean Code och DDD. Loggning och felhantering ska vara minimal, så att designen syns.

## Designbeslut

- All affärslogik ligger i `UpptagKontroll.jamfor`. Källsystemet är facit, och felkoden registreras på tjänsten som avviker.
- Servicen orkestrerar bara: hämta, jämföra, spara felkoder, markera klart. Ett fel ger `MISSLYCKAT`.
- Kö och felkoder har separata portar och adaptrar (`Kontrollko`, `Felkodlagring`). Därför sparas de i två transaktioner, vilket är accepterat.
- Saknade upptag hanteras inte särskilt. 404 räknas som ett vanligt fel.
- Tjänst X, Y och Z delar samma klient och kontrakt. Källsystemen har var sitt format och använder MapStruct.
- Virtuella trådar används för köposter och tjänstanrop.

## Nästa steg

- Enhetstester för `UpptagKontroll` saknas. Användarens profil kräver TDD.
- Topics och system- och tjänstnamn är platshållare.
- Kön har inget lås, så bara en instans får köras.
