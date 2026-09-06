ALTER TABLE SCHEDULING RENAME TO APPOINTMENT;

ALTER TABLE APPOINTMENT
    RENAME COLUMN date_scheduling TO appointment_date;

ALTER TABLE APPOINTMENT RENAME CONSTRAINT pk_scheduling
    TO pk_appointment;
ALTER TABLE APPOINTMENT RENAME CONSTRAINT uk_scheduling_clinic_idempotency
    TO uk_appointment_clinic_idempotency;
ALTER TABLE APPOINTMENT RENAME CONSTRAINT fk_scheduling_clinic
    TO fk_appointment_clinic;
ALTER TABLE APPOINTMENT RENAME CONSTRAINT fk_scheduling_patient_tenant
    TO fk_appointment_patient_tenant;
ALTER TABLE APPOINTMENT RENAME CONSTRAINT fk_scheduling_unit_tenant
    TO fk_appointment_unit_tenant;
ALTER TABLE APPOINTMENT RENAME CONSTRAINT fk_scheduling_service_tenant
    TO fk_appointment_service_tenant;
ALTER TABLE APPOINTMENT RENAME CONSTRAINT fk_scheduling_professional_tenant
    TO fk_appointment_professional_tenant;
ALTER TABLE APPOINTMENT RENAME CONSTRAINT fk_scheduling_created_by_tenant
    TO fk_appointment_created_by_tenant;
ALTER TABLE APPOINTMENT RENAME CONSTRAINT chk_scheduling_duration
    TO chk_appointment_duration;
ALTER TABLE APPOINTMENT RENAME CONSTRAINT chk_scheduling_price
    TO chk_appointment_price;
ALTER TABLE APPOINTMENT RENAME CONSTRAINT chk_scheduling_status
    TO chk_appointment_status;

ALTER INDEX idx_scheduling_clinic_start RENAME TO idx_appointment_clinic_start;
ALTER INDEX idx_scheduling_unit_start RENAME TO idx_appointment_unit_start;
ALTER INDEX idx_scheduling_professional_start RENAME TO idx_appointment_professional_start;
ALTER INDEX idx_scheduling_patient_start RENAME TO idx_appointment_patient_start;
ALTER INDEX idx_scheduling_service RENAME TO idx_appointment_service;
ALTER INDEX idx_scheduling_created_by RENAME TO idx_appointment_created_by;

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
