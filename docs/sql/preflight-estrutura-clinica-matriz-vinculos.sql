\set ON_ERROR_STOP on
\pset pager off

-- Pré-verificação somente leitura para a change
-- estrutura-clinica-matriz-vinculos.
--
-- O relatório expõe apenas identificadores técnicos e contagens. Nomes,
-- logins e demais dados pessoais não são selecionados.
-- Execute depois de V001--V009 e antes da migration desta change.

BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;

\echo '1. Clínicas sem unidade (esperado: nenhuma linha)'
SELECT
    c.id AS clinic_id,
    COUNT(cu.id) AS unit_count
FROM clinic c
LEFT JOIN clinic_unit cu ON cu.clinic_id = c.id
GROUP BY c.id
HAVING COUNT(cu.id) = 0
ORDER BY c.id;

\echo '2. Clínicas com quantidade de ADMIN diferente de 1 (esperado: nenhuma linha)'
SELECT
    c.id AS clinic_id,
    COUNT(u.id) FILTER (WHERE u.role = 'ADMIN') AS admin_count,
    COUNT(u.id) AS user_count
FROM clinic c
LEFT JOIN users u ON u.clinic_id = c.id
GROUP BY c.id
HAVING COUNT(u.id) FILTER (WHERE u.role = 'ADMIN') <> 1
ORDER BY c.id;

\echo '3. Unidades candidatas a MATRIZ'
WITH appointment_usage AS (
    SELECT
        a.clinic_id,
        a.unit_id,
        COUNT(*) AS appointment_count
    FROM appointment a
    GROUP BY a.clinic_id, a.unit_id
), unit_candidates AS (
    SELECT
        cu.clinic_id,
        cu.id AS unit_id,
        COUNT(*) OVER (PARTITION BY cu.clinic_id) AS unit_count,
        COALESCE(au.appointment_count, 0) AS appointment_count
    FROM clinic_unit cu
    LEFT JOIN appointment_usage au
        ON au.clinic_id = cu.clinic_id
       AND au.unit_id = cu.id
)
SELECT
    clinic_id,
    unit_id,
    unit_count,
    appointment_count,
    CASE
        WHEN unit_count = 1 THEN 'UNICA_UNIDADE'
        ELSE 'DECISAO_DE_MATRIZ_NECESSARIA'
    END AS migration_decision
FROM unit_candidates
ORDER BY clinic_id, unit_id;

\echo '4. Profissionais usados em appointments de mais de uma unidade'
SELECT
    p.clinic_id,
    p.id AS professional_id,
    COUNT(DISTINCT a.unit_id) AS appointment_unit_count,
    COUNT(a.id) AS appointment_count,
    ARRAY_AGG(DISTINCT a.unit_id ORDER BY a.unit_id) AS appointment_unit_ids
FROM professional p
JOIN appointment a
    ON a.clinic_id = p.clinic_id
   AND a.professional_id = p.id
GROUP BY p.clinic_id, p.id
HAVING COUNT(DISTINCT a.unit_id) > 1
ORDER BY p.clinic_id, p.id;

\echo '5. Estratégia possível para vínculo de cada profissional'
SELECT
    p.clinic_id,
    p.id AS professional_id,
    COUNT(DISTINCT a.unit_id) AS appointment_unit_count,
    CASE
        WHEN COUNT(DISTINCT a.unit_id) = 0
            THEN 'USAR_MATRIZ_APOS_DEFINICAO'
        WHEN COUNT(DISTINCT a.unit_id) = 1
            THEN 'UNIDADE_DOS_APPOINTMENTS'
        ELSE 'DECISAO_DE_UNIDADE_NECESSARIA'
    END AS migration_decision
FROM professional p
LEFT JOIN appointment a
    ON a.clinic_id = p.clinic_id
   AND a.professional_id = p.id
GROUP BY p.clinic_id, p.id
ORDER BY p.clinic_id, p.id;

COMMIT;
