ALTER TABLE SCHEDULING
    MODIFY COLUMN id BINARY(16) NOT NULL,
    MODIFY COLUMN status ENUM(
        'AGENDADO',
        'ATENDENDO',
        'CANCELADO',
        'FINALIZADO'
    ) NOT NULL DEFAULT 'AGENDADO';

-- Agendamentos iniciais para os pacientes criados na V003.
-- As datas são relativas à execução da migration para manter a agenda útil
-- em qualquer nova instalação do projeto.
INSERT INTO SCHEDULING (
    id,
    patient_id,
    pathology,
    date_scheduling,
    hours,
    status,
    variant,
    created_at,
    updated_at
)
VALUES
    (
        UNHEX(REPLACE('30000000-0000-4000-8000-000000000001', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000001', '-', '')),
        JSON_ARRAY('Consulta de rotina'),
        TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '08:00:00'),
        '08:00:00',
        'AGENDADO',
        'primary',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        UNHEX(REPLACE('30000000-0000-4000-8000-000000000002', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000002', '-', '')),
        JSON_ARRAY('Avaliação ortopédica', 'Dor lombar'),
        TIMESTAMP(CURRENT_DATE, '09:30:00'),
        '09:30:00',
        'ATENDENDO',
        'warning',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        UNHEX(REPLACE('30000000-0000-4000-8000-000000000003', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000003', '-', '')),
        JSON_ARRAY('Consulta dermatológica'),
        TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '10:00:00'),
        '10:00:00',
        'AGENDADO',
        'primary',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        UNHEX(REPLACE('30000000-0000-4000-8000-000000000004', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000004', '-', '')),
        JSON_ARRAY('Avaliação cardiológica'),
        TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), '14:00:00'),
        '14:00:00',
        'CANCELADO',
        'danger',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        UNHEX(REPLACE('30000000-0000-4000-8000-000000000005', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000005', '-', '')),
        JSON_ARRAY('Retorno clínico'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), '16:00:00'),
        '16:00:00',
        'FINALIZADO',
        'success',
        DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 DAY),
        DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY)
    ),
    (
        UNHEX(REPLACE('30000000-0000-4000-8000-000000000006', '-', '')),
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000001', '-', '')),
        JSON_ARRAY('Exames de acompanhamento'),
        TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '08:30:00'),
        '08:30:00',
        'FINALIZADO',
        'success',
        DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 8 DAY),
        DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY)
    );
