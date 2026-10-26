package dev.mfarm.com.mfarm.dao;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CategoryManager {

    public static final String OTHER_SENTINEL = "Other (Specify)...";

    private CategoryManager() {}

    public static List<String> getLocations(DatabaseAdapter db) {
        List<String> list = new ArrayList<>();
        if (db != null) {
            Cursor c = db.query("SELECT location_name FROM farm_locations WHERE is_default = 1 OR is_default = 0 ORDER BY location_name ASC", null);
            if (c != null && c.moveToFirst()) {
                do {
                    String name = c.getString(0);
                    if (name != null && !name.trim().isEmpty() && !list.contains(name.trim())) {
                        list.add(name.trim());
                    }
                } while (c.moveToNext());
            }
            if (c != null) c.close();
        }

        if (list.isEmpty()) {
            list.add("Main Farm");
            list.add("Kicwamba");
            list.add("Nkesiga");
        }
        list.add("Other (Specify)...");
        return list;
    }

    public static boolean addLocation(DatabaseAdapter db, String locationName, String description) {
        if (locationName == null || locationName.trim().isEmpty() || "Other (Specify)...".equalsIgnoreCase(locationName.trim())) {
            return false;
        }
        String clean = locationName.trim();

        if (db != null) {
            Cursor c = db.query("SELECT id FROM farm_locations WHERE LOWER(location_name) = LOWER(?)", new String[]{clean});
            if (c != null && c.moveToFirst()) {
                c.close();
                return false; // Case-insensitive duplicate
            }
            if (c != null) c.close();

            Map<String, Object> val = new HashMap<>();
            val.put("location_name", clean);
            val.put("description", description != null ? description.trim() : "Custom Location");
            val.put("is_default", 0);
            return db.insert("farm_locations", null, val) != -1;
        }
        return false;
    }

    public static List<String> getCategories(DatabaseAdapter db, String table, String colName, String[] defaultArr) {
        List<String> list = new ArrayList<>();
        if (db != null) {
            try {
                Cursor c = db.query("SELECT " + colName + " FROM " + table + " ORDER BY " + colName + " ASC", null);
                if (c != null && c.moveToFirst()) {
                    do {
                        String name = c.getString(0);
                        if (name != null && !name.trim().isEmpty() && !list.contains(name.trim())) {
                            list.add(name.trim());
                        }
                    } while (c.moveToNext());
                }
                if (c != null) c.close();
            } catch (Exception ignored) {}
        }

        if (list.isEmpty() && defaultArr != null) {
            for (String d : defaultArr) {
                if (!list.contains(d)) list.add(d);
            }
        }
        list.add("Other (Specify)...");
        return list;
    }

    public static boolean addCategory(DatabaseAdapter db, String table, String colName, String customValue) {
        if (customValue == null || customValue.trim().isEmpty() || "Other (Specify)...".equalsIgnoreCase(customValue.trim())) {
            return false;
        }
        String clean = customValue.trim();

        if (db != null) {
            try {
                Cursor c = db.query("SELECT id FROM " + table + " WHERE LOWER(" + colName + ") = LOWER(?)", new String[]{clean});
                if (c != null && c.moveToFirst()) {
                    c.close();
                    return false; // Case-insensitive duplicate
                }
                if (c != null) c.close();

                Map<String, Object> val = new HashMap<>();
                val.put(colName, clean);
                return db.insert(table, null, val) != -1;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
