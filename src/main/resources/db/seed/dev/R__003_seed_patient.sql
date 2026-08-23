-- Seed exclusivo para desenvolvimento e demonstração.
INSERT INTO PATIENT (
    id,
    code,
    name,
    created_at,
    clinic_id
)
VALUES
    (
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000001', '-', '')),
        2000001,
        'Ana Oliveira',
        CURRENT_TIMESTAMP(6),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    ),
    (
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000002', '-', '')),
        2000002,
        'Bruno Santos',
        CURRENT_TIMESTAMP(6),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    ),
    (
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000003', '-', '')),
        2000003,
        'Carla Mendes',
        CURRENT_TIMESTAMP(6),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    ),
    (
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000004', '-', '')),
        2000004,
        'Diego Lima',
        CURRENT_TIMESTAMP(6),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    ),
    (
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000005', '-', '')),
        2000005,
        'Elisa Ferreira',
        CURRENT_TIMESTAMP(6),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    )
ON DUPLICATE KEY UPDATE id = id;
