CREATE TABLE SCHEDULING (
    id UUID NOT NULL,
    patient_id UUID NOT NULL,
    clinic_id UUID NOT NULL,
    unit_id UUID NOT NULL,
    service_id UUID NOT NULL,
    professional_id UUID NULL,
    created_by UUID NOT NULL,

    starts_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    time_zone VARCHAR(80) NOT NULL,
    duration_minutes INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    idempotency_key VARCHAR(120) NULL,

    pathology JSONB NOT NULL,
    date_scheduling TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    hours TIME(6) WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AGENDADO',
    variant VARCHAR(50) NULL,

    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    CONSTRAINT chk_scheduling_status
        CHECK (status IN ('AGENDADO', 'ATENDENDO', 'CANCELADO', 'FINALIZADO'))
);

CREATE INDEX idx_scheduling_clinic_start
    ON SCHEDULING (clinic_id, starts_at);
CREATE INDEX idx_scheduling_unit_start
    ON SCHEDULING (clinic_id, unit_id, starts_at);
CREATE INDEX idx_scheduling_professional_start
    ON SCHEDULING (clinic_id, professional_id, starts_at);
CREATE INDEX idx_scheduling_patient_start
    ON SCHEDULING (clinic_id, patient_id, starts_at);
CREATE INDEX idx_scheduling_service
    ON SCHEDULING (service_id);
CREATE INDEX idx_scheduling_created_by
    ON SCHEDULING (created_by);
