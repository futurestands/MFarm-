package dev.mfarm.com.mfarm.sync;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public final class FarmSyncSchema {
    public static final String[] TABLES = {
            "animas",
            "milk_production",
            "expenses",
            "income",
            "vaccinations",
            "illness",
            "inventory",
            "inventory_transactions",
            "breeding_records",
            "vet_checks",
            "calving_records",
            "feed_types",
            "feed_consumption",
            "farm_profile",
            "audit_logs"
    };

    private static final String TAG = "FarmSyncSchema";

    private FarmSyncSchema() {}

    public static void install(SQLiteDatabase db) {
        if (db == null) {
            return;
        }
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_index ("
                + "uuid TEXT PRIMARY KEY NOT NULL, "
                + "table_name TEXT NOT NULL, "
                + "local_id INTEGER NOT NULL, "
                + "updated_at INTEGER NOT NULL, "
                + "deleted INTEGER NOT NULL DEFAULT 0, "
                + "UNIQUE(table_name, local_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_state (k TEXT PRIMARY KEY, v TEXT)");
        db.execSQL("INSERT OR IGNORE INTO sync_state(k, v) VALUES('applying', '0')");

        for (int i = 0; i < TABLES.length; i++) {
            installTriggers(db, TABLES[i]);
            backfill(db, TABLES[i]);
        }
    }

    public static void setApplying(SQLiteDatabase db, boolean applying) {
        db.execSQL("INSERT OR REPLACE INTO sync_state(k, v) VALUES('applying', ?)",
                new String[]{applying ? "1" : "0"});
    }

    private static void installTriggers(SQLiteDatabase db, String table) {
        if (!tableExists(db, table)) {
            return;
        }
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_" + table + "_ai AFTER INSERT ON " + table
                + " WHEN (SELECT v FROM sync_state WHERE k='applying') IS NOT '1' BEGIN "
                + "INSERT OR IGNORE INTO sync_index(uuid, table_name, local_id, updated_at, deleted) "
                + "VALUES (lower(hex(randomblob(16))), '" + table + "', NEW.id, "
                + "CAST(strftime('%s','now') AS INTEGER)*1000, 0); "
                + "END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_" + table + "_au AFTER UPDATE ON " + table
                + " WHEN (SELECT v FROM sync_state WHERE k='applying') IS NOT '1' BEGIN "
                + "INSERT OR IGNORE INTO sync_index(uuid, table_name, local_id, updated_at, deleted) "
                + "VALUES (lower(hex(randomblob(16))), '" + table + "', NEW.id, "
                + "CAST(strftime('%s','now') AS INTEGER)*1000, 0); "
                + "UPDATE sync_index SET updated_at = CAST(strftime('%s','now') AS INTEGER)*1000 "
                + "WHERE table_name='" + table + "' AND local_id=NEW.id AND deleted=0; "
                + "END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_" + table + "_ad AFTER DELETE ON " + table
                + " WHEN (SELECT v FROM sync_state WHERE k='applying') IS NOT '1' BEGIN "
                + "UPDATE sync_index SET deleted=1, updated_at=CAST(strftime('%s','now') AS INTEGER)*1000 "
                + "WHERE table_name='" + table + "' AND local_id=OLD.id; "
                + "END");
    }

    private static void backfill(SQLiteDatabase db, String table) {
        if (!tableExists(db, table)) {
            return;
        }
        try {
            db.execSQL("INSERT OR IGNORE INTO sync_index(uuid, table_name, local_id, updated_at, deleted) "
                    + "SELECT lower(hex(randomblob(16))), '" + table + "', id, "
                    + "CAST(strftime('%s','now') AS INTEGER)*1000, 0 FROM " + table);
        } catch (Exception e) {
            Log.w(TAG, "Backfill skipped for " + table, e);
        }
    }

    static boolean tableExists(SQLiteDatabase db, String table) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{table});
            return c.moveToFirst();
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
}
