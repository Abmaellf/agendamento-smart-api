-- Seed exclusivo para desenvolvimento e demonstração.
INSERT INTO CLINIC_UNIT (
    id,
    clinic_id,
    tenant_id,
    name,
    time_zone
)
VALUES
    (
        '60000000-0000-4000-8000-000000000001'::uuid,
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        '70000000-0000-4000-8000-000000000001'::uuid,
        'Unidade Centro',
        'America/Cuiaba'
    ),
    (
        '60000000-0000-4000-8000-000000000002'::uuid,
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        '70000000-0000-4000-8000-000000000001'::uuid,
        'Unidade Parque do Lago',
        'America/Cuiaba'
    )
ON CONFLICT DO NOTHING;
