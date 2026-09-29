package dev.mfarm.com.mfarm.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Map;

public class AndroidDatabaseAdapter implements DatabaseAdapter {
    private final SQLiteDatabase db;

    public AndroidDatabaseAdapter(SQLiteDatabase db) {
        this.db = db;
    }

    public SQLiteDatabase getSQLiteDatabase() {
        return db;
    }

    private ContentValues mapToContentValues(Map<String, Object> values) {
        if (values == null) return null;
        ContentValues cv = new ContentValues();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object v = entry.getValue();
            if (v == null) {
                cv.putNull(key);
            } else if (v instanceof Integer) {
                cv.put(key, (Integer) v);
            } else if (v instanceof Long) {
                cv.put(key, (Long) v);
            } else if (v instanceof Double) {
                cv.put(key, (Double) v);
            } else if (v instanceof Float) {
                cv.put(key, (Float) v);
            } else if (v instanceof Boolean) {
                cv.put(key, (Boolean) v ? 1 : 0);
            } else if (v instanceof byte[]) {
                cv.put(key, (byte[]) v);
            } else {
                cv.put(key, String.valueOf(v));
            }
        }
        return cv;
    }

    @Override
    public Cursor query(String sql, String[] selectionArgs) {
        return db.rawQuery(sql, selectionArgs);
    }

    @Override
    public long insert(String table, String nullColumnHack, Map<String, Object> values) {
        return db.insert(table, nullColumnHack, mapToContentValues(values));
    }

    @Override
    public int update(String table, Map<String, Object> values, String whereClause, String[] whereArgs) {
        return db.update(table, mapToContentValues(values), whereClause, whereArgs);
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        return db.delete(table, whereClause, whereArgs);
    }

    @Override
    public void beginTransaction() {
        db.beginTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        db.setTransactionSuccessful();
    }

    @Override
    public void endTransaction() {
        db.endTransaction();
    }
}
