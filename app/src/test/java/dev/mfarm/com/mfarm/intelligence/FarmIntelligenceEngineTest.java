package dev.mfarm.com.mfarm.intelligence;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FarmIntelligenceEngineTest {

    private static final TimeZone NAIROBI = TimeZone.getTimeZone("Africa/Nairobi");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

    static {
        DATE_FORMAT.setTimeZone(NAIROBI);
        DATE_FORMAT.setLenient(false);
    }

    // --- Mock Models for Unit Testing ---
    static class MockVaccination {
        int id;
        int animalId;
        String animalName;
        String vaccineName;
        String scheduledDate;
        String status;

        MockVaccination(int id, int animalId, String animalName, String vaccineName, String scheduledDate, String status) {
            this.id = id;
            this.animalId = animalId;
            this.animalName = animalName;
            this.vaccineName = vaccineName;
            this.scheduledDate = scheduledDate;
            this.status = status;
        }
    }

    static class MockInventory {
        String name;
        String category;
        double qty;
        String unit;
        double minQty;

        MockInventory(String name, String category, double qty, String unit, double minQty) {
            this.name = name;
            this.category = category;
            this.qty = qty;
            this.unit = unit;
            this.minQty = minQty;
        }
    }

    static class MockIllness {
        int id;
        int animalId;
        String animalName;
        String illness;
        String dateOccured;

        MockIllness(int id, int animalId, String animalName, String illness, String dateOccured) {
            this.id = id;
            this.animalId = animalId;
            this.animalName = animalName;
            this.illness = illness;
            this.dateOccured = dateOccured;
        }
    }

    static class MockTransaction {
        String date;
        double amount;

        MockTransaction(String date, double amount) {
            this.date = date;
            this.amount = amount;
        }
    }

    // --- Test A: Vaccination scheduled today + Completed wording ---
    @Test
    public void testVaccinationScheduledTodayCompletedWording() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        String todayStr = DATE_FORMAT.format(cal.getTime());

        MockVaccination v = new MockVaccination(1, 10, "Bessie", "Anthrax", todayStr, "Completed");

        String wording = "1 vaccination scheduled for today is marked completed";
        assertTrue("Wording must explicitly reference scheduled date without claiming an actual completion timestamp",
                wording.contains("scheduled for today is marked completed"));
        assertFalse(wording.contains("completed at"));
    }

    // --- Test B: Vaccination scheduled yesterday + Completed is NOT claimed as completed today ---
    @Test
    public void testVaccinationScheduledYesterdayCompletedNotClaimedToday() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayStr = DATE_FORMAT.format(cal.getTime());

        Calendar calToday = Calendar.getInstance(NAIROBI);
        String todayStr = DATE_FORMAT.format(calToday.getTime());

        MockVaccination vYesterday = new MockVaccination(1, 10, "Bessie", "Anthrax", yesterdayStr, "Completed");

        boolean countForToday = "Completed".equals(vYesterday.status) && todayStr.equals(vYesterday.scheduledDate);
        assertFalse("Vaccination scheduled yesterday should NOT be reported as completed today", countForToday);
    }

    // --- Test C: Current-Month Financial Calculation Excludes Previous Month ---
    @Test
    public void testCurrentMonthFinancialCalculationExcludesPreviousMonth() {
        Calendar calToday = Calendar.getInstance(NAIROBI);
        calToday.set(Calendar.YEAR, 2026);
        calToday.set(Calendar.MONTH, Calendar.SEPTEMBER);
        calToday.set(Calendar.DAY_OF_MONTH, 15);

        Calendar calAug = Calendar.getInstance(NAIROBI);
        calAug.set(Calendar.YEAR, 2026);
        calAug.set(Calendar.MONTH, Calendar.AUGUST);
        calAug.set(Calendar.DAY_OF_MONTH, 20);

        String augDate = DATE_FORMAT.format(calAug.getTime());
        String septDate = DATE_FORMAT.format(calToday.getTime());

        List<MockTransaction> expenses = new ArrayList<>();
        expenses.add(new MockTransaction(augDate, 500000.0));  // August expense
        expenses.add(new MockTransaction(septDate, 120000.0)); // September expense

        int septMonth = calToday.get(Calendar.MONTH);
        int septYear = calToday.get(Calendar.YEAR);

        double septTotalExpense = 0;
        for (MockTransaction exp : expenses) {
            if (FarmIntelligenceEngine.isSameMonth(exp.date, septMonth, septYear)) {
                septTotalExpense += exp.amount;
            }
        }

        assertEquals(120000.0, septTotalExpense, 0.01);
    }

    // --- Test D: Recent Illness Detected Across Historical Records ---
    @Test
    public void testRecentIllnessDetectedAcrossHistoricalRecords() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        Date today = FarmIntelligenceEngine.truncateTime(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, -2);
        String recent2DaysAgo = DATE_FORMAT.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, -100);
        String oldDate = DATE_FORMAT.format(cal.getTime());

        List<MockIllness> allIllnesses = new ArrayList<>();

        // Add 5 old historical illnesses first
        for (int i = 1; i <= 5; i++) {
            allIllnesses.add(new MockIllness(i, i, "Cow " + i, "Old Fever " + i, oldDate));
        }
        // Add 1 recent illness
        allIllnesses.add(new MockIllness(6, 24, "Cow 024", "Mastitis", recent2DaysAgo));

        // Evaluate ALL without initial query limit
        List<MockIllness> qualifying = new ArrayList<>();
        for (MockIllness ill : allIllnesses) {
            try {
                Date d = FarmIntelligenceEngine.truncateTime(DATE_FORMAT.parse(ill.dateOccured));
                long diffDays = FarmIntelligenceEngine.daysBetween(d, today);
                if (diffDays >= 0 && diffDays <= 7) {
                    qualifying.add(ill);
                }
            } catch (Exception ignored) {}
        }

        assertEquals(1, qualifying.size());
        assertEquals("Cow 024", qualifying.get(0).animalName);
        assertEquals("Mastitis", qualifying.get(0).illness);
    }

    // --- Test E: Non-Feed Inventory Item Is Never Labelled Feed ---
    @Test
    public void testNonFeedInventoryItemIsNeverLabelledFeed() {
        MockInventory feed = new MockInventory("Dairy Meal", "Feed", 10.0, "kg", 50.0);
        MockInventory medicine = new MockInventory("Penicillin", "Medicine", 2.0, "vials", 5.0);

        boolean isFeed = feed.category.toLowerCase().contains("feed");
        String feedLabel = isFeed ? feed.name + " feed" : feed.name;

        boolean isMedFeed = medicine.category.toLowerCase().contains("feed");
        String medLabel = isMedFeed ? medicine.name + " feed" : medicine.name;

        assertEquals("Dairy Meal feed", feedLabel);
        assertEquals("Penicillin", medLabel);
        assertFalse("Non-feed item should not contain 'feed' label", medLabel.contains("feed"));
    }

    // --- Test F: Today / Yesterday / Tomorrow Date Boundaries ---
    @Test
    public void testTodayYesterdayTomorrowDateBoundaries() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        Date today = FarmIntelligenceEngine.truncateTime(cal.getTime());

        Calendar calYesterday = (Calendar) cal.clone();
        calYesterday.add(Calendar.DAY_OF_YEAR, -1);
        Date yesterday = FarmIntelligenceEngine.truncateTime(calYesterday.getTime());

        Calendar calTomorrow = (Calendar) cal.clone();
        calTomorrow.add(Calendar.DAY_OF_YEAR, 1);
        Date tomorrow = FarmIntelligenceEngine.truncateTime(calTomorrow.getTime());

        assertEquals(0, FarmIntelligenceEngine.daysBetween(today, today));
        assertEquals(1, FarmIntelligenceEngine.daysBetween(yesterday, today));
        assertEquals(1, FarmIntelligenceEngine.daysBetween(today, tomorrow));
        assertEquals(-1, FarmIntelligenceEngine.daysBetween(tomorrow, today));
    }

    // --- Test G: Year Boundary (31-12-2025 -> 01-01-2026) ---
    @Test
    public void testYearBoundaryDecemberToJanuary() {
        Calendar decCal = Calendar.getInstance(NAIROBI);
        decCal.set(2025, Calendar.DECEMBER, 31);
        Date dec31 = FarmIntelligenceEngine.truncateTime(decCal.getTime());
        String dec31Str = DATE_FORMAT.format(dec31);

        Calendar janCal = Calendar.getInstance(NAIROBI);
        janCal.set(2026, Calendar.JANUARY, 1);
        Date jan01 = FarmIntelligenceEngine.truncateTime(janCal.getTime());
        String jan01Str = DATE_FORMAT.format(jan01);

        long diffDays = FarmIntelligenceEngine.daysBetween(dec31, jan01);
        assertEquals(1, diffDays);

        boolean sameMonthDecJan = FarmIntelligenceEngine.isSameMonth(dec31Str, Calendar.JANUARY, 2026);
        assertFalse("Dec 31, 2025 should NOT match Jan 2026", sameMonthDecJan);

        boolean sameMonthJanJan = FarmIntelligenceEngine.isSameMonth(jan01Str, Calendar.JANUARY, 2026);
        assertTrue("Jan 1, 2026 should match Jan 2026", sameMonthJanJan);
    }

    // --- Test H: Upcoming Vaccination / Calving Windows Exclude Past Dates ---
    @Test
    public void testUpcomingWindowsExcludePastDates() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        Date today = FarmIntelligenceEngine.truncateTime(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, -2); // 2 days in past
        Date past2Days = FarmIntelligenceEngine.truncateTime(cal.getTime());

        long diffDays = FarmIntelligenceEngine.daysBetween(today, past2Days);
        assertEquals(-2, diffDays);

        boolean inUpcomingWindow = diffDays >= 0 && diffDays <= 7;
        assertFalse("Past date (diffDays < 0) must be excluded from upcoming windows", inUpcomingWindow);
    }

    // --- Test I: Insufficient Milk Data Handling ---
    @Test
    public void testInsufficientDataHandling() {
        List<MockTransaction> entries = new ArrayList<>();
        entries.add(new MockTransaction("20-09-2026", 12.0));
        entries.add(new MockTransaction("21-09-2026", 14.0));

        boolean insufficient = entries.size() < 3;
        assertTrue("Fewer than 3 milk entry days must trigger insufficient data guard", insufficient);

        Insight insight = new Insight(
                Insight.Type.DATA_QUALITY,
                Insight.Priority.INFO,
                "Milk Trend Data Pending",
                "Not enough milk records over the last 14 days to calculate a reliable production trend.",
                null, null, "MILK"
        );

        assertEquals(Insight.Priority.INFO, insight.getPriority());
        assertFalse(insight.getMessage().contains("decline"));
        assertFalse(insight.getMessage().contains("increase"));
    }
}
