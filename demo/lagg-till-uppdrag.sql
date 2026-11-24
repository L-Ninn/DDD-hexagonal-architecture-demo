-- U-1: allt stämmer               -> inga felkoder
-- U-2: namn och person avviker     -> felkod 3 (tjänst Y) och 2 (tjänst Z)
-- U-3: annat upptagsid             -> felkod 1 (tjänst Y)
INSERT INTO que (kallreferens, topic)
VALUES ('A-1', 'system-a'), ('B-2', 'system-b'), ('C-3', 'system-c');
