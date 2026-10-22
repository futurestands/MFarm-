package dev.mfarm.com.mfarm.dao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FormatUtils {

    private FormatUtils() {}

    public static String formatCurrency(double amount) {
        return String.format(Locale.US, "UGX %,.0f", amount);
    }

    public static String formatQuantity(double qty, String unit) {
        if (unit == null || unit.trim().isEmpty()) {
            return String.format(Locale.US, "%,.1f", qty);
        }
        return String.format(Locale.US, "%,.1f %s", qty, unit.trim());
    }

    public static String formatDate(long timestamp) {
        if (timestamp <= 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US);
        return sdf.format(new Date(timestamp));
    }

    public static String parseSummaryJsonToText(String json) {
        if (json == null || json.trim().isEmpty()) return "Summary not available";
        try {
            org.json.JSONObject obj = new org.json.JSONObject(json);
            StringBuilder sb = new StringBuilder();
            if (obj.has("animals")) sb.append("• Animals: ").append(obj.getInt("animals")).append("\n");
            if (obj.has("breeding")) sb.append("• Breeding: ").append(obj.getInt("breeding")).append("\n");
            if (obj.has("kidding")) sb.append("• Kidding: ").append(obj.getInt("kidding")).append("\n");
            if (obj.has("vaccinations")) sb.append("• Vaccinations: ").append(obj.getInt("vaccinations")).append("\n");
            if (obj.has("disposals")) sb.append("• Disposals: ").append(obj.getInt("disposals")).append("\n");
            if (obj.has("financials")) sb.append("• Financials: ").append(obj.getInt("financials")).append("\n");
            if (obj.has("skipped")) sb.append("• Duplicates Skipped: ").append(obj.getInt("skipped"));
            return sb.toString().trim();
        } catch (Exception e) {
            return json;
        }
    }
}
