\set ON_ERROR_STOP on
\pset pager off

-- Verificação destrutiva somente dentro da transação, finalizada com ROLLBACK.
-- Requer V010 aplicada e a seed de desenvolvimento carregada.
BEGIN;

SELECT clinic_id, id AS unit_id, status, type
FROM clinic_unit
WHERE clinic_id = '550e8400-e29b-41d4-a716-446655440000'::uuid
ORDER BY id;

SELECT clinic_id, id AS professional_id, unit_id
FROM professional
WHERE clinic_id = '550e8400-e29b-41d4-a716-446655440000'::uuid
ORDER BY id;

SELECT COUNT(*) AS patient_unit_count
FROM patient_unit;

INSERT INTO clinic (id, code, name, status)
VALUES (
    '550e8400-e29b-41d4-a716-446655440099'::uuid,
    9999999,
    'CLINICA TEMPORARIA DE CONSTRAINT',
    'ATIVO'
);

INSERT INTO clinic_unit (id, clinic_id, tenant_id, name, time_zone, status, type)
VALUES (
    '60000000-0000-4000-8000-000000000099'::uuid,
    '550e8400-e29b-41d4-a716-446655440099'::uuid,
    '550e8400-e29b-41d4-a716-446655440099'::uuid,
    'UNIDADE TEMPORARIA DE CONSTRAINT',
    'America/Cuiaba',
    'ATIVO',
    'MATRIZ'
);

DO $$
BEGIN
    INSERT INTO clinic_unit (id, clinic_id, tenant_id, name, time_zone, status, type)
    VALUES (
        '60000000-0000-4000-8000-000000000098'::uuid,
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        '70000000-0000-4000-8000-000000000001'::uuid,
        'SEGUNDA MATRIZ TEMPORARIA',
        'America/Cuiaba',
        'ATIVO',
        'MATRIZ'
    );
    RAISE EXCEPTION 'A segunda MATRIZ deveria ter sido rejeitada';
EXCEPTION
    WHEN unique_violation THEN
        RAISE NOTICE 'OK: segunda MATRIZ rejeitada';
END
$$;

DO $$
BEGIN
    INSERT INTO users (id, login, password, role, clinic_id)
    VALUES (
        '10000000-0000-4000-8000-000000000099'::uuid,
        'admin-temporario-constraint@example.invalid',
        'hash-temporario',
        'ADMIN',
        '550e8400-e29b-41d4-a716-446655440000'::uuid
    );
    RAISE EXCEPTION 'O segundo ADMIN deveria ter sido rejeitado';
EXCEPTION
    WHEN unique_violation THEN
        RAISE NOTICE 'OK: segundo ADMIN rejeitado';
END
$$;

DO $$
BEGIN
    INSERT INTO professional (
        id, clinic_id, name, available_week_days, unit_id, user_id, type
    )
    VALUES (
        '30000000-0000-4000-8000-000000000099'::uuid,
        '550e8400-e29b-41d4-a716-446655440000'::uuid,
        'PROFISSIONAL TEMPORARIO DE CONSTRAINT',
        '',
        '60000000-0000-4000-8000-000000000099'::uuid,
        NULL,
        NULL
    );
    RAISE EXCEPTION 'O vínculo profissional-unidade entre clínicas deveria ter sido rejeitado';
EXCEPTION
    WHEN foreign_key_violation THEN
        RAISE NOTICE 'OK: vínculo profissional-unidade entre clínicas rejeitado';
END
$$;

DO $$
BEGIN
    INSERT INTO patient_unit (patient_id, unit_id)
    VALUES (
        '20000000-0000-4000-8000-000000000001'::uuid,
        '60000000-0000-4000-8000-000000000099'::uuid
    );
    RAISE EXCEPTION 'O vínculo paciente-unidade entre clínicas deveria ter sido rejeitado';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: vínculo paciente-unidade entre clínicas rejeitado';
END
$$;

ROLLBACK;
