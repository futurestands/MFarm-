package dev.mfarm.com.mfarm.dao;

import android.database.Cursor;
import java.util.Map;

public interface DatabaseAdapter {
    Cursor query(String sql, String[] selectionArgs);
    long insert(String table, String nullColumnHack, Map<String, Object> values);
    int update(String table, Map<String, Object> values, String whereClause, String[] whereArgs);
    int delete(String table, String whereClause, String[] whereArgs);
    void beginTransaction();
    void setTransactionSuccessful();
    void endTransaction();
}
