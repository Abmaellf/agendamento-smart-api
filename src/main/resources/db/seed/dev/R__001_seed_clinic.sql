-- Seed exclusivo para desenvolvimento e demonstração.
INSERT INTO CLINIC (
    id,
    code,
    name,
    created_at
)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000'::uuid,
    1000001,
    'Clínica Central',
    CURRENT_TIMESTAMP(6)
)
ON CONFLICT DO NOTHING;
