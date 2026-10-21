package dev.mfarm.com.mfarm.dao;

import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

public final class BatchManager {

    private BatchManager() {}

    /**
     * Creates a new livestock batch and logs its initial registration/purchase in the ledger.
     */
    public static long createBatch(DatabaseAdapter db, String batchId, String species, String subtype,
                                  String breed, String purpose, int initQty, String acqDate,
                                  double ageWeeks, String supplier, String location, String housing,
                                  double cost, String notes) {
        if (initQty <= 0) {
            throw new IllegalArgumentException("Initial batch quantity must be greater than zero");
        }

        db.beginTransaction();
        try {
            Map<String, Object> batchVal = new HashMap<>();
            batchVal.put("batch_id", batchId);
            batchVal.put("species", species);
            batchVal.put("subtype", subtype);
            batchVal.put("breed_or_strain", breed);
            batchVal.put("purpose", purpose);
            batchVal.put("initial_quantity", initQty);
            batchVal.put("current_quantity", initQty);
            batchVal.put("acquisition_date", acqDate);
            batchVal.put("age_weeks", ageWeeks);
            batchVal.put("source_supplier", supplier);
            batchVal.put("location_name", location);
            batchVal.put("housing_unit", housing);
            batchVal.put("purchase_cost", cost);
            batchVal.put("status", "Active");
            batchVal.put("notes", notes);

            long rowId = db.insert("livestock_batches", null, batchVal);

            Map<String, Object> ledgerVal = new HashMap<>();
            ledgerVal.put("batch_id", batchId);
            ledgerVal.put("event_type", cost > 0 ? "Purchase" : "Registration");
            ledgerVal.put("event_date", acqDate);
            ledgerVal.put("quantity_change", initQty);
            ledgerVal.put("resulting_quantity", initQty);
            ledgerVal.put("total_amount", cost);
            ledgerVal.put("source_ref", supplier);
            ledgerVal.put("notes", "Initial registration of " + initQty + " " + subtype);

            db.insert("batch_quantity_ledger", null, ledgerVal);

            // Log purchase expense if cost > 0
            if (cost > 0) {
                Map<String, Object> expVal = new HashMap<>();
                expVal.put("category", "Livestock Acquisition");
                expVal.put("amount", cost);
                expVal.put("date", acqDate);
                expVal.put("description", "Acquisition of " + initQty + " " + subtype + " (" + batchId + ")");
                expVal.put("project_code", resolveEnterpriseCode(species));
                db.insert("expenses", null, expVal);
            }

            db.setTransactionSuccessful();
            return rowId;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Records batch mortality/culling and updates ledger and current batch quantity.
     */
    public static void recordMortality(DatabaseAdapter db, String batchId, int count, String dateStr, String cause, String notes) {
        if (count <= 0) throw new IllegalArgumentException("Mortality count must be greater than zero");

        db.beginTransaction();
        try {
            int currentQty = getCurrentQuantity(db, batchId);
            if (count > currentQty) {
                throw new IllegalStateException("Cannot record mortality of " + count + " animals: only " + currentQty + " remaining in batch " + batchId);
            }

            int newQty = currentQty - count;

            Map<String, Object> batchVal = new HashMap<>();
            batchVal.put("current_quantity", newQty);
            if (newQty == 0) {
                batchVal.put("status", "Closed (Depleted)");
            }
            db.update("livestock_batches", batchVal, "batch_id = ?", new String[]{batchId});

            Map<String, Object> ledgerVal = new HashMap<>();
            ledgerVal.put("batch_id", batchId);
            ledgerVal.put("event_type", "Death/Mortality");
            ledgerVal.put("event_date", dateStr);
            ledgerVal.put("quantity_change", -count);
            ledgerVal.put("resulting_quantity", newQty);
            ledgerVal.put("source_ref", cause);
            ledgerVal.put("notes", "Mortality: " + count + " died due to " + cause + ". " + (notes != null ? notes : ""));

            db.insert("batch_quantity_ledger", null, ledgerVal);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Records batch sale, updates ledger and current batch quantity, and logs income.
     */
    public static void recordBatchSale(DatabaseAdapter db, String batchId, int count, String dateStr, double totalAmount, String buyer, String notes) {
        if (count <= 0) throw new IllegalArgumentException("Sale count must be greater than zero");

        db.beginTransaction();
        try {
            int currentQty = getCurrentQuantity(db, batchId);
            if (count > currentQty) {
                throw new IllegalStateException("Cannot sell " + count + " animals: only " + currentQty + " remaining in batch " + batchId);
            }

            int newQty = currentQty - count;
            String species = getBatchSpecies(db, batchId);

            Map<String, Object> batchVal = new HashMap<>();
            batchVal.put("current_quantity", newQty);
            if (newQty == 0) {
                batchVal.put("status", "Closed (Sold)");
            }
            db.update("livestock_batches", batchVal, "batch_id = ?", new String[]{batchId});

            Map<String, Object> ledgerVal = new HashMap<>();
            ledgerVal.put("batch_id", batchId);
            ledgerVal.put("event_type", "Sale");
            ledgerVal.put("event_date", dateStr);
            ledgerVal.put("quantity_change", -count);
            ledgerVal.put("resulting_quantity", newQty);
            ledgerVal.put("unit_price", count > 0 ? (totalAmount / count) : 0);
            ledgerVal.put("total_amount", totalAmount);
            ledgerVal.put("source_ref", buyer);
            ledgerVal.put("notes", "Sale of " + count + " items to " + buyer + ". " + (notes != null ? notes : ""));

            db.insert("batch_quantity_ledger", null, ledgerVal);

            if (totalAmount > 0) {
                Map<String, Object> incVal = new HashMap<>();
                incVal.put("category", "Livestock Sales");
                incVal.put("amount", totalAmount);
                incVal.put("buyer", buyer);
                incVal.put("date", dateStr);
                incVal.put("description", "Sale of " + count + " " + species + " from batch " + batchId);
                incVal.put("project_code", resolveEnterpriseCode(species));
                db.insert("income", null, incVal);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Converts a single animal from a batch to individual tracking without double counting.
     */
    public static long convertBatchToIndividual(DatabaseAdapter db, String batchId, String dateStr, String name, String tagNo, String sex, String color, String notes) {
        db.beginTransaction();
        try {
            int currentQty = getCurrentQuantity(db, batchId);
            if (currentQty <= 0) {
                throw new IllegalStateException("Batch " + batchId + " is depleted and has zero animals remaining");
            }

            int newQty = currentQty - 1;
            String species = getBatchSpecies(db, batchId);

            Map<String, Object> batchVal = new HashMap<>();
            batchVal.put("current_quantity", newQty);
            if (newQty == 0) {
                batchVal.put("status", "Closed (Converted)");
            }
            db.update("livestock_batches", batchVal, "batch_id = ?", new String[]{batchId});

            Map<String, Object> ledgerVal = new HashMap<>();
            ledgerVal.put("batch_id", batchId);
            ledgerVal.put("event_type", "Conversion_To_Individual");
            ledgerVal.put("event_date", dateStr);
            ledgerVal.put("quantity_change", -1);
            ledgerVal.put("resulting_quantity", newQty);
            ledgerVal.put("notes", "Converted 1 animal to individual tracking (Tag: " + tagNo + ")");

            db.insert("batch_quantity_ledger", null, ledgerVal);

            Map<String, Object> animVal = new HashMap<>();
            animVal.put("name", name);
            animVal.put("tag_number", tagNo);
            animVal.put("ear_tag", tagNo);
            animVal.put("species", species);
            animVal.put("gender", sex);
            animVal.put("dob", dateStr);
            animVal.put("body_conf", color);
            animVal.put("lifecycle_status", "Active");
            animVal.put("repro_status", "Open");
            animVal.put("notes", "Converted from batch " + batchId + ". " + (notes != null ? notes : ""));

            long newAnimalId = db.insert("animas", null, animVal);

            db.setTransactionSuccessful();
            return newAnimalId;
        } finally {
            db.endTransaction();
        }
    }

    public static int getCurrentQuantity(DatabaseAdapter db, String batchId) {
        Cursor c = db.query("SELECT current_quantity FROM livestock_batches WHERE batch_id = ?", new String[]{batchId});
        if (c != null && c.moveToFirst()) {
            int qty = c.getInt(0);
            c.close();
            return qty;
        }
        if (c != null) c.close();
        return 0;
    }

    public static String getBatchSpecies(DatabaseAdapter db, String batchId) {
        Cursor c = db.query("SELECT species FROM livestock_batches WHERE batch_id = ?", new String[]{batchId});
        if (c != null && c.moveToFirst()) {
            String sp = c.getString(0);
            c.close();
            return sp;
        }
        if (c != null) c.close();
        return "Poultry";
    }

    private static String resolveEnterpriseCode(String species) {
        if ("Goat".equalsIgnoreCase(species) || "Sheep".equalsIgnoreCase(species)) return "GT2";
        if ("Cattle".equalsIgnoreCase(species)) return "CW1";
        if ("Crop".equalsIgnoreCase(species) || "Coffee".equalsIgnoreCase(species)) return "CF3";
        return "GEN";
    }
}
