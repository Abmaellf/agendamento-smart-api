CREATE TABLE IF NOT EXISTS CLINIC (
    id BINARY(16) NOT NULL,
    code BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_clinic PRIMARY KEY (id),
    CONSTRAINT uk_clinic_code UNIQUE (code)
);

INSERT INTO CLINIC (id, code, name, created_at)
VALUES (
    UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')),
    1000001,
    'Clínica Central',
    NOW()
);

