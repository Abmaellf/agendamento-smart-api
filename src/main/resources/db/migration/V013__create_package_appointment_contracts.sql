CREATE TABLE PACKAGE_CONTRACT (
    id UUID NOT NULL,
    clinic_id UUID NOT NULL,
    package_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    unit_id UUID NOT NULL,
    service_id UUID NOT NULL,
    total_sessions INT NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    requested_start_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    effective_first_date DATE NOT NULL,
    effective_last_date DATE NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_package_contract PRIMARY KEY (id),
    CONSTRAINT uk_package_contract_idempotency UNIQUE (clinic_id, idempotency_key),
    CONSTRAINT fk_package_contract_clinic FOREIGN KEY (clinic_id) REFERENCES CLINIC (id),
    CONSTRAINT fk_package_contract_package FOREIGN KEY (package_id) REFERENCES SERVICE_PACKAGE (id),
    CONSTRAINT fk_package_contract_patient FOREIGN KEY (patient_id) REFERENCES PATIENT (id),
    CONSTRAINT fk_package_contract_unit FOREIGN KEY (unit_id) REFERENCES CLINIC_UNIT (id),
    CONSTRAINT fk_package_contract_service FOREIGN KEY (service_id) REFERENCES SERVICE_OFFERING (id),
    CONSTRAINT fk_package_contract_created_by FOREIGN KEY (created_by) REFERENCES USERS (id)
);

CREATE TABLE PACKAGE_CONTRACT_WEEK_DAY (
    contract_id UUID NOT NULL,
    week_day VARCHAR(16) NOT NULL,
    CONSTRAINT pk_package_contract_week_day PRIMARY KEY (contract_id, week_day),
    CONSTRAINT fk_package_contract_week_day FOREIGN KEY (contract_id) REFERENCES PACKAGE_CONTRACT (id) ON DELETE CASCADE
);

ALTER TABLE APPOINTMENT ADD COLUMN package_contract_id UUID NULL;
ALTER TABLE APPOINTMENT ADD CONSTRAINT fk_appointment_package_contract FOREIGN KEY (package_contract_id) REFERENCES PACKAGE_CONTRACT (id);
CREATE INDEX idx_appointment_package_contract ON APPOINTMENT (clinic_id, package_contract_id);
