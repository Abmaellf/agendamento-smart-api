RENAME TABLE SCHEDULING TO APPOINTMENT;

ALTER TABLE APPOINTMENT
    RENAME COLUMN date_scheduling TO appointment_date;

ALTER TABLE APPOINTMENT
    DROP FOREIGN KEY fk_scheduling_clinic,
    DROP FOREIGN KEY fk_scheduling_patient_tenant,
    DROP FOREIGN KEY fk_scheduling_unit_tenant,
    DROP FOREIGN KEY fk_scheduling_service_tenant,
    DROP FOREIGN KEY fk_scheduling_professional_tenant,
    DROP FOREIGN KEY fk_scheduling_created_by_tenant,
    DROP CHECK chk_scheduling_duration,
    DROP CHECK chk_scheduling_price;

ALTER TABLE APPOINTMENT
    RENAME INDEX uk_scheduling_clinic_idempotency
        TO uk_appointment_clinic_idempotency,
    RENAME INDEX idx_scheduling_clinic_start
        TO idx_appointment_clinic_start,
    RENAME INDEX idx_scheduling_unit_start
        TO idx_appointment_unit_start,
    RENAME INDEX idx_scheduling_professional_start
        TO idx_appointment_professional_start,
    RENAME INDEX idx_scheduling_patient_start
        TO idx_appointment_patient_start,
    RENAME INDEX fk_scheduling_service_tenant
        TO idx_appointment_service,
    RENAME INDEX fk_scheduling_created_by_tenant
        TO idx_appointment_created_by;

ALTER TABLE APPOINTMENT
    ADD CONSTRAINT fk_appointment_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_appointment_patient_tenant
        FOREIGN KEY (clinic_id, patient_id)
        REFERENCES PATIENT (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_appointment_unit_tenant
        FOREIGN KEY (clinic_id, unit_id)
        REFERENCES CLINIC_UNIT (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_appointment_service_tenant
        FOREIGN KEY (clinic_id, service_id)
        REFERENCES SERVICE_OFFERING (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_appointment_professional_tenant
        FOREIGN KEY (clinic_id, professional_id)
        REFERENCES PROFESSIONAL (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_appointment_created_by_tenant
        FOREIGN KEY (clinic_id, created_by)
        REFERENCES USERS (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    ADD CONSTRAINT chk_appointment_duration
        CHECK (duration_minutes BETWEEN 1 AND 480),
    ADD CONSTRAINT chk_appointment_price
        CHECK (price >= 0);

UPDATE APPOINTMENT
SET idempotency_key = REPLACE(
        idempotency_key,
        'dev-seed-scheduling-',
        'dev-seed-appointment-'
    )
WHERE idempotency_key IN (
    'dev-seed-scheduling-0001',
    'dev-seed-scheduling-0002',
    'dev-seed-scheduling-0003',
    'dev-seed-scheduling-0004',
    'dev-seed-scheduling-0005',
    'dev-seed-scheduling-0006'
);
