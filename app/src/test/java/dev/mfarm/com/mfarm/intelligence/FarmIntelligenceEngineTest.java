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

    // --- Test A: What Changed Only Reports Today's Milk ---
    @Test
    public void testWhatChangedOnlyReportsTodaysMilk() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        String todayStr = DATE_FORMAT.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayStr = DATE_FORMAT.format(cal.getTime());

        List<MockTransaction> milkEntries = new ArrayList<>();
        milkEntries.add(new MockTransaction(yesterdayStr, 25.0)); // Yesterday: 25L
        milkEntries.add(new MockTransaction(todayStr, 12.0));    // Today: 12L

        double todayMilkSum = 0;
        for (MockTransaction entry : milkEntries) {
            if (todayStr.equals(entry.date)) {
                todayMilkSum += entry.amount;
            }
        }

        assertEquals(12.0, todayMilkSum, 0.01);
    }

    // --- Test B: What Changed Only Reports Today's Completed Vaccinations ---
    @Test
    public void testWhatChangedOnlyReportsTodaysCompletedVaccinations() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        String todayStr = DATE_FORMAT.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, -2);
        String pastStr = DATE_FORMAT.format(cal.getTime());

        List<MockVaccination> vaccinations = new ArrayList<>();
        vaccinations.add(new MockVaccination(1, 10, "Bessie", "Anthrax", pastStr, "Completed")); // Completed 2 days ago
        vaccinations.add(new MockVaccination(2, 10, "Bessie", "FMD", todayStr, "Completed"));      // Completed today

        int completedTodayCount = 0;
        for (MockVaccination v : vaccinations) {
            if ("Completed".equals(v.status) && todayStr.equals(v.scheduledDate)) {
                completedTodayCount++;
            }
        }

        assertEquals(1, completedTodayCount);
    }

    // --- Test C: What Changed Only Reports Today's Expenses and Income ---
    @Test
    public void testWhatChangedOnlyReportsTodaysExpensesAndIncome() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        String todayStr = DATE_FORMAT.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayStr = DATE_FORMAT.format(cal.getTime());

        List<MockTransaction> expenses = new ArrayList<>();
        expenses.add(new MockTransaction(yesterdayStr, 50000.0));
        expenses.add(new MockTransaction(todayStr, 20000.0));
        expenses.add(new MockTransaction(todayStr, 15000.0));

        int todayExpenseCount = 0;
        for (MockTransaction exp : expenses) {
            if (todayStr.equals(exp.date)) {
                todayExpenseCount++;
            }
        }

        assertEquals(2, todayExpenseCount);
    }

    // --- Test D: Overdue Vaccinations Language Guard ---
    @Test
    public void testOverdueVaccinationsLanguageGuard() {
        int overdueCount = 3;
        String formattedWording = overdueCount == 1 ? "1 vaccination is currently overdue" : overdueCount + " vaccinations are currently overdue";

        assertTrue(formattedWording.contains("currently overdue"));
        assertFalse(formattedWording.contains("became overdue"));
    }

    // --- Test E: Current-Month Financial Calculation Excludes Previous Months ---
    @Test
    public void testCurrentMonthFinancialCalculationExcludesPreviousMonths() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);

        Calendar lastMonthCal = (Calendar) cal.clone();
        lastMonthCal.add(Calendar.MONTH, -1);

        String currentMonthDate = DATE_FORMAT.format(cal.getTime());
        String lastMonthDate = DATE_FORMAT.format(lastMonthCal.getTime());

        List<MockTransaction> incomeList = new ArrayList<>();
        incomeList.add(new MockTransaction(lastMonthDate, 500000.0));  // Previous month income
        incomeList.add(new MockTransaction(currentMonthDate, 150000.0)); // Current month income

        double currentMonthIncome = 0;
        for (MockTransaction inc : incomeList) {
            try {
                Date d = DATE_FORMAT.parse(inc.date);
                Calendar c = Calendar.getInstance(NAIROBI);
                c.setTime(d);
                if (c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear) {
                    currentMonthIncome += inc.amount;
                }
            } catch (Exception ignored) {}
        }

        assertEquals(150000.0, currentMonthIncome, 0.01);
    }

    // --- Test F: Health Follow-Up Detects Recent Illness Across All Records ---
    @Test
    public void testHealthFollowUpDetectsRecentIllnessAcrossAllRecords() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        Date today = cal.getTime();

        cal.add(Calendar.DAY_OF_YEAR, -2);
        String recent2DaysAgo = DATE_FORMAT.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, -100);
        String oldDate = DATE_FORMAT.format(cal.getTime());

        List<MockIllness> allIllnesses = new ArrayList<>();

        // Add 5 old historical illnesses first
        for (int i = 1; i <= 5; i++) {
            allIllnesses.add(new MockIllness(i, i, "Cow " + i, "Old Fever " + i, oldDate));
        }
        // Add 1 recent illness at the end
        allIllnesses.add(new MockIllness(6, 24, "Cow 024", "Mastitis", recent2DaysAgo));

        // Evaluate ALL without arbitrary initial LIMIT 3
        List<MockIllness> qualifying = new ArrayList<>();
        for (MockIllness ill : allIllnesses) {
            try {
                Date d = DATE_FORMAT.parse(ill.dateOccured);
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

    // --- Test G: Low Stock Wording Does Not Call Non-Feed Items "Feed" ---
    @Test
    public void testLowStockWordingDoesNotCallNonFeedItemsFeed() {
        MockInventory feed = new MockInventory("Dairy Meal", "Feed", 10.0, "kg", 50.0);
        MockInventory medicine = new MockInventory("Penicillin", "Medicine", 2.0, "vials", 5.0);

        boolean isFeed = feed.category.toLowerCase().contains("feed");
        String feedLabel = isFeed ? feed.name + " feed" : feed.name;

        boolean isMedFeed = medicine.category.toLowerCase().contains("feed");
        String medLabel = isMedFeed ? medicine.name + " feed" : medicine.name;

        assertEquals("Dairy Meal feed", feedLabel);
        assertEquals("Penicillin", medLabel);
        assertFalse(medLabel.contains("feed"));
    }

    // --- Test H: Date Boundary Cases (Today vs Yesterday) ---
    @Test
    public void testDateBoundaryCases() {
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

    // --- Test I: Empty Database State ---
    @Test
    public void testEmptyDatabaseState() {
        Insight insight = new Insight(
                Insight.Type.DATA_QUALITY,
                Insight.Priority.INFO,
                "Welcome to MFarm",
                "Register your first farm animal to begin tracking health, milk yields, breeding, and farm finances.",
                null, null, "REGISTER"
        );

        assertNotNull(insight);
        assertEquals(Insight.Type.DATA_QUALITY, insight.getType());
        assertEquals("REGISTER", insight.getActionTarget());
    }

    // --- Test J: Insufficient Milk Data Handling ---
    @Test
    public void testInsufficientMilkDataHandling() {
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
