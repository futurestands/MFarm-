package dev.mfarm.com.mfarm.dao;

import android.database.Cursor;
import android.database.MatrixCursor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JdbcDatabaseAdapter implements DatabaseAdapter {
    private final Connection conn;
    private boolean transactionSuccessful = false;

    public JdbcDatabaseAdapter(Connection conn) {
        this.conn = conn;
    }

    public Connection getConnection() {
        return conn;
    }

    @Override
    public Cursor query(String sql, String[] selectionArgs) {
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            if (selectionArgs != null) {
                for (int i = 0; i < selectionArgs.length; i++) {
                    String arg = selectionArgs[i];
                    if (arg != null && arg.matches("^-?\\d+$")) {
                        try {
                            ps.setLong(i + 1, Long.parseLong(arg));
                        } catch (NumberFormatException e) {
                            ps.setString(i + 1, arg);
                        }
                    } else {
                        ps.setString(i + 1, arg);
                    }
                }
            }
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            String[] colNames = new String[colCount];
            for (int i = 0; i < colCount; i++) {
                colNames[i] = meta.getColumnName(i + 1);
            }
            MatrixCursor cursor = new MatrixCursor(colNames);
            while (rs.next()) {
                Object[] row = new Object[colCount];
                for (int i = 0; i < colCount; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                cursor.addRow(row);
            }
            rs.close();
            ps.close();
            return cursor;
        } catch (Exception e) {
            throw new RuntimeException("JDBC query error: " + e.getMessage(), e);
        }
    }

    @Override
    public long insert(String table, String nullColumnHack, Map<String, Object> values) {
        try {
            if (values == null || values.isEmpty()) {
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("INSERT INTO " + table + " DEFAULT VALUES");
                ResultSet rsKeys = stmt.getGeneratedKeys();
                long lastId = rsKeys.next() ? rsKeys.getLong(1) : -1;
                rsKeys.close();
                stmt.close();
                return lastId;
            }
            StringBuilder cols = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            List<Object> valList = new ArrayList<>();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (cols.length() > 0) {
                    cols.append(", ");
                    placeholders.append(", ");
                }
                cols.append(entry.getKey());
                placeholders.append("?");
                valList.add(entry.getValue());
            }
            String sql = "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")";
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < valList.size(); i++) {
                ps.setObject(i + 1, valList.get(i));
            }
            ps.executeUpdate();
            ps.close();

            Statement stmtId = conn.createStatement();
            ResultSet rsId = stmtId.executeQuery("SELECT last_insert_rowid()");
            long lastId = rsId.next() ? rsId.getLong(1) : -1;
            rsId.close();
            stmtId.close();
            return lastId;
        } catch (Exception e) {
            throw new RuntimeException("JDBC insert error: " + e.getMessage(), e);
        }
    }

    @Override
    public int update(String table, Map<String, Object> values, String whereClause, String[] whereArgs) {
        try {
            StringBuilder setClause = new StringBuilder();
            List<Object> valList = new ArrayList<>();
            if (values != null) {
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    if (setClause.length() > 0) {
                        setClause.append(", ");
                    }
                    setClause.append(entry.getKey()).append(" = ?");
                    valList.add(entry.getValue());
                }
            }
            String sql = "UPDATE " + table + " SET " + setClause;
            if (whereClause != null && !whereClause.trim().isEmpty()) {
                sql += " WHERE " + whereClause;
            }
            PreparedStatement ps = conn.prepareStatement(sql);
            int idx = 1;
            for (Object v : valList) {
                ps.setObject(idx++, v);
            }
            if (whereArgs != null) {
                for (String arg : whereArgs) {
                    if (arg != null && arg.matches("^-?\\d+$")) {
                        try {
                            ps.setLong(idx++, Long.parseLong(arg));
                        } catch (NumberFormatException e) {
                            ps.setString(idx++, arg);
                        }
                    } else {
                        ps.setString(idx++, arg);
                    }
                }
            }
            int updated = ps.executeUpdate();
            ps.close();
            return updated;
        } catch (Exception e) {
            throw new RuntimeException("JDBC update error: " + e.getMessage(), e);
        }
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        try {
            String sql = "DELETE FROM " + table;
            if (whereClause != null && !whereClause.trim().isEmpty()) {
                sql += " WHERE " + whereClause;
            }
            PreparedStatement ps = conn.prepareStatement(sql);
            if (whereArgs != null) {
                for (int i = 0; i < whereArgs.length; i++) {
                    String arg = whereArgs[i];
                    if (arg != null && arg.matches("^-?\\d+$")) {
                        try {
                            ps.setLong(i + 1, Long.parseLong(arg));
                        } catch (NumberFormatException e) {
                            ps.setString(i + 1, arg);
                        }
                    } else {
                        ps.setString(i + 1, arg);
                    }
                }
            }
            int deleted = ps.executeUpdate();
            ps.close();
            return deleted;
        } catch (Exception e) {
            throw new RuntimeException("JDBC delete error: " + e.getMessage(), e);
        }
    }

    @Override
    public void beginTransaction() {
        try {
            transactionSuccessful = false;
            conn.setAutoCommit(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setTransactionSuccessful() {
        transactionSuccessful = true;
    }

    @Override
    public void endTransaction() {
        try {
            if (transactionSuccessful) {
                conn.commit();
            } else {
                conn.rollback();
            }
            conn.setAutoCommit(true);
        } catch (Exception e) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (Exception ignored) {}
            throw new RuntimeException(e);
        }
    }
}
