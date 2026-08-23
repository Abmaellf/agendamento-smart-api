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
        UNHEX(REPLACE('60000000-0000-4000-8000-000000000001', '-', '')),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')),
        UNHEX(REPLACE('70000000-0000-4000-8000-000000000001', '-', '')),
        'Unidade Centro',
        'America/Cuiaba'
    ),
    (
        UNHEX(REPLACE('60000000-0000-4000-8000-000000000002', '-', '')),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')),
        UNHEX(REPLACE('70000000-0000-4000-8000-000000000001', '-', '')),
        'Unidade Parque do Lago',
        'America/Cuiaba'
    )
ON DUPLICATE KEY UPDATE id = id;
