CREATE TABLE CLINIC_UNIT (
    id BINARY(16) NOT NULL,
    clinic_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    time_zone VARCHAR(80) NOT NULL,

    CONSTRAINT pk_clinic_unit PRIMARY KEY (id),
    CONSTRAINT uk_clinic_unit_clinic_id UNIQUE (clinic_id, id),
    CONSTRAINT uk_clinic_unit_clinic_name UNIQUE (clinic_id, name),
    CONSTRAINT fk_clinic_unit_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    INDEX idx_clinic_unit_tenant (tenant_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

