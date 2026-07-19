CREATE TABLE books
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title               VARCHAR(255)   NOT NULL,
    price               NUMERIC(10, 2) NOT NULL,
    publication_status  VARCHAR(20)    NOT NULL DEFAULT 'UNPUBLISHED',
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CHECK (price >= 0),
    CHECK (publication_status IN ('UNPUBLISHED', 'PUBLISHED'))
);
