# Backend implementation decisions (SRS v1.0)

These decisions resolve ambiguities in `Conversation_Hub_SRS.docx` without changing its requirements.

| Topic | SRS tension | Backend decision |
|---|---|---|
| Identity verification (FR-1.3, FR-1.5) vs. no SMS/email (FR-9.6, NFR-8.4) | A phone/email must be verified and password recovery must exist, but no external message transport is available in release 1. | Verification codes and password-reset tokens are generated server-side, stored hashed with a TTL, and returned once in the API response for the authenticated client flow. No SMS/email/push service is used. Swapping in a transport later does not change the challenge API. |
| Guest role (FR-2.1) | Guests have no account, so no stored role assignment is meaningful. | `GUEST` exists as a role value, but guest access is represented by unauthenticated (`permitAll`) public endpoints rather than a user row. |
| Slot capacity (FR-6.4, FR-6.6, FR-7.9, BR-09) | Capacity may be interpreted as consumed at submission or at approval. | One unit is **held at submission** under a pessimistic lock. Approval keeps the hold; rejection, expiry, withdrawal and cancellation release it exactly once. This prevents oversubscription under concurrent requests. |
| Provider profile edits (FR-4.7) | Verified fields must not change public data until re-verified. | Edited values are stored as pending values; the currently published snapshot stays visible. An authorized verifier publishes the new values or rejects them. |
| Cancellation / no-show consequences (FR-8.8) | Rules are provider-defined; no detailed policy format is specified. | Providers configure a cancellation cutoff (hours), no-show rule text and a restriction window (hours). Late cancellation/no-show can temporarily block new bookings. |
| Service catalogue configuration (NFR-9.3, FR-4.8) | Six service types and four categories must exist at launch without hard-coded workflow branches. | Service types are seeded from `app.catalog.services[*]` configuration and manageable through an admin API. The request/approval workflow uses the provider's service category only for routing, so new types need no workflow code changes. |
| Data retention (FR-12.3, NFR-5.6) | A period is required but not specified. | `app.audit.retention-days` (default 365) controls the audit cleanup job; booking records are retained for historical reporting. |
| HTTPS (NFR-4.1) | Local development and automated tests use plain HTTP, and Spring Security 7 removed the legacy channel API. | A small servlet filter redirects HTTP to HTTPS when `app.security.require-https=true`; it is disabled by default for local development and must be enabled in production where TLS is terminated. It also honours `X-Forwarded-Proto: https` from a trusted reverse proxy. |
| Booking-for (FR-1.8, FR-1.9, BR-16) | Attendee data must be available to approvers. | Attendee name and relationship are stored on the booking and included in approver-facing booking payloads. |
| Payments (BR-11, BR-12) | Service charges may be displayed. | Charges are informational text only; no payment client, credentials, or enforcement exists. |

## Running backend tests

The repository contains an H2 in-memory test profile so the context and workflow tests do not need a local MySQL server:

```bash
cd conversationHub
./mvnw -o test
```

Application configuration for real deployments still expects MySQL (`application.properties`, `ddl-auto=validate`). `src/main/resources/schema.sql` is an idempotent schema/migration script: it creates missing tables, adds missing columns with safe defaults, updates enum/type definitions, and then Hibernate validates the final structure. It works on both a fresh database and the original release-1 database. Duplicate constraint statements are ignored via `spring.sql.init.continue-on-error=true`. The H2 profile is activated only by tests via `src/test/resources/application-test.properties`.

Deployment-owned requirements: daily backups and a tested restore (NFR-3.4/3.5), 99% availability monitoring (NFR-3.1), HTTPS termination/certificates (NFR-4.1), and confirming the applicable data-protection jurisdiction (NFR-5.7).
