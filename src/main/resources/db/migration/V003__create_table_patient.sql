CREATE TABLE PATIENT (
    id UUID NOT NULL,
    code BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    clinic_id UUID NOT NULL,

    CONSTRAINT pk_patient PRIMARY KEY (id),
    CONSTRAINT uk_patient_code UNIQUE (code),
    CONSTRAINT uk_patient_clinic_id UNIQUE (clinic_id, id),
    CONSTRAINT fk_patient_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

CREATE INDEX idx_patient_clinic_name ON PATIENT (clinic_id, name);
