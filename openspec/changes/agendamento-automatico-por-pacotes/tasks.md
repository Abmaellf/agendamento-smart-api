## 1. Persistence and domain

- [x] 1.1 Add migration for package contract, snapshot weekdays and nullable appointment FK.
- [x] 1.2 Add entities, repositories, DTOs and decimal-safe mapping.
- [x] 1.3 Implement date generation from minimum date and effective weekdays only.

## 2. Application and API

- [x] 2.1 Reuse the existing AppointmentService booking boundary for generated commands.
- [x] 2.2 Implement preview with fingerprint and no persistence.
- [x] 2.3 Implement atomic confirmation with idempotency and rollback.
- [x] 2.4 Expose preview, confirmation and detail endpoints with authorization.

## 3. Verification and documentation

- [ ] 3.1 Add unit/integration tests for dates, conflicts, scope, transaction and idempotency.
- [ ] 3.2 Preserve manual appointment regression coverage.
- [ ] 3.3 Update module READMEs and run `./mvnw clean verify`.
- [x] 3.4 Run `openspec validate agendamento-automatico-por-pacotes --strict`.
