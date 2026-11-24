--liquibase formatted sql

--changeset avstamning:003-upptagskontroll
DROP TABLE felkoder;
DROP TABLE que;

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
