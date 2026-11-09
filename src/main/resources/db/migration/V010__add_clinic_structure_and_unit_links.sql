ALTER TABLE CLINIC
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    ADD CONSTRAINT chk_clinic_status
        CHECK (status IN ('ATIVO', 'INATIVO', 'BLOQUEADA'));

ALTER TABLE CLINIC_UNIT
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    ADD COLUMN type VARCHAR(20),
    ADD CONSTRAINT chk_clinic_unit_status
        CHECK (status IN ('ATIVO', 'INATIVO', 'BLOQUEADA')),
    ADD CONSTRAINT chk_clinic_unit_type
        CHECK (type IN ('MATRIZ', 'FILIAL'));

-- Uma clínica já existente com somente uma unidade não é ambígua.
UPDATE CLINIC_UNIT candidate
SET type = 'MATRIZ'
WHERE (
    SELECT COUNT(*)
    FROM CLINIC_UNIT sibling
    WHERE sibling.clinic_id = candidate.clinic_id
) = 1;

-- Mapeamento explícito da seed de desenvolvimento revisado na pré-verificação.
-- A segunda unidade é preservada como FILIAL.
UPDATE CLINIC_UNIT
SET type = CASE id
    WHEN '60000000-0000-4000-8000-000000000001'::uuid THEN 'MATRIZ'
    WHEN '60000000-0000-4000-8000-000000000002'::uuid THEN 'FILIAL'
END
WHERE clinic_id = '550e8400-e29b-41d4-a716-446655440000'::uuid
  AND id IN (
      '60000000-0000-4000-8000-000000000001'::uuid,
      '60000000-0000-4000-8000-000000000002'::uuid
  )
  AND 2 = (
      SELECT COUNT(*)
      FROM CLINIC_UNIT seed_units
      WHERE seed_units.clinic_id = '550e8400-e29b-41d4-a716-446655440000'::uuid
        AND seed_units.id IN (
            '60000000-0000-4000-8000-000000000001'::uuid,
            '60000000-0000-4000-8000-000000000002'::uuid
        )
  );

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM CLINIC_UNIT WHERE type IS NULL) THEN
        RAISE EXCEPTION
            'V010 requer definição explícita da MATRIZ para clínicas com múltiplas unidades; execute a pré-verificação antes da migration';
    END IF;
END
$$;

ALTER TABLE CLINIC_UNIT
    ALTER COLUMN type SET NOT NULL;

CREATE UNIQUE INDEX uk_clinic_unit_single_matriz_per_clinic
    ON CLINIC_UNIT (clinic_id)
    WHERE type = 'MATRIZ';

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM CLINIC clinic
        LEFT JOIN CLINIC_UNIT unit ON unit.clinic_id = clinic.id
        GROUP BY clinic.id
        HAVING COUNT(unit.id) FILTER (WHERE unit.type = 'MATRIZ') <> 1
    ) THEN
        RAISE EXCEPTION
            'V010 requer exatamente uma MATRIZ por clínica; execute a pré-verificação antes da migration';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM CLINIC clinic
        LEFT JOIN USERS account ON account.clinic_id = clinic.id
        GROUP BY clinic.id
        HAVING COUNT(account.id) FILTER (WHERE account.role = 'ADMIN') <> 1
    ) THEN
        RAISE EXCEPTION
            'V010 requer exatamente um ADMIN por clínica; execute a pré-verificação antes da migration';
    END IF;
END
$$;

CREATE UNIQUE INDEX uk_users_single_admin_per_clinic
    ON USERS (clinic_id)
    WHERE role = 'ADMIN';

ALTER TABLE PROFESSIONAL
    ADD COLUMN unit_id UUID,
    ADD COLUMN user_id UUID,
    ADD COLUMN type VARCHAR(30),
    ADD CONSTRAINT chk_professional_type
        CHECK (type IS NULL OR type IN ('ATENDENTE', 'ADMINISTRATIVO'));

-- Profissionais usados em uma única unidade recebem esse vínculo.
WITH single_unit AS (
    SELECT
        professional_id,
        MIN(unit_id::text)::uuid AS unit_id
    FROM APPOINTMENT
    WHERE professional_id IS NOT NULL
    GROUP BY professional_id
    HAVING COUNT(DISTINCT unit_id) = 1
)
UPDATE PROFESSIONAL professional
SET unit_id = single_unit.unit_id
FROM single_unit
WHERE professional.id = single_unit.professional_id;

-- Profissionais sem histórico ficam na MATRIZ de sua clínica.
UPDATE PROFESSIONAL professional
SET unit_id = matrix.id
FROM CLINIC_UNIT matrix
WHERE matrix.clinic_id = professional.clinic_id
  AND matrix.type = 'MATRIZ'
  AND professional.unit_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM APPOINTMENT appointment
      WHERE appointment.professional_id = professional.id
  );

-- Decisão explícita para o único profissional ambíguo da seed atual.
UPDATE PROFESSIONAL
SET unit_id = '60000000-0000-4000-8000-000000000002'::uuid
WHERE id = '30000000-0000-4000-8000-000000000001'::uuid
  AND clinic_id = '550e8400-e29b-41d4-a716-446655440000'::uuid
  AND unit_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM PROFESSIONAL WHERE unit_id IS NULL) THEN
        RAISE EXCEPTION
            'V010 encontrou profissional usado em múltiplas unidades sem decisão de vínculo; execute a pré-verificação antes da migration';
    END IF;
END
$$;

ALTER TABLE PROFESSIONAL
    ALTER COLUMN unit_id SET NOT NULL,
    ADD CONSTRAINT fk_professional_unit_tenant
        FOREIGN KEY (clinic_id, unit_id)
        REFERENCES CLINIC_UNIT (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_professional_user_tenant
        FOREIGN KEY (clinic_id, user_id)
        REFERENCES USERS (clinic_id, id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    ADD CONSTRAINT uk_professional_user UNIQUE (user_id);

CREATE INDEX idx_professional_unit ON PROFESSIONAL (clinic_id, unit_id);

ALTER TABLE PROFESSIONAL
    DROP CONSTRAINT chk_professional_available_week_days,
    ADD CONSTRAINT chk_professional_available_week_days
        CHECK (
            available_week_days = ''
            OR available_week_days ~
                '^[[:space:]]*[1-7]([[:space:]]*,[[:space:]]*[1-7])*[[:space:]]*$'
        );

CREATE TABLE PATIENT_UNIT (
    patient_id UUID NOT NULL,
    unit_id UUID NOT NULL,

    CONSTRAINT pk_patient_unit PRIMARY KEY (patient_id, unit_id),
    CONSTRAINT fk_patient_unit_patient
        FOREIGN KEY (patient_id)
        REFERENCES PATIENT (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_patient_unit_unit
        FOREIGN KEY (unit_id)
        REFERENCES CLINIC_UNIT (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

CREATE INDEX idx_patient_unit_unit ON PATIENT_UNIT (unit_id, patient_id);

CREATE FUNCTION enforce_patient_unit_same_clinic()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    patient_clinic_id UUID;
    unit_clinic_id UUID;
BEGIN
    SELECT clinic_id INTO patient_clinic_id
    FROM PATIENT
    WHERE id = NEW.patient_id;

    SELECT clinic_id INTO unit_clinic_id
    FROM CLINIC_UNIT
    WHERE id = NEW.unit_id;

    IF patient_clinic_id IS DISTINCT FROM unit_clinic_id THEN
        RAISE EXCEPTION 'Paciente e unidade devem pertencer à mesma clínica'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END
$$;

CREATE TRIGGER trg_patient_unit_same_clinic
BEFORE INSERT OR UPDATE ON PATIENT_UNIT
FOR EACH ROW
EXECUTE FUNCTION enforce_patient_unit_same_clinic();

-- O histórico define todos os vínculos já comprovados do paciente.
INSERT INTO PATIENT_UNIT (patient_id, unit_id)
SELECT DISTINCT patient_id, unit_id
FROM APPOINTMENT
ON CONFLICT DO NOTHING;

-- Pacientes sem histórico são vinculados à MATRIZ de sua clínica.
INSERT INTO PATIENT_UNIT (patient_id, unit_id)
SELECT patient.id, matrix.id
FROM PATIENT patient
JOIN CLINIC_UNIT matrix
  ON matrix.clinic_id = patient.clinic_id
 AND matrix.type = 'MATRIZ'
WHERE NOT EXISTS (
    SELECT 1
    FROM PATIENT_UNIT existing
    WHERE existing.patient_id = patient.id
)
ON CONFLICT DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM PATIENT patient
        WHERE NOT EXISTS (
            SELECT 1
            FROM PATIENT_UNIT link
            WHERE link.patient_id = patient.id
        )
    ) THEN
        RAISE EXCEPTION 'V010 não conseguiu vincular todos os pacientes a uma unidade';
    END IF;
END
$$;
