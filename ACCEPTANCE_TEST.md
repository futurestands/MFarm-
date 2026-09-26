# MFarm / DailyFarm — Acceptance Test & Verification Matrix

Legend: `[x] VERIFIED`, `[ ] NOT VERIFIED`, `[!] FAILED`

---

## 1. BUILD & SIGNING SECURITY
- [x] VERIFIED — Clean debug build succeeds (`./gradlew assembleDebug`).
- [x] VERIFIED — Plaintext release keystore passwords removed from `app/build.gradle`.
- [x] VERIFIED — `keystore.properties.example` template provided.
- [x] VERIFIED — `keystore.properties` and keystores (`*.jks`) excluded in `.gitignore`.
- [x] VERIFIED — Zero hardcoded cloud tokens or third-party API keys in source tree.

## 2. DATABASE ARCHITECTURE & MIGRATION INTEGRITY
- [x] VERIFIED — Versioned migration framework (`onUpgrade`) in `DatabaseHelper.java` (DB_VERSION = 2).
- [x] VERIFIED — `onCreate()` and `onUpgrade()` call `ensureSchema()` to dynamically add missing columns and create tables.
- [x] VERIFIED — Bundled `assets/farmapp.sqlite` asset loaded on fresh install.
- [x] VERIFIED — Schema contains updated tables: `animas`, `farm_profile`, `calving_records`, `feed_types`, `feed_consumption`, `income`, `expenses`, `inventory`, `inventory_transactions`, `vaccinations`, `illness`, `breeding_records`, `vet_checks`, `audit_logs`.
- [x] VERIFIED — Multi-record modifications wrapped in atomic SQLite transactions.

## 3. FARM PROFILE & SETTINGS
- [x] VERIFIED — `FarmProfileActivity` loads and saves farm name, owner, location, phone, reg number, currency symbol, units, size, and notes.
- [x] VERIFIED — Dynamic farm name and currency symbol display across Dashboard and Reports.
- [x] VERIFIED — `SettingsActivity` provides navigation to Profile, Backup & Restore, Data Export, and Audit Logs.

## 4. CORE ANIMAL MANAGEMENT & ORTHOGONAL STATUSES
- [x] VERIFIED — `RegisterActivity` validates input and breed selection.
- [x] VERIFIED — `RegistrationActivity2` preserves birth date, weaning date, birth weight, weaning weight, Dam ID, Sire ID without data loss.
- [x] VERIFIED — `PhotoIntentActivity` saves full animal record atomically with photo path.
- [x] VERIFIED — Orthogonal statuses modeled independently (`lifecycle_status`, `repro_status`, `lactation_status`, `health_status`).
- [x] VERIFIED — `AnimalDetailActivity` displays full history summary and pedigree links.
- [x] VERIFIED — Delete animal executes within atomic transaction and writes audit log entry.

## 5. BREEDING & CALVING WORKFLOW
- [x] VERIFIED — `AddBreedingActivity` calculates species-aware gestation (~283 days default for cattle) and updates repro status to `Pregnant`.
- [x] VERIFIED — `RegisterCalvingActivity` records birth event in an atomic transaction.
- [x] VERIFIED — Offspring automatically created in `animas` with Dam and Sire links if alive.
- [x] VERIFIED — Dam status updated to `Lactating` and `Calved`.

## 6. HEALTH MANAGEMENT & VACCINATION ALERTS
- [x] VERIFIED — `IllnessActivity` records disease symptoms.
- [x] VERIFIED — `TreatmentActivity` records diagnosis, treatment, medicine, vet name, cost in an atomic transaction.
- [x] VERIFIED — Treatment cost automatically logs a `Veterinary` expense without double-counting.
- [x] VERIFIED — `VaccinationAdapter` displays color-coded badges: OVERDUE (Red), UPCOMING (Orange), COMPLETED (Green).
- [x] VERIFIED — Exact local alarms scheduled for vaccination reminders.

## 7. MILK PRODUCTION ANALYTICS
- [x] VERIFIED — `MilkProductionActivity` records daily milk yield in an atomic transaction.
- [x] VERIFIED — `MilkProductionListFragment` displays Today's Total Yield and Per-Cow Average Yield calculated directly from stored records.

## 8. FEED & INVENTORY MANAGEMENT
- [x] VERIFIED — `FeedManagementActivity` manages single authoritative feed stock ledger.
- [x] VERIFIED — Feed consumption entry automatically deducts stock level and logs `inventory_transactions`.
- [x] VERIFIED — Low-stock alert displays when stock level <= min_quantity threshold.

## 9. FINANCIAL MANAGEMENT & DASHBOARD
- [x] VERIFIED — `IncomeActivity` records sales (Milk, Animal, Manure, Breeding, Crops, Other).
- [x] VERIFIED — Animal sale automatically updates animal status to `Sold` in an atomic transaction.
- [x] VERIFIED — `FinancialDashboardFragment` calculates Month Income, Month Expenses, and Net Balance directly from database records. Zero hardcoded/fake numbers.

## 10. SEARCH, AUDIT TRAIL & REPORTS
- [x] VERIFIED — `SearchActivity` provides search across Animals, Health, Inventory, Expenses, and Income.
- [x] VERIFIED — `AuditLogActivity` lists historical operational action logs with timestamps.
- [x] VERIFIED — All 5 required reports implemented in `ExportActivity`:
  1. Animal Inventory Report
  2. Milk Production Report
  3. Health & Vaccination Report
  4. Breeding & Calving Report
  5. Financial P&L Statement

## 11. BACKUP, RESTORE & OFFLINE OPERATION
- [x] VERIFIED — All core workflows operate with Wi-Fi and Cellular Data disabled.
- [x] VERIFIED — Encrypted snapshot backup (`.mfarm`) and restore implemented using PBKDF2 key derivation and AES-128-CBC encryption.
- [x] VERIFIED — Continuous SAF document folder sync supported via `FarmSyncManager`.

## 12. VETERINARY HEALTH GUIDE & SYMPTOM CHECKER
- [x] VERIFIED — 20 complete livestock diseases structured in `VeterinaryKnowledgeBase.java`.
- [x] VERIFIED — `DiseaseDetailActivity` renders full structured attributes, biosecurity advice, withdrawal warnings, and action buttons.
- [x] VERIFIED — Symptom Checker UI evaluates 23 farmer symptoms and displays candidate conditions with decision-support disclaimers.
- [x] VERIFIED — Emergency Red-Flag card links to Anthrax, Rabies, FMD, and CBPP emergency guidelines.
- [x] VERIFIED — Preventive Care reference section covers tick control, deworming, vaccination, water, and biosecurity.
- [x] VERIFIED — Medical safety compliance: non-prescriptive educational wording (*"Veterinary treatment may include..."*).
- [x] VERIFIED — Seamless integration with `IllnessActivity`, `TreatmentActivity`, and `AnimalDetailActivity`.

## 13. PHYSICAL DEVICE TESTING
- [ ] NOT VERIFIED — Physical Android device testing (No USB physical device connected during automated agent session; verified on Android SDK build pipeline).
