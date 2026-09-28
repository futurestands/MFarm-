package dev.mfarm.com.mfarm.intelligence;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FarmIntelligenceEngine {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private static final SimpleDateFormat MONTH_NAME_FORMAT = new SimpleDateFormat("MMMM yyyy", Locale.US);

    static {
        DATE_FORMAT.setLenient(false);
    }

    public static List<Insight> evaluateInsights(SQLiteDatabase db) {
        List<Insight> insights = new ArrayList<>();
        if (db == null || !db.isOpen()) {
            return insights;
        }

        Calendar calToday = Calendar.getInstance();
        Date todayDate = truncateTime(calToday.getTime());

        // Rule I: Check active animals count
        int activeAnimalCount = 0;
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM animas WHERE lifecycle_status = 'Active'", null);
            if (c.moveToFirst()) {
                activeAnimalCount = c.getInt(0);
            }
            c.close();
        } catch (Exception ignored) {}

        if (activeAnimalCount == 0) {
            insights.add(new Insight(
                    Insight.Type.DATA_QUALITY,
                    Insight.Priority.INFO,
                    "Welcome to MFarm",
                    "Register your first farm animal to begin tracking health, milk yields, breeding, and farm finances.",
                    null, null, "REGISTER"
            ));
            return insights;
        }

        // Rule A: Overdue Vaccination
        try {
            Cursor c = db.rawQuery(
                    "SELECT v.id, a.name, v.vaccine_name, v.scheduled_date, a.id " +
                            "FROM vaccinations v JOIN animas a ON v.animal_id = a.id " +
                            "WHERE v.status = 'Overdue'", null);
            while (c.moveToNext()) {
                String animalName = c.getString(1);
                String vaccineName = c.getString(2);
                String schedDate = c.getString(3);
                String animalId = String.valueOf(c.getInt(4));

                insights.add(new Insight(
                        Insight.Type.VACCINATION,
                        Insight.Priority.CRITICAL,
                        "Overdue Vaccination",
                        "Vaccination '" + vaccineName + "' for " + animalName + " was scheduled for " + schedDate + " and is currently overdue.",
                        animalId, animalName, "VACCINATION"
                ));
            }
            c.close();
        } catch (Exception ignored) {}

        // Rule B: Upcoming Vaccination (Next 3 Days)
        try {
            Cursor c = db.rawQuery(
                    "SELECT v.id, a.name, v.vaccine_name, v.scheduled_date, a.id " +
                            "FROM vaccinations v JOIN animas a ON v.animal_id = a.id " +
                            "WHERE v.status = 'Pending'", null);
            while (c.moveToNext()) {
                String animalName = c.getString(1);
                String vaccineName = c.getString(2);
                String schedDateStr = c.getString(3);
                String animalId = String.valueOf(c.getInt(4));

                if (schedDateStr != null && !schedDateStr.trim().isEmpty()) {
                    try {
                        Date schedDate = truncateTime(DATE_FORMAT.parse(schedDateStr.trim()));
                        if (schedDate != null) {
                            long diffDays = daysBetween(todayDate, schedDate);
                            if (diffDays >= 0 && diffDays <= 3) {
                                String dueDesc = diffDays == 0 ? "today" : (diffDays == 1 ? "tomorrow" : "in " + diffDays + " days");
                                insights.add(new Insight(
                                        Insight.Type.VACCINATION,
                                        Insight.Priority.INFO,
                                        "Upcoming Vaccination",
                                        animalName + " is scheduled for " + vaccineName + " vaccination " + dueDesc + " (" + schedDateStr + ").",
                                        animalId, animalName, "VACCINATION"
                                ));
                            }
                        }
                    } catch (ParseException ignored) {}
                }
            }
            c.close();
        } catch (Exception ignored) {}

        // Rule C: Low Stock Warning (Accurate item labeling)
        try {
            Cursor c = db.rawQuery("SELECT item_name, category, quantity, unit, min_quantity FROM inventory WHERE quantity <= min_quantity", null);
            while (c.moveToNext()) {
                String itemName = c.getString(0);
                String category = c.getString(1);
                double qty = c.getDouble(2);
                String unit = c.getString(3);
                double minQty = c.getDouble(4);

                boolean isFeed = category != null && category.toLowerCase().contains("feed");
                String itemLabel = isFeed ? itemName + " feed" : itemName;

                insights.add(new Insight(
                        Insight.Type.FEED,
                        Insight.Priority.ATTENTION,
                        "Low Stock Warning",
                        itemLabel + " stock is low (" + String.format(Locale.US, "%.1f", qty) + " " + (unit != null ? unit : "units") + " remaining; minimum threshold is " + String.format(Locale.US, "%.1f", minQty) + ").",
                        null, null, "INVENTORY"
                ));
            }
            c.close();
        } catch (Exception ignored) {}

        // Rule D & E: Milk Production Trend
        evaluateMilkTrends(db, todayDate, insights);

        // Rule F: Upcoming Calving (Next 14 Days)
        try {
            Cursor c = db.rawQuery(
                    "SELECT b.id, a.name, b.expected_birth_date, a.id " +
                            "FROM breeding_records b JOIN animas a ON b.animal_id = a.id " +
                            "WHERE b.status = 'Pregnant'", null);
            while (c.moveToNext()) {
                String animalName = c.getString(1);
                String expBirthStr = c.getString(2);
                String animalId = String.valueOf(c.getInt(3));

                if (expBirthStr != null && !expBirthStr.trim().isEmpty()) {
                    try {
                        Date expDate = truncateTime(DATE_FORMAT.parse(expBirthStr.trim()));
                        if (expDate != null) {
                            long diffDays = daysBetween(todayDate, expDate);
                            if (diffDays >= 0 && diffDays <= 14) {
                                String dueDesc = diffDays == 0 ? "today" : (diffDays == 1 ? "tomorrow" : "in " + diffDays + " days");
                                insights.add(new Insight(
                                        Insight.Type.CALVING,
                                        Insight.Priority.ATTENTION,
                                        "Upcoming Calving",
                                        animalName + " is expected to calve " + dueDesc + " (" + expBirthStr + "). Prepare calving area.",
                                        animalId, animalName, "BREEDING"
                                ));
                            }
                        }
                    } catch (ParseException ignored) {}
                }
            }
            c.close();
        } catch (Exception ignored) {}

        // Rule G: Recent Illness Follow-Up (Filter ALL qualifying records in last 7 days)
        try {
            Cursor c = db.rawQuery(
                    "SELECT a.id, a.name, i.illness_occured, i.date_occured, i.diagnosis " +
                            "FROM illness i JOIN animas a ON i.animal_id = a.id " +
                            "ORDER BY i.id DESC", null);
            int healthCount = 0;
            while (c.moveToNext()) {
                String animalId = String.valueOf(c.getInt(0));
                String animalName = c.getString(1);
                String illness = c.getString(2);
                String dateStr = c.getString(3);

                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    try {
                        Date illnessDate = truncateTime(DATE_FORMAT.parse(dateStr.trim()));
                        if (illnessDate != null) {
                            long diffDays = daysBetween(illnessDate, todayDate);
                            if (diffDays >= 0 && diffDays <= 7) {
                                insights.add(new Insight(
                                        Insight.Type.HEALTH,
                                        Insight.Priority.ATTENTION,
                                        "Recent Illness Recorded",
                                        animalName + ": Recent illness '" + illness + "' recorded on " + dateStr + " — monitor recovery.",
                                        animalId, animalName, "HEALTH"
                                ));
                                healthCount++;
                                if (healthCount >= 5) break; // Cap display after identifying qualifying records
                            }
                        }
                    } catch (ParseException ignored) {}
                }
            }
            c.close();
        } catch (Exception ignored) {}

        // Rule H: Financial Attention (CURRENT MONTH ONLY)
        evaluateMonthlyFinancials(db, calToday, insights);

        Collections.sort(insights);
        return insights;
    }

    private static void evaluateMonthlyFinancials(SQLiteDatabase db, Calendar calToday, List<Insight> insights) {
        try {
            double monthIncome = 0;
            double monthExpense = 0;
            String currSym = "UGX ";

            Cursor cSym = db.rawQuery("SELECT currency_symbol FROM farm_profile LIMIT 1", null);
            if (cSym.moveToFirst()) {
                String s = cSym.getString(0);
                if (s != null && !s.trim().isEmpty() && !"$".equals(s.trim())) {
                    currSym = s.trim() + (s.trim().endsWith(" ") ? "" : " ");
                }
            }
            cSym.close();

            int targetMonth = calToday.get(Calendar.MONTH);
            int targetYear = calToday.get(Calendar.YEAR);
            String monthName = MONTH_NAME_FORMAT.format(calToday.getTime());

            // Current month income
            Cursor cInc = db.rawQuery("SELECT date, amount FROM income", null);
            while (cInc.moveToNext()) {
                String dStr = cInc.getString(0);
                double amount = cInc.getDouble(1);
                if (isSameMonth(dStr, targetMonth, targetYear)) {
                    monthIncome += amount;
                }
            }
            cInc.close();

            // Current month expenses
            Cursor cExp = db.rawQuery("SELECT date, amount FROM expenses", null);
            while (cExp.moveToNext()) {
                String dStr = cExp.getString(0);
                double amount = cExp.getDouble(1);
                if (isSameMonth(dStr, targetMonth, targetYear)) {
                    monthExpense += amount;
                }
            }
            cExp.close();

            if (monthExpense > monthIncome && monthExpense > 0) {
                insights.add(new Insight(
                        Insight.Type.FINANCIAL,
                        Insight.Priority.ATTENTION,
                        "Monthly Expenses Exceed Income",
                        "Recorded farm expenses (" + currSym + String.format(Locale.US, "%,.0f", monthExpense) + ") exceed sales (" + currSym + String.format(Locale.US, "%,.0f", monthIncome) + ") for " + monthName + ".",
                        null, null, "FINANCIAL"
                ));
            } else if (monthIncome > monthExpense && monthIncome > 0) {
                double net = monthIncome - monthExpense;
                insights.add(new Insight(
                        Insight.Type.FINANCIAL,
                        Insight.Priority.POSITIVE,
                        "Positive Monthly Balance",
                        "Net farm balance for " + monthName + " is +" + currSym + String.format(Locale.US, "%,.0f", net) + ".",
                        null, null, "FINANCIAL"
                ));
            }
        } catch (Exception ignored) {}
    }

    public static boolean isSameMonth(String dateStr, int targetMonth, int targetYear) {
        if (dateStr == null || dateStr.trim().isEmpty()) return false;
        try {
            Date d = DATE_FORMAT.parse(dateStr.trim());
            if (d == null) return false;
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            return cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear;
        } catch (ParseException e) {
            return false;
        }
    }

    private static void evaluateMilkTrends(SQLiteDatabase db, Date todayDate, List<Insight> insights) {
        try {
            double recent7DaysTotal = 0;
            double previous7DaysTotal = 0;
            Set<String> distinctMilkDays = new HashSet<>();

            Cursor c = db.rawQuery("SELECT datetime, litres FROM milk_production", null);
            while (c.moveToNext()) {
                String dateStr = c.getString(0);
                double litres = c.getDouble(1);

                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    try {
                        Date d = truncateTime(DATE_FORMAT.parse(dateStr.trim()));
                        if (d != null) {
                            long diffDays = daysBetween(d, todayDate);
                            if (diffDays >= 0 && diffDays < 14) {
                                distinctMilkDays.add(dateStr.trim());
                                if (diffDays < 7) {
                                    recent7DaysTotal += litres;
                                } else {
                                    previous7DaysTotal += litres;
                                }
                            }
                        }
                    } catch (ParseException ignored) {}
                }
            }
            c.close();

            if (distinctMilkDays.size() < 3) {
                insights.add(new Insight(
                        Insight.Type.DATA_QUALITY,
                        Insight.Priority.INFO,
                        "Milk Trend Data Pending",
                        "Not enough milk records over the last 14 days to calculate a reliable production trend.",
                        null, null, "MILK"
                ));
                return;
            }

            if (previous7DaysTotal > 0 && recent7DaysTotal > 0) {
                double diff = recent7DaysTotal - previous7DaysTotal;
                double pctChange = (diff / previous7DaysTotal) * 100.0;

                if (pctChange <= -10.0) {
                    insights.add(new Insight(
                            Insight.Type.MILK,
                            Insight.Priority.ATTENTION,
                            "Milk Production Decline",
                            "Milk production has decreased " + String.format(Locale.US, "%.1f", Math.abs(pctChange)) + "% over the last 7 days (" + String.format(Locale.US, "%.1f", recent7DaysTotal) + " L vs " + String.format(Locale.US, "%.1f", previous7DaysTotal) + " L in the previous 7-day period).",
                            null, null, "MILK"
                    ));
                } else if (pctChange >= 5.0) {
                    insights.add(new Insight(
                            Insight.Type.MILK,
                            Insight.Priority.POSITIVE,
                            "Milk Production Improvement",
                            "Milk production has increased " + String.format(Locale.US, "%.1f", pctChange) + "% over the last 7 days (" + String.format(Locale.US, "%.1f", recent7DaysTotal) + " L vs " + String.format(Locale.US, "%.1f", previous7DaysTotal) + " L in the previous 7-day period).",
                            null, null, "MILK"
                    ));
                }
            }
        } catch (Exception ignored) {}
    }

    public static FarmBrief generateBrief(SQLiteDatabase db) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String greeting = "Good morning 👋";
        if (hour >= 12 && hour < 17) {
            greeting = "Good afternoon 👋";
        } else if (hour >= 17) {
            greeting = "Good evening 👋";
        }

        List<String> attention = new ArrayList<>();
        List<String> goodNews = new ArrayList<>();
        List<String> comingUp = new ArrayList<>();

        if (db == null || !db.isOpen()) {
            return new FarmBrief(greeting, attention, goodNews, comingUp);
        }

        Date today = truncateTime(cal.getTime());

        // 1. Attention: Currently overdue vaccinations
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM vaccinations WHERE status = 'Overdue'", null);
            if (c.moveToFirst() && c.getInt(0) > 0) {
                int count = c.getInt(0);
                attention.add(count == 1 ? "1 vaccination is currently overdue" : count + " vaccinations are currently overdue");
            }
            c.close();
        } catch (Exception ignored) {}

        // Attention: Low stock items
        try {
            Cursor c = db.rawQuery("SELECT item_name, category FROM inventory WHERE quantity <= min_quantity", null);
            while (c.moveToNext()) {
                String itemName = c.getString(0);
                String category = c.getString(1);
                boolean isFeed = category != null && category.toLowerCase().contains("feed");
                attention.add(isFeed ? itemName + " feed stock is low" : itemName + " stock is low");
            }
            c.close();
        } catch (Exception ignored) {}

        // Attention: Active sick animals
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM animas WHERE health_status = 'Sick' AND lifecycle_status = 'Active'", null);
            if (c.moveToFirst() && c.getInt(0) > 0) {
                int count = c.getInt(0);
                attention.add(count == 1 ? "1 animal is currently flagged as sick" : count + " animals are currently flagged as sick");
            }
            c.close();
        } catch (Exception ignored) {}

        // 2. Good news: Positive current month balance
        try {
            int targetMonth = cal.get(Calendar.MONTH);
            int targetYear = cal.get(Calendar.YEAR);
            double monthInc = 0, monthExp = 0;
            String currSym = "UGX ";

            Cursor cSym = db.rawQuery("SELECT currency_symbol FROM farm_profile LIMIT 1", null);
            if (cSym.moveToFirst()) {
                String s = cSym.getString(0);
                if (s != null && !s.trim().isEmpty() && !"$".equals(s.trim())) {
                    currSym = s.trim() + (s.trim().endsWith(" ") ? "" : " ");
                }
            }
            cSym.close();

            Cursor c1 = db.rawQuery("SELECT date, amount FROM income", null);
            while (c1.moveToNext()) {
                if (isSameMonth(c1.getString(0), targetMonth, targetYear)) {
                    monthInc += c1.getDouble(1);
                }
            }
            c1.close();

            Cursor c2 = db.rawQuery("SELECT date, amount FROM expenses", null);
            while (c2.moveToNext()) {
                if (isSameMonth(c2.getString(0), targetMonth, targetYear)) {
                    monthExp += c2.getDouble(1);
                }
            }
            c2.close();

            if (monthInc > monthExp && monthInc > 0) {
                goodNews.add("Net balance for this month is +" + currSym + String.format(Locale.US, "%,.0f", (monthInc - monthExp)));
            }
        } catch (Exception ignored) {}

        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM animas WHERE lifecycle_status = 'Active'", null);
            if (c.moveToFirst() && c.getInt(0) > 0) {
                goodNews.add(c.getInt(0) == 1 ? "1 active animal registered on farm" : c.getInt(0) + " active animals registered on farm");
            }
            c.close();
        } catch (Exception ignored) {}

        // 3. Coming up: Upcoming vaccinations and upcoming calvings (Future / today dates only)
        try {
            Cursor c = db.rawQuery(
                    "SELECT a.name, v.vaccine_name, v.scheduled_date " +
                            "FROM vaccinations v JOIN animas a ON v.animal_id = a.id " +
                            "WHERE v.status = 'Pending' ORDER BY v.id DESC", null);
            while (c.moveToNext()) {
                String animalName = c.getString(0);
                String vaccine = c.getString(1);
                String schedStr = c.getString(2);

                if (schedStr != null && !schedStr.trim().isEmpty()) {
                    try {
                        Date d = truncateTime(DATE_FORMAT.parse(schedStr.trim()));
                        if (d != null) {
                            long diffDays = daysBetween(today, d);
                            if (diffDays >= 0 && diffDays <= 7) {
                                String dueStr = diffDays == 0 ? "today" : (diffDays == 1 ? "tomorrow" : "in " + diffDays + " days");
                                comingUp.add(animalName + " " + vaccine + " vaccination " + dueStr);
                                if (comingUp.size() >= 2) break;
                            }
                        }
                    } catch (ParseException ignored) {}
                }
            }
            c.close();
        } catch (Exception ignored) {}

        try {
            Cursor c = db.rawQuery(
                    "SELECT a.name, b.expected_birth_date " +
                            "FROM breeding_records b JOIN animas a ON b.animal_id = a.id " +
                            "WHERE b.status = 'Pregnant' ORDER BY b.id DESC", null);
            while (c.moveToNext()) {
                String animalName = c.getString(0);
                String expStr = c.getString(1);

                if (expStr != null && !expStr.trim().isEmpty()) {
                    try {
                        Date d = truncateTime(DATE_FORMAT.parse(expStr.trim()));
                        if (d != null) {
                            long diffDays = daysBetween(today, d);
                            if (diffDays >= 0 && diffDays <= 14) {
                                String dueStr = diffDays == 0 ? "today" : (diffDays == 1 ? "tomorrow" : "in " + diffDays + " days");
                                comingUp.add(animalName + " expected calving " + dueStr);
                                if (comingUp.size() >= 4) break;
                            }
                        }
                    } catch (ParseException ignored) {}
                }
            }
            c.close();
        } catch (Exception ignored) {}

        return new FarmBrief(greeting, attention, goodNews, comingUp);
    }

    public static WhatChanged generateWhatChanged(SQLiteDatabase db) {
        List<String> changes = new ArrayList<>();
        if (db == null || !db.isOpen()) {
            return new WhatChanged(changes);
        }

        String todayStr = DATE_FORMAT.format(new Date());

        // Milk recorded TODAY
        try {
            Cursor c = db.rawQuery("SELECT SUM(litres) FROM milk_production WHERE datetime = ?", new String[]{todayStr});
            if (c.moveToFirst() && c.getDouble(0) > 0) {
                changes.add("+ " + String.format(Locale.US, "%.1f L", c.getDouble(0)) + " milk recorded today");
            }
            c.close();
        } catch (Exception ignored) {}

        // Completed vaccinations scheduled for TODAY
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM vaccinations WHERE status = 'Completed' AND scheduled_date = ?", new String[]{todayStr});
            if (c.moveToFirst() && c.getInt(0) > 0) {
                int count = c.getInt(0);
                changes.add("+ " + count + (count == 1 ? " vaccination scheduled for today is marked completed" : " vaccinations scheduled for today are marked completed"));
            }
            c.close();
        } catch (Exception ignored) {}

        // Currently overdue vaccinations
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM vaccinations WHERE status = 'Overdue'", null);
            if (c.moveToFirst() && c.getInt(0) > 0) {
                int count = c.getInt(0);
                changes.add("⚠ " + count + (count == 1 ? " vaccination is currently overdue" : " vaccinations are currently overdue"));
            }
            c.close();
        } catch (Exception ignored) {}

        // Today's Expenses
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM expenses WHERE date = ?", new String[]{todayStr});
            if (c.moveToFirst() && c.getInt(0) > 0) {
                int count = c.getInt(0);
                changes.add("+ " + count + (count == 1 ? " expense recorded today" : " expenses recorded today"));
            }
            c.close();
        } catch (Exception ignored) {}

        // Today's Income
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM income WHERE date = ?", new String[]{todayStr});
            if (c.moveToFirst() && c.getInt(0) > 0) {
                int count = c.getInt(0);
                changes.add("+ " + count + (count == 1 ? " sale/income recorded today" : " sales/income recorded today"));
            }
            c.close();
        } catch (Exception ignored) {}

        return new WhatChanged(changes);
    }

    public static Date truncateTime(Date date) {
        if (date == null) return null;
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static long daysBetween(Date startDate, Date endDate) {
        Date start = truncateTime(startDate);
        Date end = truncateTime(endDate);
        if (start == null || end == null) return 0;
        return Math.round((double) (end.getTime() - start.getTime()) / (24 * 60 * 60 * 1000L));
    }
}
