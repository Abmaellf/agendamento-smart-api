-- Seed exclusivo para desenvolvimento e demonstração.
INSERT INTO SERVICE_OFFERING (
    id,
    clinic_id,
    name,
    duration_minutes,
    price,
    capacity
)
VALUES
    (
        UNHEX(REPLACE('40000000-0000-4000-8000-000000000001', '-', '')),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')),
        'Pilates',
        60,
        150.00,
        10
    ),
    (
        UNHEX(REPLACE('40000000-0000-4000-8000-000000000002', '-', '')),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')),
        'Atendimento individual',
        60,
        150.00,
        1
    ),
    (
        UNHEX(REPLACE('40000000-0000-4000-8000-000000000003', '-', '')),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')),
        'Turma de reabilitação',
        60,
        150.00,
        3
    ),
    (
        UNHEX(REPLACE('40000000-0000-4000-8000-000000000004', '-', '')),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')),
        'Avaliação fisioterapêutica',
        60,
        100.00,
        1
    )
ON DUPLICATE KEY UPDATE id = id;
