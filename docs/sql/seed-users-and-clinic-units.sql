\set ON_ERROR_STOP on
\pset pager off

-- Seed manual da clínica de demonstração.
-- Requer as migrations V001--V011 aplicadas.
BEGIN;

INSERT INTO clinic_unit (
    id,
    clinic_id,
    tenant_id,
    name,
    time_zone,
    status,
    type
)
VALUES
    (
        '60000000-0000-4000-8000-000000000001'::uuid,
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        '70000000-0000-4000-8000-000000000001'::uuid,
        'Unidade Centro',
        'America/Cuiaba',
        'ATIVO',
        'MATRIZ'
    ),
    (
        '60000000-0000-4000-8000-000000000002'::uuid,
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        '70000000-0000-4000-8000-000000000001'::uuid,
        'Unidade Parque do Lago',
        'America/Cuiaba',
        'ATIVO',
        'FILIAL'
    )
ON CONFLICT DO NOTHING;

INSERT INTO users (
    id,
    login,
    password,
    role,
    clinic_id,
    clinic_unit_id
)
VALUES
    (
        '10000000-0000-4000-8000-000000000001'::uuid,
        'admin@agendasmart.com',
        '$2a$10$Vn1SASq9MECw4bxqc9vEsuORGKRBM09APd08g5vtGnofYgkk3JJSa',
        'ADMIN',
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        '60000000-0000-4000-8000-000000000001'::uuid
    ),
    (
        '10000000-0000-4000-8000-000000000003'::uuid,
        'atendente@agendasmart.com',
        '$2a$10$qOhK9v/zKMxv5hKH0nK.Cec/FEhehFMl8ElhZAy3gA0Tsb1xPwS/y',
        'USER',
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        '60000000-0000-4000-8000-000000000001'::uuid
    ),
    (
        '10000000-0000-4000-8000-000000000002'::uuid,
        'recepcao@agendasmart.com',
        '$2a$10$qOhK9v/zKMxv5hKH0nK.Cec/FEhehFMl8ElhZAy3gA0Tsb1xPwS/y',
        'USER',
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        '60000000-0000-4000-8000-000000000002'::uuid
    )
ON CONFLICT DO NOTHING;

COMMIT;
