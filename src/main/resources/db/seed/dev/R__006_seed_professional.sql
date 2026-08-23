-- Seed exclusivo para desenvolvimento e demonstração.
INSERT INTO PROFESSIONAL (
    id,
    clinic_id,
    name,
    available_week_days
)
VALUES
    (
        UNHEX(REPLACE('30000000-0000-4000-8000-000000000001', '-', '')),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')),
        'Carla Profissional',
        '1,2,3,4,5,6,7'
    ),
    (
        UNHEX(REPLACE('30000000-0000-4000-8000-000000000002', '-', '')),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')),
        'Diego Profissional',
        '1,2,3,4,5'
    )
ON DUPLICATE KEY UPDATE id = id;
