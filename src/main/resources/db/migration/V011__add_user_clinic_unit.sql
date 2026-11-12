ALTER TABLE USERS ADD COLUMN clinic_unit_id UUID;

UPDATE USERS account
SET clinic_unit_id = matrix.id
FROM CLINIC_UNIT matrix
WHERE matrix.clinic_id = account.clinic_id
  AND matrix.type = 'MATRIZ'
  AND account.clinic_unit_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM USERS WHERE clinic_unit_id IS NULL) THEN
        RAISE EXCEPTION 'V011 requer uma MATRIZ para cada clínica com usuários';
    END IF;
END
$$;

ALTER TABLE USERS
    ALTER COLUMN clinic_unit_id SET NOT NULL,
    ADD CONSTRAINT fk_users_clinic_unit_tenant
        FOREIGN KEY (clinic_id, clinic_unit_id)
        REFERENCES CLINIC_UNIT (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT;

CREATE INDEX idx_users_clinic_unit ON USERS (clinic_id, clinic_unit_id);
