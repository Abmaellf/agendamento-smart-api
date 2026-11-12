# Design

## Context

See `proposal.md` for motivation and the delta specs for behavior. `USERS` currently stores only `clinic_id`; `GET /patient/list` is public and global; `GET /api/appointments` scopes only by clinic and optional client-provided unit. These behaviors cannot implement the requested unit boundary safely.

## Goals / Non-Goals

**Goals:**

- Persist a valid user-to-unit association and backfill existing users.
- Derive effective unit scope from the authenticated `User` in one authorization boundary.
- Restrict legacy patient listing and appointment operations on the server.
- Extend `/auth/me` with non-sensitive clinic and unit display context.

**Non-Goals:**

- User-selected unit switching for non-administrators.
- Global redesign of user, patient, or unit management endpoints.
- Client-side authorization.

## Decisions

### Persist a nullable-then-required unit foreign key during migration

Add a `clinic_unit_id` relationship to `USERS`, backfill it with each user's clinic `MATRIZ` in the same versioned Flyway migration, and enforce non-null once existing rows are populated. Application services validate that a selected unit belongs to the user's clinic before persistence.

This makes scope auditable and database-backed. An unversioned data update was rejected because it is not reproducible; accepting a client-provided unit without relationship validation was rejected because it permits cross-clinic assignments.

### Centralize effective scope resolution

Introduce a small authorization/domain service that resolves allowed units from the principal: all units in its clinic for `ADMIN`, or its associated unit for `USER`. Appointment and patient services consume this result and verify request parameters against it.

Duplicating role checks across controllers was rejected because authorization behavior would drift. Frontend filtering was rejected because it cannot prevent data exposure.

### Keep the selected legacy patient endpoint and make it protected

Change `GET /patient/list` from public/global to authenticated/scoped; update the frontend API client to call this endpoint. It preserves pagination while intentionally breaking anonymous consumers, as selected by the product owner.

Adding a parallel `/api/patients` endpoint was rejected to avoid two list contracts for the same capability.

### Expose display context through `/auth/me`

Extend `UserProfileResponse` with clinic and unit names, keeping IDs available for API operations. The frontend handles absent values defensively during coordinated deployment, but new API responses provide the complete context.

## Risks / Trade-offs

- [A clinic has no MATRIZ] → validate the target unit for every clinic before updates and fail the migration transaction clearly.
- [Legacy clients rely on anonymous `/patient/list`] → document the breaking authentication requirement and update the owned frontend in the same release.
- [Existing appointments use a different unit than a backfilled user] → retain historical records, but enforce the new association only for reads/creates after deployment; validate with representative data.
- [Role comparison or ORM loading is inconsistent] → cover both roles with service and controller integration tests.

## Migration Plan

1. Add the relationship, target-unit validation and idempotent user backfill migration.
2. Add scope resolution and apply it to profile, user management, patients and appointments.
3. Make `/patient/list` authenticated, then deploy the frontend endpoint update and session display changes together.
4. Run integration tests with two units and two clinics before deployment.

Rollback retains the populated association and schema because removing it discards scope evidence. If an application rollback is required, deploy a compatible version that ignores the additional field; correct failed migrations before retrying rather than manually editing Flyway history.
