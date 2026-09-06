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
        '20000000-0000-4000-8000-000000000001'::uuid,
        2000001,
        'Ana Oliveira',
        CURRENT_TIMESTAMP(6),
        '550e8400-e29b-41d4-a716-446655440000'::uuid
    ),
    (
        '20000000-0000-4000-8000-000000000002'::uuid,
        2000002,
        'Bruno Santos',
        CURRENT_TIMESTAMP(6),
        '550e8400-e29b-41d4-a716-446655440000'::uuid
    ),
    (
        '20000000-0000-4000-8000-000000000003'::uuid,
        2000003,
        'Carla Mendes',
        CURRENT_TIMESTAMP(6),
        '550e8400-e29b-41d4-a716-446655440000'::uuid
    ),
    (
        '20000000-0000-4000-8000-000000000004'::uuid,
        2000004,
        'Diego Lima',
        CURRENT_TIMESTAMP(6),
        '550e8400-e29b-41d4-a716-446655440000'::uuid
    ),
    (
        '20000000-0000-4000-8000-000000000005'::uuid,
        2000005,
        'Elisa Ferreira',
        CURRENT_TIMESTAMP(6),
        '550e8400-e29b-41d4-a716-446655440000'::uuid
    )
ON CONFLICT DO NOTHING;
