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

    public static List<Insight> evaluateInsights(SQLiteDatabase db) {
        List<Insight> insights = new ArrayList<>();
        if (db == null || !db.isOpen()) {
            return insights;
        }

        Calendar calToday = Calendar.getInstance();
        Date todayDate = calToday.getTime();
        String todayStr = DATE_FORMAT.format(todayDate);

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
                        "Vaccination '" + vaccineName + "' for " + animalName + " was scheduled for " + schedDate + " and is overdue.",
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
                        Date schedDate = DATE_FORMAT.parse(schedDateStr.trim());
                        if (schedDate != null) {
                            long diffMillis = schedDate.getTime() - todayDate.getTime();
                            long diffDays = diffMillis / (24 * 60 * 60 * 1000L);
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

        // Rule C: Low Feed Stock
        try {
            Cursor c = db.rawQuery("SELECT item_name, quantity, unit, min_quantity FROM inventory WHERE quantity <= min_quantity", null);
            while (c.moveToNext()) {
                String itemName = c.getString(0);
                double qty = c.getDouble(1);
                String unit = c.getString(2);
                double minQty = c.getDouble(3);

                insights.add(new Insight(
                        Insight.Type.FEED,
                        Insight.Priority.ATTENTION,
                        "Low Stock Warning",
                        itemName + " stock is low (" + String.format(Locale.US, "%.1f", qty) + " " + (unit != null ? unit : "units") + " remaining; minimum alert threshold is " + String.format(Locale.US, "%.1f", minQty) + ").",
                        null, null, "INVENTORY"
                ));
            }
            c.close();
        } catch (Exception ignored) {}

        // Rule D & E: Milk Production Trend (Decline / Improvement)
        evaluateMilkTrends(db, todayDate, insights);

        // Rule F: Upcoming Calving
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
                        Date expDate = DATE_FORMAT.parse(expBirthStr.trim());
                        if (expDate != null) {
                            long diffDays = (expDate.getTime() - todayDate.getTime()) / (24 * 60 * 60 * 1000L);
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

        // Rule G: Recent Illness Follow-Up
        try {
            Cursor c = db.rawQuery(
                    "SELECT a.id, a.name, i.illness_occured, i.date_occured, i.diagnosis " +
                            "FROM illness i JOIN animas a ON i.animal_id = a.id " +
                            "ORDER BY i.id DESC LIMIT 3", null);
            while (c.moveToNext()) {
                String animalId = String.valueOf(c.getInt(0));
                String animalName = c.getString(1);
                String illness = c.getString(2);
                String dateStr = c.getString(3);
                String diag = c.getString(4);

                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    try {
                        Date illnessDate = DATE_FORMAT.parse(dateStr.trim());
                        if (illnessDate != null) {
                            long diffDays = (todayDate.getTime() - illnessDate.getTime()) / (24 * 60 * 60 * 1000L);
                            if (diffDays >= 0 && diffDays <= 7) {
                                String diagText = (diag != null && !diag.trim().isEmpty()) ? " (Diagnosis: " + diag + ")" : "";
                                insights.add(new Insight(
                                        Insight.Type.HEALTH,
                                        Insight.Priority.ATTENTION,
                                        "Health Follow-Up Required",
                                        animalName + " had illness '" + illness + "'" + diagText + " recorded on " + dateStr + ". Monitor recovery closely.",
                                        animalId, animalName, "HEALTH"
                                ));
                            }
                        }
                    } catch (ParseException ignored) {}
                }
            }
            c.close();
        } catch (Exception ignored) {}

        // Rule H: Financial Attention
        try {
            double monthIncome = 0;
            double monthExpense = 0;
            String currSym = "UGX ";

            Cursor cSym = db.rawQuery("SELECT currency_symbol FROM farm_profile LIMIT 1", null);
            if (cSym.moveToFirst()) {
                String s = cSym.getString(0);
                if (s != null && !s.trim().isEmpty() && !"$".equals(s.trim())) {
                    currSym = s.trim() + " ";
                }
            }
            cSym.close();

            Cursor cInc = db.rawQuery("SELECT SUM(amount) FROM income", null);
            if (cInc.moveToFirst()) monthIncome = cInc.getDouble(0);
            cInc.close();

            Cursor cExp = db.rawQuery("SELECT SUM(amount) FROM expenses", null);
            if (cExp.moveToFirst()) monthExpense = cExp.getDouble(0);
            cExp.close();

            if (monthExpense > monthIncome && monthExpense > 0) {
                insights.add(new Insight(
                        Insight.Type.FINANCIAL,
                        Insight.Priority.ATTENTION,
                        "Monthly Expenses Exceed Sales",
                        "Recorded farm expenses (" + currSym + String.format(Locale.US, "%,.0f", monthExpense) + ") exceed total sales (" + currSym + String.format(Locale.US, "%,.0f", monthIncome) + ") for this period.",
                        null, null, "FINANCIAL"
                ));
            } else if (monthIncome > monthExpense && monthIncome > 0) {
                double net = monthIncome - monthExpense;
                insights.add(new Insight(
                        Insight.Type.FINANCIAL,
                        Insight.Priority.POSITIVE,
                        "Positive Farm Balance",
                        "Net farm profit for recorded transactions is +" + currSym + String.format(Locale.US, "%,.0f", net) + ".",
                        null, null, "FINANCIAL"
                ));
            }
        } catch (Exception ignored) {}

        Collections.sort(insights);
        return insights;
    }

    private static void evaluateMilkTrends(SQLiteDatabase db, Date todayDate, List<Insight> insights) {
        try {
            Calendar cal7DaysAgo = Calendar.getInstance();
            cal7DaysAgo.setTime(todayDate);
            cal7DaysAgo.add(Calendar.DAY_OF_YEAR, -7);

            Calendar cal14DaysAgo = Calendar.getInstance();
            cal14DaysAgo.setTime(todayDate);
            cal14DaysAgo.add(Calendar.DAY_OF_YEAR, -14);

            double recent7DaysTotal = 0;
            double previous7DaysTotal = 0;
            Set<String> distinctMilkDays = new HashSet<>();

            Cursor c = db.rawQuery("SELECT datetime, litres FROM milk_production", null);
            while (c.moveToNext()) {
                String dateStr = c.getString(0);
                double litres = c.getDouble(1);

                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    try {
                        Date d = DATE_FORMAT.parse(dateStr.trim());
                        if (d != null) {
                            if (!d.before(cal14DaysAgo.getTime()) && !d.after(todayDate)) {
                                distinctMilkDays.add(dateStr.trim());
                            }
                            if (!d.before(cal7DaysAgo.getTime()) && !d.after(todayDate)) {
                                recent7DaysTotal += litres;
                            } else if (!d.before(cal14DaysAgo.getTime()) && d.before(cal7DaysAgo.getTime())) {
                                previous7DaysTotal += litres;
                            }
                        }
                    } catch (ParseException ignored) {}
                }
            }
            c.close();

            // Insufficient Data check
            if (distinctMilkDays.size() < 3) {
                insights.add(new Insight(
                        Insight.Type.DATA_QUALITY,
                        Insight.Priority.INFO,
                        "Milk Trend Data Pending",
                        "Not enough milk records over the last 14 days to calculate a reliable production trend. Record daily milk yields to enable analytics.",
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

        Date today = cal.getTime();

        // 1. Attention: Overdue vaccinations
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM vaccinations WHERE status = 'Overdue'", null);
            if (c.moveToFirst() && c.getInt(0) > 0) {
                int count = c.getInt(0);
                attention.add(count == 1 ? "1 vaccination is overdue" : count + " vaccinations are overdue");
            }
            c.close();
        } catch (Exception ignored) {}

        // Attention: Low stock items
        try {
            Cursor c = db.rawQuery("SELECT item_name FROM inventory WHERE quantity <= min_quantity", null);
            while (c.moveToNext()) {
                attention.add(c.getString(0) + " feed/supply is running low");
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

        // 2. Good news: Positive net balance or positive milk trend
        try {
            double income = 0, expense = 0;
            Cursor c1 = db.rawQuery("SELECT SUM(amount) FROM income", null);
            if (c1.moveToFirst()) income = c1.getDouble(0);
            c1.close();

            Cursor c2 = db.rawQuery("SELECT SUM(amount) FROM expenses", null);
            if (c2.moveToFirst()) expense = c2.getDouble(0);
            c2.close();

            if (income > expense && income > 0) {
                goodNews.add("Net farm balance is positive (+UGX " + String.format(Locale.US, "%,.0f", (income - expense)) + ")");
            }
        } catch (Exception ignored) {}

        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM animas WHERE lifecycle_status = 'Active'", null);
            if (c.moveToFirst() && c.getInt(0) > 0) {
                goodNews.add(c.getInt(0) == 1 ? "1 active animal registered on farm" : c.getInt(0) + " active animals registered on farm");
            }
            c.close();
        } catch (Exception ignored) {}

        // 3. Coming up: Upcoming vaccinations and upcoming calvings
        try {
            Cursor c = db.rawQuery(
                    "SELECT a.name, v.vaccine_name, v.scheduled_date " +
                            "FROM vaccinations v JOIN animas a ON v.animal_id = a.id " +
                            "WHERE v.status = 'Pending' LIMIT 2", null);
            while (c.moveToNext()) {
                String animalName = c.getString(0);
                String vaccine = c.getString(1);
                String schedStr = c.getString(2);

                if (schedStr != null && !schedStr.trim().isEmpty()) {
                    try {
                        Date d = DATE_FORMAT.parse(schedStr.trim());
                        if (d != null) {
                            long diffDays = (d.getTime() - today.getTime()) / (24 * 60 * 60 * 1000L);
                            if (diffDays >= 0 && diffDays <= 7) {
                                String dueStr = diffDays == 0 ? "today" : (diffDays == 1 ? "tomorrow" : "in " + diffDays + " days");
                                comingUp.add(animalName + " " + vaccine + " vaccination " + dueStr);
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
                            "WHERE b.status = 'Pregnant' LIMIT 2", null);
            while (c.moveToNext()) {
                String animalName = c.getString(0);
                String expStr = c.getString(1);

                if (expStr != null && !expStr.trim().isEmpty()) {
                    try {
                        Date d = DATE_FORMAT.parse(expStr.trim());
                        if (d != null) {
                            long diffDays = (d.getTime() - today.getTime()) / (24 * 60 * 60 * 1000L);
                            if (diffDays >= 0 && diffDays <= 14) {
                                String dueStr = diffDays == 0 ? "today" : (diffDays == 1 ? "tomorrow" : "in " + diffDays + " days");
                                comingUp.add(animalName + " expected calving " + dueStr);
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

        // Milk today
        try {
            Cursor c = db.rawQuery("SELECT SUM(litres) FROM milk_production WHERE datetime = ?", new String[]{todayStr});
            if (c.moveToFirst() && c.getDouble(0) > 0) {
                changes.add("+ " + String.format(Locale.US, "%.1f L", c.getDouble(0)) + " milk recorded today");
            }
            c.close();
        } catch (Exception ignored) {}

        // Completed vaccinations today
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM vaccinations WHERE status = 'Completed'", null);
            if (c.moveToFirst() && c.getInt(0) > 0) {
                int count = c.getInt(0);
                changes.add("+ " + count + (count == 1 ? " vaccination completed" : " vaccinations completed"));
            }
            c.close();
        } catch (Exception ignored) {}

        // Overdue vaccinations
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM vaccinations WHERE status = 'Overdue'", null);
            if (c.moveToFirst() && c.getInt(0) > 0) {
                int count = c.getInt(0);
                changes.add("⚠ " + count + (count == 1 ? " vaccination became overdue" : " vaccinations overdue"));
            }
            c.close();
        } catch (Exception ignored) {}

        // Today's Expenses
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*), SUM(amount) FROM expenses WHERE date = ?", new String[]{todayStr});
            if (c.moveToFirst() && c.getInt(0) > 0) {
                int count = c.getInt(0);
                changes.add("+ " + count + (count == 1 ? " expense recorded today" : " expenses recorded today"));
            }
            c.close();
        } catch (Exception ignored) {}

        // Today's Income
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*), SUM(amount) FROM income WHERE date = ?", new String[]{todayStr});
            if (c.moveToFirst() && c.getInt(0) > 0) {
                int count = c.getInt(0);
                changes.add("+ " + count + (count == 1 ? " sale/income recorded today" : " sales/income recorded today"));
            }
            c.close();
        } catch (Exception ignored) {}

        return new WhatChanged(changes);
    }
}
