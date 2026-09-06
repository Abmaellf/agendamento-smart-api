-- Seed exclusivo para desenvolvimento e demonstração.
-- Credenciais locais:
-- admin@agendasmart.com / Admin@123
-- recepcao@agendasmart.com / User@123
-- atendente@agendasmart.com / User@123
INSERT INTO USERS (
    id,
    login,
    password,
    role,
    clinic_id
)
VALUES
    (
        '10000000-0000-4000-8000-000000000001'::uuid,
        'admin@agendasmart.com',
        '$2a$10$Vn1SASq9MECw4bxqc9vEsuORGKRBM09APd08g5vtGnofYgkk3JJSa',
        'ADMIN',
        '550e8400-e29b-41d4-a716-446655440000'::uuid
    ),
    (
        '10000000-0000-4000-8000-000000000002'::uuid,
        'recepcao@agendasmart.com',
        '$2a$10$qOhK9v/zKMxv5hKH0nK.Cec/FEhehFMl8ElhZAy3gA0Tsb1xPwS/y',
        'USER',
        '550e8400-e29b-41d4-a716-446655440000'::uuid
    ),
    (
        '10000000-0000-4000-8000-000000000003'::uuid,
        'atendente@agendasmart.com',
        '$2a$10$qOhK9v/zKMxv5hKH0nK.Cec/FEhehFMl8ElhZAy3gA0Tsb1xPwS/y',
        'USER',
        '550e8400-e29b-41d4-a716-446655440000'::uuid
    )
ON CONFLICT DO NOTHING;
