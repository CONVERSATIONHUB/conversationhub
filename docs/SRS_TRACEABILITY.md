# SRS traceability matrix

Source: `Conversation_Hub_SRS.docx` v1.0 (22 November 2026). Backend scope only; frontend templates are tracked separately.
Implementation notes and ambiguity resolutions: `docs/BACKEND_DECISIONS.md`.

Legend: **B** = backend API/service, **C** = configuration only, **O** = operational/deployment concern, **–** = out of release scope.

## 1. Accounts and identity

| ID | Scope | Where satisfied |
|---|---|---|
| FR-1.1 | B | Public catalog endpoints (`PublicController`, `PublicCatalogService`), permitAll rules in `SecurityConfig` |
| FR-1.2 | B | Seeker-only guard in `BookingService.requestBooking` |
| FR-1.3 | B | `VerificationChallenge`, `IdentityService`, seeker registration flow |
| FR-1.4 | B | `RegisterSeekerRequest`, `AccountService.createUser` |
| FR-1.5 | B | `PasswordResetToken`, `IdentityService` forgot/reset endpoints |
| FR-1.6 | B | `AccountType` separation, single account type per `AppUser` |
| FR-1.7 | B | `BookingService.requestBooking` seeker check |
| FR-1.8 | B | `CreateBookingRequest`, `Booking` attendee fields |
| FR-1.9 | B | `BookingView` attendee fields on approver-facing lists/detail |
| FR-1.10 | B | `GET /api/bookings/mine` |
| FR-1.11 | B | `POST /api/me/contact` and `/api/me/contact/verify` |

## 2. Roles and permissions

| ID | Scope | Where satisfied |
|---|---|---|
| FR-2.1 | B/C | `AccountType` roles incl. `GUEST`, `RoleBootstrap`, permitAll public endpoints |
| FR-2.2 | B | `SecurityConfig` URL rules + service-level scope checks |
| FR-2.3 | B | Booking, notification and catalog endpoints scoped by role/owner |
| FR-2.4 | B | Institution-scoped booking lists, decisions and availability checks |
| FR-2.5 | B | `InstitutionService`, `StaffService`, `ProviderManagementService` manager operations |
| FR-2.6 | B | Provider-only schedule/requests; no decision branch for institutional providers |
| FR-2.7 | B | `ProviderProfileService`, `ProviderManagementService`, booking decisions |
| FR-2.8 | B | Admin controllers for institutions, providers, users, settings, audit |
| FR-2.9 | B | `BookingService.ensureNotSelf` |
| FR-2.10 | B | Owner/institution/provider scoping in services |
| FR-2.11 | B | `AuditService` role-assignment actions in `StaffService`, `UserAdminService` |

## 3. Institutions and partners

| ID | Scope | Where satisfied |
|---|---|---|
| FR-3.1 | B | Admin-only `InstitutionService.registerInstitution` with contact fields |
| FR-3.2 | B | `InstitutionStatus` transitions |
| FR-3.3 | B | `Provider.published`, gating in `PublicCatalogService` and `BookingService` |
| FR-3.4 | B | `InstitutionService.changeStatus` |
| FR-3.5 | B | Single-manager enforcement in `InstitutionService`/`StaffService` |
| FR-3.6 | B | Officer assignment/removal in `StaffService` |
| FR-3.7 | B | `InstitutionType` |
| FR-3.8 | B | Status changes only; booking rows never deleted |

## 4. Provider profiles

| ID | Scope | Where satisfied |
|---|---|---|
| FR-4.1 | B | `ProviderManagementService.createInstitutionalProvider` |
| FR-4.2 | B | Institution verification before `published=true` |
| FR-4.3 | B | Independent registration starts pending/unpublished |
| FR-4.4 | B | `RegisterIndependentProviderRequest`, `VerificationRecord` identity/qualification fields |
| FR-4.5 | B | `ProviderView.verificationStatus` |
| FR-4.6 | B | Admin-only independent publish/reject |
| FR-4.7 | B | `ProviderProfileService` pending values + re-verification |
| FR-4.8 | B/C | `app.catalog.services[*]`, `CatalogSeedService`, `ServiceController` |
| FR-4.9 | B | Provider deactivation endpoint; bookings preserved |
| FR-4.10 | B | Published/verified/active guards in public catalog and booking service |

## 5. Catalogue and discovery

| ID | Scope | Where satisfied |
|---|---|---|
| FR-5.1 | B/C | `ServiceCategory`, seeded services |
| FR-5.2 | B | Filter parameters on public service/provider endpoints |
| FR-5.3 | B | Enriched `ProviderView` and `ProviderDetailView` |
| FR-5.4 | B | `GET /api/public/providers/{id}` |
| FR-5.5 | B | Informational `chargeInformation` text; no payment code |
| FR-5.6 | B | `ServiceMode` + provider location |
| FR-5.7 | B | `ProviderView.hasAvailableSlots`, slot list endpoints |

## 6. Availability and capacity

| ID | Scope | Where satisfied |
|---|---|---|
| FR-6.1 | B | `AvailabilityService.createSlot` |
| FR-6.2 | B | Own-provider authorization in `AvailabilityService` |
| FR-6.3 | B | `ServiceEntity.durationMinutes` |
| FR-6.4 | B | `AvailabilitySlot.capacity`, validated on create/update |
| FR-6.5 | B | Past-slot rejection in request and slot creation |
| FR-6.6 | B | Locked hold-at-request capacity check |
| FR-6.7 | B | Block/unblock endpoints; bookings retained |
| FR-6.8 | B | Public slot list filters blocked/full/past slots |
| FR-6.9 | B | `SlotRecurrence`, `SlotRecurrenceService` |
| FR-6.10 | B | Idempotent `releaseSlot` on reject/expire/withdraw/cancel |

## 7. Requests and approval

| ID | Scope | Where satisfied |
|---|---|---|
| FR-7.1 | B | `CreateBookingRequest`, `BookingService.requestBooking` |
| FR-7.2 | B | Attendee name/relationship fields |
| FR-7.3 | B | Deadline/rule snapshot on `Booking` |
| FR-7.4 | B | `BookingStatus.PENDING` default |
| FR-7.5 | B | Officer/manager notification routing for institutional providers |
| FR-7.6 | B | Direct-to-provider routing for independent providers |
| FR-7.7 | B | Enriched `BookingView` for approvers |
| FR-7.8 | B | Approve/reject endpoints with optional reason |
| FR-7.9 | B | Hold retained on approval; `CONFIRMED` status |
| FR-7.10 | B | Rejection releases hold, stores reason, notifies seeker |
| FR-7.11 | B | `validateSlotForApproval` |
| FR-7.12 | B | `BookingExpiryJob` + `expirePendingBookings` |
| FR-7.13 | B | `EXPIRED` status + capacity release |
| FR-7.14 | B | Deadline in provider view, booking detail and notification |
| FR-7.15 | B | Same-day future slots accepted when lead time is zero |
| FR-7.16 | B | Withdraw endpoint + `WITHDRAWN` status |
| FR-7.17 | B | Submitted/decided/withdrawn/cancelled/expired timestamps |
| FR-7.18 | B | Notification on every implemented transition |
| FR-7.19 | B | `Provider.minLeadTimeHours` enforced at request time |

## 8. Rescheduling, cancellation, no-shows

| ID | Scope | Where satisfied |
|---|---|---|
| FR-8.1 | B | Seeker cancellation with cutoff enforcement |
| FR-8.2 | B | Staff cancellation with mandatory reason |
| FR-8.3 | B | Capacity release on cancellation |
| FR-8.4 | B | `RescheduleRequest` + endpoints |
| FR-8.5 | B | Reschedule decisions use the same decider rules |
| FR-8.6 | B | `markNoShow` after slot end |
| FR-8.7 | B | `NO_SHOW` status exposed in booking lists |
| FR-8.8 | B | Restriction window after late cancellation/no-show |
| FR-8.9 | B | Cancellation/no-show rules in provider and booking views |

## 9. Notifications

| ID | Scope | Where satisfied |
|---|---|---|
| FR-9.1 | B | `NotificationService` calls on all transitions |
| FR-9.2 | B | `GET /api/notifications` for owners/institution users |
| FR-9.3 | B | `Notification.read` + read endpoints |
| FR-9.4 | B | Recipient language captured and rendered via `MessageSource` |
| FR-9.5 | B | `BookingView.currentDay` flag |
| FR-9.6 | B | In-app persistence only; no external notification clients |

## 10. Reporting and monitoring

| ID | Scope | Where satisfied |
|---|---|---|
| FR-10.1 | B | `GET /api/admin/reports/platform` grouped counts |
| FR-10.2 | B | `ReportingService.institutionStats` |
| FR-10.3 | B | `GET /api/bookings/provider` history |
| FR-10.4 | B | `GET /api/admin/audit` |
| FR-10.5 | B | CSV export endpoints in `ReportController` |

## 11. Language support

| ID | Scope | Where satisfied |
|---|---|---|
| FR-11.1 | B | `messages*.properties`, localized DTO labels and messages |
| FR-11.2 | B | `?lang=` switch + `/api/me/language` |
| FR-11.3 | B | `LanguagePreference` + persistent locale resolution |
| FR-11.4 | B | Localized notification/validation message keys |
| FR-11.5 | B | `AppSetting` default language + admin settings endpoint |

## 12. Audit and records

| ID | Scope | Where satisfied |
|---|---|---|
| FR-12.1 | B | `AuditService` on institution/provider/availability/booking actions |
| FR-12.2 | B | No hard deletes of bookings; soft deactivation |
| FR-12.3 | B/C | `AuditRetentionService` + `app.audit.retention-days` |

## Non-functional and business rules

| ID(s) | Scope | Where satisfied |
|---|---|---|
| NFR-1.x, NFR-2.x | C/O | Performance/scalability targets: stateless services, indexed FKs, DB-backed history; measured at deployment |
| NFR-3.1, NFR-3.4, NFR-3.5 | O | Availability, daily backups and restore drill are deployment responsibilities |
| NFR-3.2 | B | No booking deletes; status-only transitions |
| NFR-3.3 | B | Pessimistic locks on slot/booking updates and expiry |
| NFR-4.1 | B/C | `HttpsEnforcementFilter` + `app.security.require-https` |
| NFR-4.2 | B | BCrypt password hashing |
| NFR-4.3/4.4 | B | URL rules + service-level role/owner/institution scoping |
| NFR-4.5 | B | Bean validation on request DTOs; output escaping by view layer |
| NFR-4.6 | C | `server.servlet.session-timeout` |
| NFR-4.7 | B | Admin audit log endpoint |
| NFR-4.8 | B | Verification details only under `/api/admin/**` |
| NFR-5.1/5.2 | B | Minimal data model; no clinical/counselling content fields |
| NFR-5.3 | B | Consent field required at registration |
| NFR-5.4 | B | Seeker contact details only in scoped booking/report payloads |
| NFR-5.5 | B | `DeletionRequest` workflow with anonymization |
| NFR-5.6 | B/C | Retention configuration + audit cleanup |
| NFR-5.7 | O | Jurisdiction-specific compliance confirmed before deployment |
| NFR-6.x, NFR-7.x, NFR-8.2 | – | Frontend/accessibility work (next phase) |
| NFR-8.1/8.3/8.4 | – | Browser compatibility and no native app / external notifications |
| NFR-9.1 | B | Logging/exception handler context |
| NFR-9.2 | – | This document |
| NFR-9.3 | B/C | Config-driven service catalogue and default language |
| NFR-10.1–10.4 | B | No payment, clinical-system or non-browser interfaces |
| BR-01–BR-16 | B/C | Enforced in service-layer guards and configuration; see sections above |
| Section 6 exclusions | – | Not implemented by design |
| Section 7 deferred | – | Not implemented by design |

## Verification

`docs/BACKEND_DECISIONS.md` records how ambiguous requirements were resolved. Automated verification: `cd conversationHub && ./mvnw -o test` (H2 test profile).
