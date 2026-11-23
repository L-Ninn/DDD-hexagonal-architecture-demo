--liquibase formatted sql

--changeset avstamning:002-skapa-que
CREATE TABLE que (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    affarsreferens TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VANTAR',
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT que_affarsreferens CHECK (length(btrim(affarsreferens)) > 0),
    CONSTRAINT que_status CHECK (status IN ('VANTAR', 'KLART', 'MISSLYCKAT'))
);

CREATE INDEX que_status_timestamp_id_idx ON que (status, timestamp, id);
