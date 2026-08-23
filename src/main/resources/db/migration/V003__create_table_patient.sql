CREATE TABLE PATIENT (
    id BINARY(16) NOT NULL,
    code BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    clinic_id BINARY(16) NOT NULL,

    CONSTRAINT pk_patient PRIMARY KEY (id),
    CONSTRAINT uk_patient_code UNIQUE (code),
    CONSTRAINT uk_patient_clinic_id UNIQUE (clinic_id, id),
    CONSTRAINT fk_patient_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    INDEX idx_patient_clinic_name (clinic_id, name)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

