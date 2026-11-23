CREATE TABLE felkoder (
    id UUID PRIMARY KEY,
    kod VARCHAR(40) NOT NULL,
    system_id VARCHAR(20) NOT NULL,
    ref_id TEXT NOT NULL,
    jamforelse_id UUID NOT NULL,
    affarsreferens TEXT NOT NULL,
    falt VARCHAR(20) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    CONSTRAINT felkoder_kod CHECK (kod IN ('AVVIKANDE_FALT')),
    CONSTRAINT felkoder_system CHECK (system_id IN ('SYSTEM_A', 'SYSTEM_B', 'SYSTEM_C')),
    CONSTRAINT felkoder_falt CHECK (falt IN ('NAMN', 'EPOST')),
    CONSTRAINT felkoder_ref_id CHECK (length(btrim(ref_id)) > 0),
    CONSTRAINT felkoder_affarsreferens CHECK (length(btrim(affarsreferens)) > 0),
    CONSTRAINT felkoder_jamforelse_falt_system UNIQUE (jamforelse_id, falt, system_id)
);

CREATE INDEX felkoder_timestamp_id_idx ON felkoder (timestamp DESC, id ASC);
