CREATE TABLE SCHEDULING (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    clinic_id BINARY(16) NOT NULL,
    unit_id BINARY(16) NOT NULL,
    service_id BINARY(16) NOT NULL,
    professional_id BINARY(16) NULL,
    created_by BINARY(16) NOT NULL,

    starts_at TIMESTAMP(6) NOT NULL,
    time_zone VARCHAR(80) NOT NULL,
    duration_minutes INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    idempotency_key VARCHAR(120) NULL,

    pathology JSON NOT NULL,
    date_scheduling DATETIME(6) NOT NULL,
    hours TIME(6) NOT NULL,
    status ENUM(
        'AGENDADO',
        'ATENDENDO',
        'CANCELADO',
        'FINALIZADO'
    ) NOT NULL DEFAULT 'AGENDADO',
    variant VARCHAR(50) NULL,

    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_scheduling PRIMARY KEY (id),
    CONSTRAINT uk_scheduling_clinic_idempotency
        UNIQUE (clinic_id, idempotency_key),

    CONSTRAINT fk_scheduling_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_scheduling_patient_tenant
        FOREIGN KEY (clinic_id, patient_id)
        REFERENCES PATIENT (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_scheduling_unit_tenant
        FOREIGN KEY (clinic_id, unit_id)
        REFERENCES CLINIC_UNIT (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_scheduling_service_tenant
        FOREIGN KEY (clinic_id, service_id)
        REFERENCES SERVICE_OFFERING (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_scheduling_professional_tenant
        FOREIGN KEY (clinic_id, professional_id)
        REFERENCES PROFESSIONAL (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_scheduling_created_by_tenant
        FOREIGN KEY (clinic_id, created_by)
        REFERENCES USERS (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT chk_scheduling_duration
        CHECK (duration_minutes BETWEEN 1 AND 480),
    CONSTRAINT chk_scheduling_price
        CHECK (price >= 0),

    INDEX idx_scheduling_clinic_start
        (clinic_id, starts_at),
    INDEX idx_scheduling_unit_start
        (clinic_id, unit_id, starts_at),
    INDEX idx_scheduling_professional_start
        (clinic_id, professional_id, starts_at),
    INDEX idx_scheduling_patient_start
        (clinic_id, patient_id, starts_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

