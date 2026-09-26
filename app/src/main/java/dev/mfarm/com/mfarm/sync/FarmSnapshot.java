package dev.mfarm.com.mfarm.sync;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class FarmSnapshot {
    private static final String TAG = "FarmSnapshot";
    private static final String[] LINK_COLUMNS = {"animal_id", "item_id"};

    private FarmSnapshot() {}

    public static JSONObject capture(SQLiteDatabase db, FarmIdentity identity) throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", 1);
        root.put("farmId", identity.farmId);
        root.put("farmName", identity.farmName);
        root.put("exportedAt", System.currentTimeMillis());
        root.put("deviceId", identity.deviceId);
        JSONArray records = new JSONArray();
        for (int t = 0; t < FarmSyncSchema.TABLES.length; t++) {
            exportTable(db, FarmSyncSchema.TABLES[t], records);
        }
        exportDeleted(db, records);
        root.put("records", records);
        return root;
    }

    public static int apply(SQLiteDatabase db, JSONObject snapshot) throws Exception {
        if (snapshot == null) {
            return 0;
        }
        JSONArray records = snapshot.optJSONArray("records");
        if (records == null) {
            return 0;
        }
        int applied = 0;
        FarmSyncSchema.setApplying(db, true);
        db.beginTransaction();
        try {
            List<JSONObject> ordered = orderForApply(records);
            for (int i = 0; i < ordered.size(); i++) {
                if (applyRecord(db, ordered.get(i))) {
                    applied++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            FarmSyncSchema.setApplying(db, false);
        }
        return applied;
    }

    private static List<JSONObject> orderForApply(JSONArray records) throws Exception {
        List<JSONObject> first = new ArrayList<>();
        List<JSONObject> rest = new ArrayList<>();
        for (int i = 0; i < records.length(); i++) {
            JSONObject rec = records.getJSONObject(i);
            String table = rec.optString("table");
            if ("animas".equals(table) || "inventory".equals(table)) {
                first.add(rec);
            } else {
                rest.add(rec);
            }
        }
        first.addAll(rest);
        return first;
    }

    private static void exportTable(SQLiteDatabase db, String table, JSONArray records) {
        if (!FarmSyncSchema.tableExists(db, table)) {
            return;
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT t.*, s.uuid, s.updated_at, s.deleted FROM " + table
                    + " t JOIN sync_index s ON s.table_name=? AND s.local_id=t.id AND s.deleted=0",
                    new String[]{table});
            while (cursor.moveToNext()) {
                records.put(cursorToRecord(db, table, cursor, false));
            }
        } catch (Exception e) {
            Log.w(TAG, "Export failed for " + table, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static void exportDeleted(SQLiteDatabase db, JSONArray records) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT uuid, table_name, updated_at FROM sync_index WHERE deleted=1",
                    null);
            while (cursor.moveToNext()) {
                JSONObject rec = new JSONObject();
                rec.put("uuid", cursor.getString(0));
                rec.put("table", cursor.getString(1));
                rec.put("updatedAt", cursor.getLong(2));
                rec.put("deleted", true);
                rec.put("fields", new JSONObject());
                rec.put("links", new JSONObject());
                records.put(rec);
            }
        } catch (Exception e) {
            Log.w(TAG, "Export deleted failed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static JSONObject cursorToRecord(SQLiteDatabase db, String table, Cursor cursor, boolean deleted)
            throws Exception {
        JSONObject rec = new JSONObject();
        rec.put("uuid", cursor.getString(cursor.getColumnIndex("uuid")));
        rec.put("table", table);
        rec.put("updatedAt", cursor.getLong(cursor.getColumnIndex("updated_at")));
        rec.put("deleted", deleted);
        JSONObject fields = new JSONObject();
        JSONObject links = new JSONObject();
        String[] names = cursor.getColumnNames();
        for (int i = 0; i < names.length; i++) {
            String col = names[i];
            if ("id".equals(col) || "uuid".equals(col) || "updated_at".equals(col)
                    || "deleted".equals(col) || "table_name".equals(col)) {
                continue;
            }
            if (isLinkColumn(col)) {
                int idx = cursor.getColumnIndex(col);
                if (idx >= 0 && !cursor.isNull(idx)) {
                    String targetTable = "item_id".equals(col) ? "inventory" : "animas";
                    String uuid = uuidForLocalId(db, targetTable, cursor.getInt(idx));
                    if (uuid != null) {
                        links.put(col, uuid);
                    }
                }
                continue;
            }
            int idx = cursor.getColumnIndex(col);
            if (idx >= 0) {
                putCursorValue(fields, col, cursor, idx);
            }
        }
        rec.put("fields", fields);
        rec.put("links", links);
        return rec;
    }

    private static boolean applyRecord(SQLiteDatabase db, JSONObject rec) {
        try {
            String uuid = rec.getString("uuid");
            String table = rec.getString("table");
            long updatedAt = rec.optLong("updatedAt", System.currentTimeMillis());
            boolean deleted = rec.optBoolean("deleted", false);
            if (!FarmSyncSchema.tableExists(db, table)) {
                return false;
            }
            Integer localId = localIdForUuid(db, uuid);
            long localUpdated = localUpdatedAt(db, uuid);
            if (localId != null && updatedAt < localUpdated) {
                return false;
            }
            if (deleted) {
                if (localId != null) {
                    db.delete(table, "id=?", new String[]{String.valueOf(localId)});
                    ContentValues index = new ContentValues();
                    index.put("deleted", 1);
                    index.put("updated_at", updatedAt);
                    db.update("sync_index", index, "uuid=?", new String[]{uuid});
                } else {
                    ContentValues index = new ContentValues();
                    index.put("uuid", uuid);
                    index.put("table_name", table);
                    index.put("local_id", -1);
                    index.put("updated_at", updatedAt);
                    index.put("deleted", 1);
                    db.insertWithOnConflict("sync_index", null, index, SQLiteDatabase.CONFLICT_REPLACE);
                }
                return true;
            }
            ContentValues values = fieldsToValues(db, table, rec.optJSONObject("fields"), rec.optJSONObject("links"));
            if (localId == null) {
                long newId = db.insert(table, null, values);
                if (newId == -1) {
                    return false;
                }
                ContentValues index = new ContentValues();
                index.put("uuid", uuid);
                index.put("table_name", table);
                index.put("local_id", newId);
                index.put("updated_at", updatedAt);
                index.put("deleted", 0);
                db.insertWithOnConflict("sync_index", null, index, SQLiteDatabase.CONFLICT_REPLACE);
            } else {
                db.update(table, values, "id=?", new String[]{String.valueOf(localId)});
                ContentValues index = new ContentValues();
                index.put("updated_at", updatedAt);
                index.put("deleted", 0);
                db.update("sync_index", index, "uuid=?", new String[]{uuid});
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Skip record", e);
            return false;
        }
    }

    private static ContentValues fieldsToValues(SQLiteDatabase db, String table, JSONObject fields, JSONObject links)
            throws Exception {
        ContentValues values = new ContentValues();
        java.util.HashSet<String> columns = tableColumns(db, table);
        if (fields != null) {
            JSONArray names = fields.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String key = names.getString(i);
                    if (!columns.contains(key)) {
                        continue;
                    }
                    if (fields.isNull(key)) {
                        values.putNull(key);
                    } else {
                        Object v = fields.get(key);
                        if (v instanceof Integer) {
                            values.put(key, (Integer) v);
                        } else if (v instanceof Long) {
                            values.put(key, (Long) v);
                        } else if (v instanceof Double) {
                            values.put(key, (Double) v);
                        } else if (v instanceof Boolean) {
                            values.put(key, (Boolean) v ? 1 : 0);
                        } else {
                            values.put(key, String.valueOf(v));
                        }
                    }
                }
            }
        }
        if (links != null) {
            JSONArray names = links.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String col = names.getString(i);
                    if (!columns.contains(col)) {
                        continue;
                    }
                    String uuid = links.optString(col, "");
                    String target = "item_id".equals(col) ? "inventory" : "animas";
                    Integer id = localIdForUuid(db, uuid);
                    if (id == null) {
                        id = localIdForUuidAndTable(db, uuid, target);
                    }
                    if (id != null) {
                        values.put(col, id);
                    }
                }
            }
        }
        return values;
    }

    private static java.util.HashSet<String> tableColumns(SQLiteDatabase db, String table) {
        java.util.HashSet<String> cols = new java.util.HashSet<>();
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            while (c.moveToNext()) {
                cols.add(c.getString(1));
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return cols;
    }

    private static void putCursorValue(JSONObject fields, String col, Cursor cursor, int i) throws Exception {
        int type = cursor.getType(i);
        if (type == Cursor.FIELD_TYPE_NULL) {
            fields.put(col, JSONObject.NULL);
        } else if (type == Cursor.FIELD_TYPE_INTEGER) {
            fields.put(col, cursor.getLong(i));
        } else if (type == Cursor.FIELD_TYPE_FLOAT) {
            fields.put(col, cursor.getDouble(i));
        } else {
            fields.put(col, cursor.getString(i));
        }
    }

    private static boolean isLinkColumn(String col) {
        for (int i = 0; i < LINK_COLUMNS.length; i++) {
            if (LINK_COLUMNS[i].equals(col)) {
                return true;
            }
        }
        return false;
    }

    private static String uuidForLocalId(SQLiteDatabase db, String table, int localId) {
        Cursor c = db.rawQuery(
                "SELECT uuid FROM sync_index WHERE table_name=? AND local_id=? AND deleted=0",
                new String[]{table, String.valueOf(localId)});
        try {
            if (c.moveToFirst()) {
                return c.getString(0);
            }
            return null;
        } finally {
            c.close();
        }
    }

    private static Integer localIdForUuid(SQLiteDatabase db, String uuid) {
        Cursor c = db.rawQuery("SELECT local_id FROM sync_index WHERE uuid=? AND deleted=0",
                new String[]{uuid});
        try {
            if (c.moveToFirst()) {
                int id = c.getInt(0);
                return id > 0 ? id : null;
            }
            return null;
        } finally {
            c.close();
        }
    }

    private static Integer localIdForUuidAndTable(SQLiteDatabase db, String uuid, String table) {
        Cursor c = db.rawQuery(
                "SELECT local_id FROM sync_index WHERE uuid=? AND table_name=? AND deleted=0",
                new String[]{uuid, table});
        try {
            if (c.moveToFirst()) {
                int id = c.getInt(0);
                return id > 0 ? id : null;
            }
            return null;
        } finally {
            c.close();
        }
    }

    private static long localUpdatedAt(SQLiteDatabase db, String uuid) {
        Cursor c = db.rawQuery("SELECT updated_at FROM sync_index WHERE uuid=?", new String[]{uuid});
        try {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
            return 0L;
        } finally {
            c.close();
        }
    }
}
