ALTER TABLE APPOINTMENT
    DROP CONSTRAINT chk_appointment_status;

ALTER TABLE APPOINTMENT
    ADD CONSTRAINT chk_appointment_status
        CHECK (status IN ('AGENDADO', 'ATENDENDO', 'CANCELADO', 'FALTA', 'FINALIZADO'));

CREATE INDEX idx_appointment_status_start
    ON APPOINTMENT (status, starts_at);
