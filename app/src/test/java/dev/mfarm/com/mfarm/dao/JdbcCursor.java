package dev.mfarm.com.mfarm.dao;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcCursor implements Cursor {

    private final List<String> colNames = new ArrayList<>();
    private final Map<String, Integer> colMap = new HashMap<>();
    private final List<Object[]> rows = new ArrayList<>();
    private int pos = -1;
    private boolean closed = false;

    public JdbcCursor(ResultSet rs) {
        try {
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            for (int i = 1; i <= count; i++) {
                String name = meta.getColumnName(i);
                colNames.add(name);
                colMap.put(name.toLowerCase(), i - 1);
            }
            while (rs.next()) {
                Object[] row = new Object[count];
                for (int i = 1; i <= count; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                rows.add(row);
            }
            rs.close();
        } catch (Exception e) {
            throw new RuntimeException("Error populating JdbcCursor: " + e.getMessage(), e);
        }
    }

    @Override public int getCount() { return rows.size(); }
    @Override public int getPosition() { return pos; }

    @Override public boolean move(int offset) { return moveToPosition(pos + offset); }

    @Override
    public boolean moveToPosition(int position) {
        if (position < 0) {
            pos = -1;
            return false;
        }
        if (position >= rows.size()) {
            pos = rows.size();
            return false;
        }
        pos = position;
        return true;
    }

    @Override public boolean moveToFirst() { return moveToPosition(0); }
    @Override public boolean moveToLast() { return moveToPosition(rows.size() - 1); }
    @Override public boolean moveToNext() { return moveToPosition(pos + 1); }
    @Override public boolean moveToPrevious() { return moveToPosition(pos - 1); }
    @Override public boolean isFirst() { return pos == 0 && rows.size() > 0; }
    @Override public boolean isLast() { return pos == rows.size() - 1 && rows.size() > 0; }
    @Override public boolean isBeforeFirst() { return pos == -1; }
    @Override public boolean isAfterLast() { return pos >= rows.size(); }

    @Override
    public int getColumnIndex(String columnName) {
        if (columnName == null) return -1;
        Integer idx = colMap.get(columnName.toLowerCase());
        return idx != null ? idx : -1;
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        int idx = getColumnIndex(columnName);
        if (idx < 0) throw new IllegalArgumentException("Column not found: " + columnName);
        return idx;
    }

    @Override public String getColumnName(int columnIndex) { return colNames.get(columnIndex); }
    @Override public String[] getColumnNames() { return colNames.toArray(new String[0]); }
    @Override public int getColumnCount() { return colNames.size(); }

    private Object getVal(int columnIndex) {
        if (pos < 0 || pos >= rows.size()) return null;
        return rows.get(pos)[columnIndex];
    }

    @Override public String getString(int columnIndex) { Object v = getVal(columnIndex); return v != null ? String.valueOf(v) : null; }
    @Override public short getShort(int columnIndex) { Object v = getVal(columnIndex); return v != null ? ((Number) v).shortValue() : 0; }
    @Override public int getInt(int columnIndex) { Object v = getVal(columnIndex); return v != null ? ((Number) v).intValue() : 0; }
    @Override public long getLong(int columnIndex) { Object v = getVal(columnIndex); return v != null ? ((Number) v).longValue() : 0L; }
    @Override public float getFloat(int columnIndex) { Object v = getVal(columnIndex); return v != null ? ((Number) v).floatValue() : 0.0f; }
    @Override public double getDouble(int columnIndex) { Object v = getVal(columnIndex); return v != null ? ((Number) v).doubleValue() : 0.0; }

    @Override
    public boolean isNull(int columnIndex) {
        return getVal(columnIndex) == null;
    }

    @Override public void close() { closed = true; }
    @Override public boolean isClosed() { return closed; }

    @Override public byte[] getBlob(int columnIndex) { Object v = getVal(columnIndex); return v instanceof byte[] ? (byte[]) v : null; }
    @Override public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {}
    @Override public int getType(int columnIndex) { return 0; }
    @Override public void registerContentObserver(ContentObserver observer) {}
    @Override public void unregisterContentObserver(ContentObserver observer) {}
    @Override public void registerDataSetObserver(DataSetObserver observer) {}
    @Override public void unregisterDataSetObserver(DataSetObserver observer) {}
    @Override public void setNotificationUri(ContentResolver cr, Uri uri) {}
    @Override public Uri getNotificationUri() { return null; }
    @Override public boolean getWantsAllOnMoveCalls() { return false; }
    @Override public void setExtras(Bundle extras) {}
    @Override public Bundle getExtras() { return Bundle.EMPTY; }
    @Override public Bundle respond(Bundle extras) { return Bundle.EMPTY; }
    @Override public void deactivate() {}
    @Override public boolean requery() { return false; }
}
