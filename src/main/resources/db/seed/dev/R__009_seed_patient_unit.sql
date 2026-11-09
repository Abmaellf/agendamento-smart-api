-- Seed exclusivo para desenvolvimento e demonstração.
-- Mantém os vínculos inferidos do histórico da R__008 e torna explícita a
-- unidade de pacientes que venham a existir sem appointments.
INSERT INTO PATIENT_UNIT (patient_id, unit_id)
SELECT DISTINCT patient_id, unit_id
FROM APPOINTMENT
ON CONFLICT DO NOTHING;

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
