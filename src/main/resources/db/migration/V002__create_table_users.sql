CREATE TABLE IF NOT EXISTS USERS (
    id BINARY(16) NOT NULL,
    login VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    clinic_id BINARY(16) NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_login UNIQUE (login),
    CONSTRAINT fk_users_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

-- Usuários iniciais para desenvolvimento e demonstração.
-- Credenciais: admin@agendasmart.com / Admin@123
--              recepcao@agendasmart.com / User@123
--              atendente@agendasmart.com / User@123
INSERT INTO USERS (id, login, password, role, clinic_id)
VALUES
    (
        UNHEX(REPLACE('10000000-0000-4000-8000-000000000001', '-', '')),
        'admin@agendasmart.com',
        '$2a$10$Vn1SASq9MECw4bxqc9vEsuORGKRBM09APd08g5vtGnofYgkk3JJSa',
        'ADMIN',
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    ),
    (
        UNHEX(REPLACE('10000000-0000-4000-8000-000000000002', '-', '')),
        'recepcao@agendasmart.com',
        '$2a$10$qOhK9v/zKMxv5hKH0nK.Cec/FEhehFMl8ElhZAy3gA0Tsb1xPwS/y',
        'USER',
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    ),
    (
        UNHEX(REPLACE('10000000-0000-4000-8000-000000000003', '-', '')),
        'atendente@agendasmart.com',
        '$2a$10$qOhK9v/zKMxv5hKH0nK.Cec/FEhehFMl8ElhZAy3gA0Tsb1xPwS/y',
        'USER',
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    );
