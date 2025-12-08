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


