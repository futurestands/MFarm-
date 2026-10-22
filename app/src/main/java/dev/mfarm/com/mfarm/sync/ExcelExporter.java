package dev.mfarm.com.mfarm.sync;

import android.database.Cursor;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import dev.mfarm.com.mfarm.dao.DatabaseAdapter;

public final class ExcelExporter {

    private static final String TAG = "ExcelExporter";

    private ExcelExporter() {}

    public static File exportModuleToCsv(DatabaseAdapter db, File exportDir, String moduleName, String projectCodeFilter) {
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String safeFileName = sanitizeFileName("MFarm_Export_" + moduleName + "_" + System.currentTimeMillis() + ".csv");
        File file = new File(exportDir, safeFileName);

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            if ("Animals".equalsIgnoreCase(moduleName)) {
                exportAnimals(db, writer);
            } else if ("Batches".equalsIgnoreCase(moduleName)) {
                exportBatches(db, writer);
            } else if ("Disposals".equalsIgnoreCase(moduleName)) {
                exportDisposals(db, writer);
            } else if ("Crops".equalsIgnoreCase(moduleName)) {
                exportCrops(db, writer);
            } else if ("Finances".equalsIgnoreCase(moduleName)) {
                exportFinances(db, writer, projectCodeFilter);
            } else {
                exportAllSummary(db, writer);
            }

            writer.flush();
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting module " + moduleName, e);
            return null;
        }
    }

    private static void exportAnimals(DatabaseAdapter db, OutputStreamWriter writer) throws Exception {
        writer.write("ID,Tag_Number,Species,Gender,DOB,Origin_Source,Location,Status,Notes\n");
        Cursor c = db.query("SELECT id, name, species, gender, dob, origin_source, location_name, lifecycle_status, notes FROM animas ORDER BY id ASC", null);
        if (c != null && c.moveToFirst()) {
            do {
                writer.write(encodeCsv(c.getString(0)) + ","
                        + encodeCsv(c.getString(1)) + ","
                        + encodeCsv(c.getString(2)) + ","
                        + encodeCsv(c.getString(3)) + ","
                        + encodeCsv(c.getString(4)) + ","
                        + encodeCsv(c.getString(5)) + ","
                        + encodeCsv(c.getString(6)) + ","
                        + encodeCsv(c.getString(7)) + ","
                        + encodeCsv(c.getString(8)) + "\n");
            } while (c.moveToNext());
            c.close();
        }
    }

    private static void exportBatches(DatabaseAdapter db, OutputStreamWriter writer) throws Exception {
        writer.write("Batch_ID,Species,Subtype,Breed_Strain,Current_Qty,Location,Status,Project_Code\n");
        Cursor c = db.query("SELECT batch_id, species, subtype, breed_or_strain, current_quantity, location_name, status, project_code FROM livestock_batches ORDER BY id ASC", null);
        if (c != null && c.moveToFirst()) {
            do {
                writer.write(encodeCsv(c.getString(0)) + ","
                        + encodeCsv(c.getString(1)) + ","
                        + encodeCsv(c.getString(2)) + ","
                        + encodeCsv(c.getString(3)) + ","
                        + c.getInt(4) + ","
                        + encodeCsv(c.getString(5)) + ","
                        + encodeCsv(c.getString(6)) + ","
                        + encodeCsv(c.getString(7)) + "\n");
            } while (c.moveToNext());
            c.close();
        }
    }

    private static void exportDisposals(DatabaseAdapter db, OutputStreamWriter writer) throws Exception {
        writer.write("Animal_Tag,Date,Method,Cause_Reason,Price,Buyer\n");
        Cursor c = db.query("SELECT animal_tag, disposal_date, method, cause_or_reason, price, buyer_or_destination FROM disposal_records ORDER BY id ASC", null);
        if (c != null && c.moveToFirst()) {
            do {
                writer.write(encodeCsv(c.getString(0)) + ","
                        + encodeCsv(c.getString(1)) + ","
                        + encodeCsv(c.getString(2)) + ","
                        + encodeCsv(c.getString(3)) + ","
                        + c.getDouble(4) + ","
                        + encodeCsv(c.getString(5)) + "\n");
            } while (c.moveToNext());
            c.close();
        }
    }

    private static void exportCrops(DatabaseAdapter db, OutputStreamWriter writer) throws Exception {
        writer.write("Activity_Type,Date,Inputs_Used,Cost,Harvest_Yield,Notes\n");
        Cursor c = db.query("SELECT activity_type, activity_date, inputs_used, cost, yield_harvested, notes FROM crop_activities ORDER BY id ASC", null);
        if (c != null && c.moveToFirst()) {
            do {
                writer.write(encodeCsv(c.getString(0)) + ","
                        + encodeCsv(c.getString(1)) + ","
                        + encodeCsv(c.getString(2)) + ","
                        + c.getDouble(3) + ","
                        + c.getDouble(4) + ","
                        + encodeCsv(c.getString(5)) + "\n");
            } while (c.moveToNext());
            c.close();
        }
    }

    private static void exportFinances(DatabaseAdapter db, OutputStreamWriter writer, String pCode) throws Exception {
        writer.write("Type,Date,Project_Code,Category,Description,Amount,Notes\n");

        String sqlInc = "SELECT 'Income', date, project_code, category, description, amount, notes FROM income";
        String sqlExp = "SELECT 'Expense', date, project_code, category, description, amount, notes FROM expenses";

        if (pCode != null && !pCode.trim().isEmpty() && !"ALL".equalsIgnoreCase(pCode.trim())) {
            sqlInc += " WHERE project_code = '" + pCode.trim() + "'";
            sqlExp += " WHERE project_code = '" + pCode.trim() + "'";
        }

        Cursor cInc = db.query(sqlInc, null);
        if (cInc != null && cInc.moveToFirst()) {
            do {
                writer.write(encodeCsv(cInc.getString(0)) + ","
                        + encodeCsv(cInc.getString(1)) + ","
                        + encodeCsv(cInc.getString(2)) + ","
                        + encodeCsv(cInc.getString(3)) + ","
                        + encodeCsv(cInc.getString(4)) + ","
                        + cInc.getDouble(5) + ","
                        + encodeCsv(cInc.getString(6)) + "\n");
            } while (cInc.moveToNext());
            cInc.close();
        }

        Cursor cExp = db.query(sqlExp, null);
        if (cExp != null && cExp.moveToFirst()) {
            do {
                writer.write(encodeCsv(cExp.getString(0)) + ","
                        + encodeCsv(cExp.getString(1)) + ","
                        + encodeCsv(cExp.getString(2)) + ","
                        + encodeCsv(cExp.getString(3)) + ","
                        + encodeCsv(cExp.getString(4)) + ","
                        + cExp.getDouble(5) + ","
                        + encodeCsv(cExp.getString(6)) + "\n");
            } while (cExp.moveToNext());
            cExp.close();
        }
    }

    private static void exportAllSummary(DatabaseAdapter db, OutputStreamWriter writer) throws Exception {
        writer.write("Module,Record_Count\n");
        writer.write("Animals," + getCount(db, "animas") + "\n");
        writer.write("Batches," + getCount(db, "livestock_batches") + "\n");
        writer.write("Disposals," + getCount(db, "disposal_records") + "\n");
        writer.write("Crop Activities," + getCount(db, "crop_activities") + "\n");
        writer.write("Income Transactions," + getCount(db, "income") + "\n");
        writer.write("Expense Transactions," + getCount(db, "expenses") + "\n");
    }

    private static int getCount(DatabaseAdapter db, String table) {
        Cursor c = db.query("SELECT COUNT(*) FROM " + table, null);
        if (c != null && c.moveToFirst()) {
            int cnt = c.getInt(0);
            c.close();
            return cnt;
        }
        if (c != null) c.close();
        return 0;
    }

    /**
     * Prevents CSV / Spreadsheet Formula Injection by prepending ' to string values starting with =, +, -, @
     */
    public static String encodeCsv(String val) {
        if (val == null) return "\"\"";
        String clean = val.trim();
        if (clean.startsWith("=") || clean.startsWith("+") || clean.startsWith("-") || clean.startsWith("@")) {
            clean = "'" + clean;
        }
        return "\"" + clean.replace("\"", "\"\"") + "\"";
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
