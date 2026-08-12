CREATE TABLE IF NOT EXISTS PATIENT (
    id BINARY(16) NOT NULL,
    code BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    clinic_id BINARY(16) NOT NULL,

    CONSTRAINT pk_patient PRIMARY KEY (id),
    CONSTRAINT uk_patient_code UNIQUE (code),
    CONSTRAINT fk_patient_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

-- Pacientes iniciais vinculados à Clínica Central criada na V001.
INSERT INTO PATIENT (id, code, name, created_at, clinic_id)
VALUES
    (
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000001', '-', '')),
        2000001,
        'Ana Oliveira',
        CURRENT_TIMESTAMP(6),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    ),
    (
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000002', '-', '')),
        2000002,
        'Bruno Santos',
        CURRENT_TIMESTAMP(6),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    ),
    (
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000003', '-', '')),
        2000003,
        'Carla Mendes',
        CURRENT_TIMESTAMP(6),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    ),
    (
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000004', '-', '')),
        2000004,
        'Diego Lima',
        CURRENT_TIMESTAMP(6),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    ),
    (
        UNHEX(REPLACE('20000000-0000-4000-8000-000000000005', '-', '')),
        2000005,
        'Elisa Ferreira',
        CURRENT_TIMESTAMP(6),
        UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', ''))
    );
