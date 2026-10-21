package dev.mfarm.com.mfarm.sync;

import android.database.Cursor;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import dev.mfarm.com.mfarm.dao.DatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public final class ExcelImporter {

    private static final String TAG = "ExcelImporter";

    public static class ImportSummary {
        public String batchUuid = UUID.randomUUID().toString();
        public String fileName = "John.xlsx";
        public int animalsImported = 0;
        public int breedingImported = 0;
        public int kiddingImported = 0;
        public int vaccinationImported = 0;
        public int disposalImported = 0;
        public int financialImported = 0;
        public int duplicateSkipped = 0;
        public List<String> ambiguousTags = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
    }

    private ExcelImporter() {}

    /**
     * Imports Flock Overview Goat Records from John.xlsx
     */
    public static int importFlockOverview(DatabaseAdapter db, List<Map<String, String>> flockRows, ImportSummary summary) {
        if (flockRows == null || flockRows.isEmpty()) return 0;

        int imported = 0;
        db.beginTransaction();
        try {
            for (Map<String, String> row : flockRows) {
                String tagNo = row.get("tag_no");
                String damId = row.get("dam_id");
                String sireId = row.get("sire_id");
                String sex = row.get("sex");
                String dob = row.get("dob");
                String origin = row.get("origin");
                String color = row.get("color");
                String comments = row.get("comments");

                if (tagNo == null || tagNo.trim().isEmpty()) continue;
                String cleanTag = normalizeTag(tagNo.trim());

                // Flag ambiguous newborn tags
                if (cleanTag.equalsIgnoreCase("New Born") || cleanTag.equalsIgnoreCase("New Born**") || cleanTag.contains("...")) {
                    if (summary != null) {
                        summary.ambiguousTags.add("Ambiguous Tag '" + cleanTag + "' for animal born " + dob + " (Dam: " + damId + ")");
                    }
                }

                // Check duplicate tag in database
                Cursor c = db.query("SELECT id FROM animas WHERE name = ? OR tag_number = ? OR ear_tag = ?", new String[]{cleanTag, cleanTag, cleanTag});
                if (c != null && c.moveToFirst()) {
                    c.close();
                    if (summary != null) summary.duplicateSkipped++;
                    continue;
                }
                if (c != null) c.close();

                Map<String, Object> values = new HashMap<>();
                values.put("name", cleanTag);
                values.put("tag_number", cleanTag);
                values.put("ear_tag", cleanTag);
                values.put("species", "Goat");
                values.put("gender", "M".equalsIgnoreCase(sex) ? "Male" : "Female");
                values.put("dob", dob);
                values.put("breed_id", 2); // Goat breed
                values.put("body_conf", color);
                values.put("dam_id", damId);
                values.put("sire_id", sireId);
                values.put("origin_source", origin);
                values.put("lifecycle_status", "Active");
                values.put("repro_status", "Open");
                values.put("notes", comments);

                long rowId = db.insert("animas", null, values);
                if (rowId != -1) {
                    imported++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (summary != null) summary.animalsImported += imported;
        return imported;
    }

    /**
     * Imports Breeding Records from John.xlsx Sheet 3
     */
    public static int importBreedingRecords(DatabaseAdapter db, List<Map<String, String>> breedingRows, ImportSummary summary) {
        if (breedingRows == null || breedingRows.isEmpty()) return 0;

        int imported = 0;
        db.beginTransaction();
        try {
            for (Map<String, String> row : breedingRows) {
                String doeTag = normalizeTag(row.get("doe_tag"));
                String buckTag = row.get("buck_tag");
                String matingDate = row.get("mating_date");
                String expKiddingDate = row.get("exp_kidding_date");
                String comments = row.get("comments");

                if (doeTag == null || doeTag.trim().isEmpty()) continue;

                // Find doe animal ID
                int animalId = -1;
                Cursor cDoe = db.query("SELECT id FROM animas WHERE name = ? OR tag_number = ?", new String[]{doeTag, doeTag});
                if (cDoe != null && cDoe.moveToFirst()) {
                    animalId = cDoe.getInt(0);
                    cDoe.close();
                }

                // Check duplicate breeding record
                Cursor c = db.query("SELECT id FROM breeding_records WHERE animal_id = ? AND mating_date = ?", new String[]{String.valueOf(animalId), matingDate});
                if (c != null && c.moveToFirst()) {
                    c.close();
                    if (summary != null) summary.duplicateSkipped++;
                    continue;
                }
                if (c != null) c.close();

                Map<String, Object> values = new HashMap<>();
                values.put("animal_id", animalId);
                values.put("bull_id", buckTag);
                values.put("mating_date", matingDate);
                values.put("expected_birth_date", expKiddingDate);
                values.put("status", "Pregnant");
                values.put("pregnancy_confirmed", 1);

                long rowId = db.insert("breeding_records", null, values);
                if (rowId != -1) {
                    imported++;
                    // Update doe repro_status to Pregnant
                    if (animalId != -1) {
                        Map<String, Object> statusVal = new HashMap<>();
                        statusVal.put("repro_status", "Pregnant");
                        db.update("animas", statusVal, "id = ?", new String[]{String.valueOf(animalId)});
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (summary != null) summary.breedingImported += imported;
        return imported;
    }

    /**
     * Imports Kidding Records from John.xlsx Sheet 4
     */
    public static int importKiddingRecords(DatabaseAdapter db, List<Map<String, String>> kiddingRows, ImportSummary summary) {
        if (kiddingRows == null || kiddingRows.isEmpty()) return 0;

        int imported = 0;
        db.beginTransaction();
        try {
            for (Map<String, String> row : kiddingRows) {
                String kidTag = normalizeTag(row.get("kid_tag"));
                String birthDate = row.get("birth_date");
                String sex = row.get("sex");
                String birthType = row.get("birth_type");
                String color = row.get("color");
                String damTag = row.get("dam_tag");
                String sireTag = row.get("sire_tag");
                String comments = row.get("comments");

                if (damTag == null || damTag.trim().isEmpty()) continue;

                // Check duplicate kidding event
                Cursor c = db.query("SELECT id FROM calving_records WHERE birth_date = ? AND notes = ?", new String[]{birthDate, "Dam: " + damTag});
                if (c != null && c.moveToFirst()) {
                    c.close();
                    if (summary != null) summary.duplicateSkipped++;
                    continue;
                }
                if (c != null) c.close();

                // Create kid animal record if tag exists
                long offspringId = -1;
                if (kidTag != null && !kidTag.trim().isEmpty() && !"n/a".equalsIgnoreCase(kidTag)) {
                    Map<String, Object> kidVal = new HashMap<>();
                    kidVal.put("name", kidTag);
                    kidVal.put("tag_number", kidTag);
                    kidVal.put("species", "Goat");
                    kidVal.put("gender", "M".equalsIgnoreCase(sex) ? "Male" : "Female");
                    kidVal.put("dob", birthDate);
                    kidVal.put("dam_id", damTag);
                    kidVal.put("sire_id", sireTag);
                    kidVal.put("body_conf", color);
                    kidVal.put("birth_type", birthType != null ? birthType : "Single");
                    kidVal.put("lifecycle_status", "Active");
                    kidVal.put("repro_status", "Open");
                    offspringId = db.insert("animas", null, kidVal);
                }

                Map<String, Object> calvingVal = new HashMap<>();
                calvingVal.put("birth_date", birthDate);
                calvingVal.put("sex", sex);
                calvingVal.put("offspring_id", offspringId);
                calvingVal.put("birth_type", birthType != null ? birthType : "Single");
                calvingVal.put("species", "Goat");
                calvingVal.put("notes", "Dam: " + damTag + ". " + (comments != null ? comments : ""));

                long rowId = db.insert("calving_records", null, calvingVal);
                if (rowId != -1) {
                    imported++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (summary != null) summary.kiddingImported += imported;
        return imported;
    }

    /**
     * Imports Vaccination Schedule protocols from John.xlsx Sheet 1
     */
    public static int importVaccinationSchedules(DatabaseAdapter db, List<Map<String, String>> vaccineRows, ImportSummary summary) {
        if (vaccineRows == null || vaccineRows.isEmpty()) return 0;

        int imported = 0;
        db.beginTransaction();
        try {
            for (Map<String, String> row : vaccineRows) {
                String disease = row.get("disease");
                String vaccine = row.get("vaccine");
                String frequency = row.get("frequency");
                String schedDate = row.get("sched_date");

                if (vaccine == null || vaccine.trim().isEmpty()) continue;

                Cursor c = db.query("SELECT id FROM vaccinations WHERE vaccine_name = ? AND scheduled_date = ?", new String[]{vaccine.trim(), schedDate});
                if (c != null && c.moveToFirst()) {
                    c.close();
                    if (summary != null) summary.duplicateSkipped++;
                    continue;
                }
                if (c != null) c.close();

                Map<String, Object> values = new HashMap<>();
                values.put("animal_id", 1); // Herd level
                values.put("vaccine_name", vaccine.trim() + " (" + disease + ")");
                values.put("scheduled_date", schedDate != null ? schedDate : "01-09-2026");
                values.put("status", "Pending");
                values.put("remarks", "Schedule: " + frequency);

                long rowId = db.insert("vaccinations", null, values);
                if (rowId != -1) {
                    imported++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (summary != null) summary.vaccinationImported += imported;
        return imported;
    }

    /**
     * Imports Disposal / Offtake records from John.xlsx
     */
    public static int importDisposalRecords(DatabaseAdapter db, List<Map<String, String>> disposalRows, ImportSummary summary) {
        if (disposalRows == null || disposalRows.isEmpty()) return 0;

        int imported = 0;
        db.beginTransaction();
        try {
            for (Map<String, String> row : disposalRows) {
                String dateStr = row.get("disposal_date");
                String tagNo = row.get("tag_no");
                String sex = row.get("sex");
                String age = row.get("age");
                String method = row.get("method");
                String cause = row.get("cause");

                if (tagNo == null || tagNo.trim().isEmpty()) continue;
                String cleanTag = normalizeTag(tagNo.trim());

                // Check duplicate disposal record
                Cursor c = db.query("SELECT id FROM disposal_records WHERE animal_tag = ? AND disposal_date = ?", new String[]{cleanTag, dateStr});
                if (c != null && c.moveToFirst()) {
                    c.close();
                    if (summary != null) summary.duplicateSkipped++;
                    continue;
                }
                if (c != null) c.close();

                // Find animal ID if exists
                long animalId = -1;
                Cursor cAnim = db.query("SELECT id FROM animas WHERE name = ? OR tag_number = ?", new String[]{cleanTag, cleanTag});
                if (cAnim != null && cAnim.moveToFirst()) {
                    animalId = cAnim.getLong(0);
                    cAnim.close();
                }

                Map<String, Object> values = new HashMap<>();
                values.put("animal_id", animalId);
                values.put("animal_tag", cleanTag);
                values.put("sex", sex);
                values.put("disposal_date", dateStr);
                values.put("method", method);
                values.put("cause_or_reason", cause);

                long rowId = db.insert("disposal_records", null, values);
                if (rowId != -1) {
                    imported++;

                    // Update animal lifecycle status if animal exists
                    if (animalId != -1) {
                        Map<String, Object> animStatus = new HashMap<>();
                        animStatus.put("lifecycle_status", "Sold".equalsIgnoreCase(method) ? "Sold" : "Died");
                        db.update("animas", animStatus, "id = ?", new String[]{String.valueOf(animalId)});
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (summary != null) summary.disposalImported += imported;
        return imported;
    }

    /**
     * Imports Financial Records tagged with Enterprise Project Codes (GT2, CW1, CF3)
     */
    public static int importFinancialRecords(DatabaseAdapter db, List<Map<String, String>> finRows, ImportSummary summary) {
        if (finRows == null || finRows.isEmpty()) return 0;

        int imported = 0;
        db.beginTransaction();
        try {
            for (Map<String, String> row : finRows) {
                String expDate = row.get("exp_date");
                String expCode = row.get("exp_code");
                String expDesc = row.get("exp_desc");
                String expAmountStr = row.get("exp_amount");

                String incDate = row.get("inc_date");
                String incCode = row.get("inc_code");
                String incDesc = row.get("inc_desc");
                String incAmountStr = row.get("inc_amount");
                String comments = row.get("comments");

                // Import Expense if present and non-empty
                if (expDesc != null && !expDesc.trim().isEmpty() && expAmountStr != null && !expAmountStr.trim().isEmpty()) {
                    try {
                        double amt = Double.parseDouble(expAmountStr.replace(",", "").trim());
                        String pCode = (expCode != null && !expCode.trim().isEmpty()) ? expCode.trim() : "GEN";

                        Cursor c = db.query("SELECT id FROM expenses WHERE description = ? AND date = ? AND amount = ?", new String[]{expDesc.trim(), expDate, String.valueOf(amt)});
                        if (c != null && c.moveToFirst()) {
                            c.close();
                            if (summary != null) summary.duplicateSkipped++;
                        } else {
                            if (c != null) c.close();
                            Map<String, Object> expVal = new HashMap<>();
                            expVal.put("category", resolveCategoryFromCode(pCode));
                            expVal.put("amount", amt);
                            expVal.put("date", expDate);
                            expVal.put("description", expDesc.trim());
                            expVal.put("project_code", pCode);
                            expVal.put("notes", comments);
                            if (db.insert("expenses", null, expVal) != -1) imported++;
                        }
                    } catch (Exception ignored) {}
                }

                // Import Income if present and non-empty
                if (incDesc != null && !incDesc.trim().isEmpty() && incAmountStr != null && !incAmountStr.trim().isEmpty()) {
                    try {
                        double amt = Double.parseDouble(incAmountStr.replace(",", "").trim());
                        String pCode = (incCode != null && !incCode.trim().isEmpty()) ? incCode.trim() : "GEN";

                        Cursor c = db.query("SELECT id FROM income WHERE description = ? AND date = ? AND amount = ?", new String[]{incDesc.trim(), incDate, String.valueOf(amt)});
                        if (c != null && c.moveToFirst()) {
                            c.close();
                            if (summary != null) summary.duplicateSkipped++;
                        } else {
                            if (c != null) c.close();
                            Map<String, Object> incVal = new HashMap<>();
                            incVal.put("category", resolveCategoryFromCode(pCode));
                            incVal.put("amount", amt);
                            incVal.put("date", incDate);
                            incVal.put("description", incDesc.trim());
                            incVal.put("project_code", pCode);
                            incVal.put("notes", comments);
                            if (db.insert("income", null, incVal) != -1) imported++;
                        }
                    } catch (Exception ignored) {}
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (summary != null) summary.financialImported += imported;
        return imported;
    }

    public static void recordImportBatch(DatabaseAdapter db, ImportSummary summary) {
        if (db == null || summary == null) return;
        try {
            Map<String, Object> batchVal = new HashMap<>();
            batchVal.put("batch_uuid", summary.batchUuid);
            batchVal.put("file_name", summary.fileName);
            batchVal.put("imported_at", System.currentTimeMillis());
            batchVal.put("record_count", summary.animalsImported + summary.breedingImported + summary.kiddingImported + summary.vaccinationImported + summary.disposalImported + summary.financialImported);
            batchVal.put("summary_json", String.format(Locale.US, "{\"animals\":%d,\"breeding\":%d,\"kidding\":%d,\"vaccinations\":%d,\"disposals\":%d,\"financials\":%d,\"skipped\":%d}",
                    summary.animalsImported, summary.breedingImported, summary.kiddingImported, summary.vaccinationImported, summary.disposalImported, summary.financialImported, summary.duplicateSkipped));
            db.insert("import_batches", null, batchVal);
        } catch (Exception e) {
            Log.w(TAG, "Failed to record import batch log", e);
        }
    }

    private static String normalizeTag(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("\\s+", " ");
    }

    private static String resolveCategoryFromCode(String code) {
        if ("GT2".equalsIgnoreCase(code)) return "Goats / Flock";
        if ("CW1".equalsIgnoreCase(code)) return "Cattle / Livestock";
        if ("CF3".equalsIgnoreCase(code)) return "Coffee / Crops";
        return "General Farm";
    }
}
