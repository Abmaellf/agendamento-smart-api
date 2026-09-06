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
        '40000000-0000-4000-8000-000000000001'::uuid,
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        'Pilates',
        60,
        150.00,
        10
    ),
    (
        '40000000-0000-4000-8000-000000000002'::uuid,
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        'Atendimento individual',
        60,
        150.00,
        1
    ),
    (
        '40000000-0000-4000-8000-000000000003'::uuid,
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        'Turma de reabilitação',
        60,
        150.00,
        3
    ),
    (
        '40000000-0000-4000-8000-000000000004'::uuid,
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        'Avaliação fisioterapêutica',
        60,
        100.00,
        1
    )
ON CONFLICT DO NOTHING;
