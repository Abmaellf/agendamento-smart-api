-- Seed exclusivo para desenvolvimento e demonstração.
-- starts_at é um instante absoluto; appointment_date e hours preservam o
-- horário local da unidade America/Cuiaba.

WITH seed_context AS (
    SELECT
        '550e8400-e29b-41d4-a716-446655440000'::uuid AS clinic_id,
        '10000000-0000-4000-8000-000000000001'::uuid AS admin_user_id,
        (CURRENT_TIMESTAMP AT TIME ZONE 'America/Cuiaba')::date AS clinic_date,
        'America/Cuiaba'::text AS time_zone
),
seed_rows (
    id,
    patient_id,
    unit_id,
    service_id,
    professional_id,
    day_offset,
    start_time,
    idempotency_key,
    status,
    variant,
    created_age,
    updated_age
) AS (
    VALUES
        (
            '50000000-0000-4000-8000-000000000001'::uuid,
            '20000000-0000-4000-8000-000000000001'::uuid,
            '60000000-0000-4000-8000-000000000001'::uuid,
            '40000000-0000-4000-8000-000000000003'::uuid,
            NULL::uuid,
            1,
            TIME '08:30:00',
            'dev-seed-appointment-0001',
            'AGENDADO',
            'primary',
            INTERVAL '0 days',
            INTERVAL '0 days'
        ),
        (
            '50000000-0000-4000-8000-000000000002'::uuid,
            '20000000-0000-4000-8000-000000000005'::uuid,
            '60000000-0000-4000-8000-000000000002'::uuid,
            '40000000-0000-4000-8000-000000000001'::uuid,
            '30000000-0000-4000-8000-000000000001'::uuid,
            2,
            TIME '09:30:00',
            'dev-seed-appointment-0002',
            'AGENDADO',
            'primary',
            INTERVAL '0 days',
            INTERVAL '0 days'
        ),
        (
            '50000000-0000-4000-8000-000000000003'::uuid,
            '20000000-0000-4000-8000-000000000003'::uuid,
            '60000000-0000-4000-8000-000000000001'::uuid,
            '40000000-0000-4000-8000-000000000001'::uuid,
            '30000000-0000-4000-8000-000000000002'::uuid,
            3,
            TIME '10:00:00',
            'dev-seed-appointment-0003',
            'AGENDADO',
            'primary',
            INTERVAL '0 days',
            INTERVAL '0 days'
        ),
        (
            '50000000-0000-4000-8000-000000000004'::uuid,
            '20000000-0000-4000-8000-000000000004'::uuid,
            '60000000-0000-4000-8000-000000000002'::uuid,
            '40000000-0000-4000-8000-000000000002'::uuid,
            '30000000-0000-4000-8000-000000000001'::uuid,
            4,
            TIME '14:00:00',
            'dev-seed-appointment-0004',
            'AGENDADO',
            'primary',
            INTERVAL '0 days',
            INTERVAL '0 days'
        ),
        (
            '50000000-0000-4000-8000-000000000005'::uuid,
            '20000000-0000-4000-8000-000000000001'::uuid,
            '60000000-0000-4000-8000-000000000001'::uuid,
            '40000000-0000-4000-8000-000000000003'::uuid,
            NULL::uuid,
            5,
            TIME '16:00:00',
            'dev-seed-appointment-0005',
            'AGENDADO',
            'primary',
            INTERVAL '0 days',
            INTERVAL '0 days'
        ),
        (
            '50000000-0000-4000-8000-000000000006'::uuid,
            '20000000-0000-4000-8000-000000000002'::uuid,
            '60000000-0000-4000-8000-000000000001'::uuid,
            '40000000-0000-4000-8000-000000000001'::uuid,
            '30000000-0000-4000-8000-000000000001'::uuid,
            -1,
            TIME '08:30:00',
            'dev-seed-appointment-0006',
            'FINALIZADO',
            'success',
            INTERVAL '2 days',
            INTERVAL '1 day'
        )
)
INSERT INTO APPOINTMENT (
    id,
    patient_id,
    clinic_id,
    unit_id,
    service_id,
    professional_id,
    created_by,
    starts_at,
    time_zone,
    duration_minutes,
    price,
    idempotency_key,
    pathology,
    appointment_date,
    hours,
    status,
    variant,
    created_at,
    updated_at,
    version
)
SELECT
    seed_rows.id,
    seed_rows.patient_id,
    seed_context.clinic_id,
    seed_rows.unit_id,
    seed_rows.service_id,
    seed_rows.professional_id,
    seed_context.admin_user_id,
    ((seed_context.clinic_date + seed_rows.day_offset) + seed_rows.start_time)
        AT TIME ZONE seed_context.time_zone,
    seed_context.time_zone,
    60,
    150.00,
    seed_rows.idempotency_key,
    '[]'::jsonb,
    (seed_context.clinic_date + seed_rows.day_offset) + seed_rows.start_time,
    seed_rows.start_time,
    seed_rows.status,
    seed_rows.variant,
    CURRENT_TIMESTAMP - seed_rows.created_age,
    CURRENT_TIMESTAMP - seed_rows.updated_age,
    0
FROM seed_context
CROSS JOIN seed_rows
ON CONFLICT DO NOTHING;
