-- Seed exclusivo para desenvolvimento e demonstração.
INSERT INTO CLINIC (
    id,
    code,
    name,
    created_at
)
VALUES (
    UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')),
    1000001,
    'Clínica Central',
    CURRENT_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE id = id;
