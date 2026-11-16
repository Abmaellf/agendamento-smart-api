## ADDED Requirements

### Requirement: Contract stores an immutable package snapshot
The API MUST create a patient-specific contract containing clinic, unit, patient,
service, session count, price, requested date, manual time and effective
weekdays. Existing appointments MAY remain unlinked; generated appointments MUST
reference the contract.

#### Scenario: Contract snapshot
- **WHEN** a valid package is confirmed for a patient
- **THEN** the API MUST persist the package conditions and effective weekdays as an immutable snapshot

### Requirement: Dates use effective weekdays
The API MUST walk forward from the minimum local date and create exactly the
configured number of sessions whose weekdays are in the effective set. It MUST
NOT consult or infer holidays, unit operation or calendar blocks in this change.

#### Scenario: Date generation
- **WHEN** the minimum date is not an effective weekday
- **THEN** the first generated date MUST be the next matching effective weekday

### Requirement: Preview does not reserve
Preview MUST return all sessions, dates, manual time, conflicts, totals and a
fingerprint without writing appointments or a contract.

#### Scenario: Non-persistent preview
- **WHEN** a client requests a preview
- **THEN** no appointment or contract row MUST be written

### Requirement: Confirmation is atomic and idempotent
Confirmation MUST revalidate package scope, patient, effective weekdays, manual
time and existing appointment rules, then create snapshot and appointments in a
single transaction. Repeated `Idempotency-Key` requests MUST return one contract.

#### Scenario: Idempotent confirmation
- **WHEN** the same idempotency key is submitted more than once
- **THEN** the API MUST return the original contract without duplicating appointments

### Requirement: Authorization is scoped
The API MUST derive clinic and unit authorization from the authenticated principal
and MUST NOT reveal packages, patients or contracts outside that scope.

#### Scenario: Scope isolation
- **WHEN** a principal requests a package outside its clinic or unit scope
- **THEN** the API MUST reject the request without revealing the resource
