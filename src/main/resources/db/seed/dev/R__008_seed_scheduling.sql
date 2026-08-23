-- Seed exclusivo para desenvolvimento e demonstração.
-- O Instant é persistido em UTC; appointment_date e hours representam o
-- horário local da unidade America/Cuiaba (-04:00).

SET @previous_session_time_zone = @@SESSION.time_zone;
SET SESSION time_zone = '+00:00';

SET @clinic_id = UNHEX(REPLACE(
    '550e8400-e29b-41d4-a716-446655440000', '-', ''
));
SET @unit_centro_id = UNHEX(REPLACE(
    '60000000-0000-4000-8000-000000000001', '-', ''
));
SET @unit_parque_id = UNHEX(REPLACE(
    '60000000-0000-4000-8000-000000000002', '-', ''
));
SET @admin_user_id = UNHEX(REPLACE(
    '10000000-0000-4000-8000-000000000001', '-', ''
));
SET @service_pilates_id = UNHEX(REPLACE(
    '40000000-0000-4000-8000-000000000001', '-', ''
));
SET @service_individual_id = UNHEX(REPLACE(
    '40000000-0000-4000-8000-000000000002', '-', ''
));
SET @service_group_id = UNHEX(REPLACE(
    '40000000-0000-4000-8000-000000000003', '-', ''
));
SET @professional_carla_id = UNHEX(REPLACE(
    '30000000-0000-4000-8000-000000000001', '-', ''
));
SET @professional_diego_id = UNHEX(REPLACE(
    '30000000-0000-4000-8000-000000000002', '-', ''
));

SET @clinic_current_date = DATE(
    CONVERT_TZ(UTC_TIMESTAMP(6), '+00:00', '-04:00')
);

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
VALUES
    (
        UNHEX(REPLACE('50000000-0000-4000-8000-000000000001', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000001', '-', '')),
        @clinic_id,
        @unit_centro_id,
        @service_group_id,
        NULL,
        @admin_user_id,
        CONVERT_TZ(
            TIMESTAMP(DATE_ADD(@clinic_current_date, INTERVAL 1 DAY), '08:30:00'),
            '-04:00',
            '+00:00'
        ),
        'America/Cuiaba',
        60,
        150.00,
        'dev-seed-appointment-0001',
        JSON_ARRAY(),
        TIMESTAMP(DATE_ADD(@clinic_current_date, INTERVAL 1 DAY), '08:30:00'),
        '08:30:00',
        'AGENDADO',
        'primary',
        UTC_TIMESTAMP(6),
        UTC_TIMESTAMP(6),
        0
    ),
    (
        UNHEX(REPLACE('50000000-0000-4000-8000-000000000002', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000005', '-', '')),
        @clinic_id,
        @unit_parque_id,
        @service_pilates_id,
        @professional_carla_id,
        @admin_user_id,
        CONVERT_TZ(
            TIMESTAMP(DATE_ADD(@clinic_current_date, INTERVAL 2 DAY), '09:30:00'),
            '-04:00',
            '+00:00'
        ),
        'America/Cuiaba',
        60,
        150.00,
        'dev-seed-appointment-0002',
        JSON_ARRAY(),
        TIMESTAMP(DATE_ADD(@clinic_current_date, INTERVAL 2 DAY), '09:30:00'),
        '09:30:00',
        'AGENDADO',
        'primary',
        UTC_TIMESTAMP(6),
        UTC_TIMESTAMP(6),
        0
    ),
    (
        UNHEX(REPLACE('50000000-0000-4000-8000-000000000003', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000003', '-', '')),
        @clinic_id,
        @unit_centro_id,
        @service_pilates_id,
        @professional_diego_id,
        @admin_user_id,
        CONVERT_TZ(
            TIMESTAMP(DATE_ADD(@clinic_current_date, INTERVAL 3 DAY), '10:00:00'),
            '-04:00',
            '+00:00'
        ),
        'America/Cuiaba',
        60,
        150.00,
        'dev-seed-appointment-0003',
        JSON_ARRAY(),
        TIMESTAMP(DATE_ADD(@clinic_current_date, INTERVAL 3 DAY), '10:00:00'),
        '10:00:00',
        'AGENDADO',
        'primary',
        UTC_TIMESTAMP(6),
        UTC_TIMESTAMP(6),
        0
    ),
    (
        UNHEX(REPLACE('50000000-0000-4000-8000-000000000004', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000004', '-', '')),
        @clinic_id,
        @unit_parque_id,
        @service_individual_id,
        @professional_carla_id,
        @admin_user_id,
        CONVERT_TZ(
            TIMESTAMP(DATE_ADD(@clinic_current_date, INTERVAL 4 DAY), '14:00:00'),
            '-04:00',
            '+00:00'
        ),
        'America/Cuiaba',
        60,
        150.00,
        'dev-seed-appointment-0004',
        JSON_ARRAY(),
        TIMESTAMP(DATE_ADD(@clinic_current_date, INTERVAL 4 DAY), '14:00:00'),
        '14:00:00',
        'AGENDADO',
        'primary',
        UTC_TIMESTAMP(6),
        UTC_TIMESTAMP(6),
        0
    ),
    (
        UNHEX(REPLACE('50000000-0000-4000-8000-000000000005', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000001', '-', '')),
        @clinic_id,
        @unit_centro_id,
        @service_group_id,
        NULL,
        @admin_user_id,
        CONVERT_TZ(
            TIMESTAMP(DATE_ADD(@clinic_current_date, INTERVAL 5 DAY), '16:00:00'),
            '-04:00',
            '+00:00'
        ),
        'America/Cuiaba',
        60,
        150.00,
        'dev-seed-appointment-0005',
        JSON_ARRAY(),
        TIMESTAMP(DATE_ADD(@clinic_current_date, INTERVAL 5 DAY), '16:00:00'),
        '16:00:00',
        'AGENDADO',
        'primary',
        UTC_TIMESTAMP(6),
        UTC_TIMESTAMP(6),
        0
    ),
    (
        UNHEX(REPLACE('50000000-0000-4000-8000-000000000006', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000002', '-', '')),
        @clinic_id,
        @unit_centro_id,
        @service_pilates_id,
        @professional_carla_id,
        @admin_user_id,
        CONVERT_TZ(
            TIMESTAMP(DATE_SUB(@clinic_current_date, INTERVAL 1 DAY), '08:30:00'),
            '-04:00',
            '+00:00'
        ),
        'America/Cuiaba',
        60,
        150.00,
        'dev-seed-appointment-0006',
        JSON_ARRAY(),
        TIMESTAMP(DATE_SUB(@clinic_current_date, INTERVAL 1 DAY), '08:30:00'),
        '08:30:00',
        'FINALIZADO',
        'success',
        DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY),
        DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY),
        0
    )
ON DUPLICATE KEY UPDATE id = id;

SET SESSION time_zone = @previous_session_time_zone;
