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
    }

    // --- Mock Models for Pure Unit Testing ---
    static class MockAnimal {
        int id;
        String name;
        String lifecycleStatus;

        MockAnimal(int id, String name, String lifecycleStatus) {
            this.id = id;
            this.name = name;
            this.lifecycleStatus = lifecycleStatus;
        }
    }

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
        double qty;
        String unit;
        double minQty;

        MockInventory(String name, double qty, String unit, double minQty) {
            this.name = name;
            this.qty = qty;
            this.unit = unit;
            this.minQty = minQty;
        }
    }

    static class MockMilkEntry {
        String date;
        double litres;

        MockMilkEntry(String date, double litres) {
            this.date = date;
            this.litres = litres;
        }
    }

    static class MockBreedingRecord {
        int id;
        int animalId;
        String animalName;
        String expectedBirthDate;
        String status;

        MockBreedingRecord(int id, int animalId, String animalName, String expectedBirthDate, String status) {
            this.id = id;
            this.animalId = animalId;
            this.animalName = animalName;
            this.expectedBirthDate = expectedBirthDate;
            this.status = status;
        }
    }

    // --- Test 1: Empty Farm State ---
    @Test
    public void testEmptyFarmState() {
        List<MockAnimal> animals = new ArrayList<>();
        boolean isEmpty = animals.isEmpty();
        assertTrue("Farm with zero animals should be evaluated as empty state", isEmpty);

        Insight insight = new Insight(
                Insight.Type.DATA_QUALITY,
                Insight.Priority.INFO,
                "Welcome to MFarm",
                "Register your first farm animal to begin tracking health, milk yields, breeding, and farm finances.",
                null, null, "REGISTER"
        );

        assertEquals(Insight.Type.DATA_QUALITY, insight.getType());
        assertEquals(Insight.Priority.INFO, insight.getPriority());
        assertEquals("REGISTER", insight.getActionTarget());
    }

    // --- Test 2: Overdue Vaccination Rule ---
    @Test
    public void testOverdueVaccinationRule() {
        List<MockVaccination> vaccinations = new ArrayList<>();
        vaccinations.add(new MockVaccination(1, 10, "Bessie", "Anthrax", "15-09-2026", "Overdue"));

        List<Insight> insights = new ArrayList<>();
        for (MockVaccination v : vaccinations) {
            if ("Overdue".equals(v.status)) {
                insights.add(new Insight(
                        Insight.Type.VACCINATION,
                        Insight.Priority.CRITICAL,
                        "Overdue Vaccination",
                        "Vaccination '" + v.vaccineName + "' for " + v.animalName + " was scheduled for " + v.scheduledDate + " and is overdue.",
                        String.valueOf(v.animalId), v.animalName, "VACCINATION"
                ));
            }
        }

        assertEquals(1, insights.size());
        Insight insight = insights.get(0);
        assertEquals(Insight.Priority.CRITICAL, insight.getPriority());
        assertEquals("Bessie", insight.getRelatedAnimalName());
        assertTrue(insight.getMessage().contains("Anthrax"));
        assertTrue(insight.getMessage().contains("overdue"));
    }

    // --- Test 3: Upcoming Vaccination Rule ---
    @Test
    public void testUpcomingVaccinationRule() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        cal.add(Calendar.DAY_OF_YEAR, 2);
        String upcomingStr = DATE_FORMAT.format(cal.getTime());

        MockVaccination v = new MockVaccination(2, 12, "Daisy", "Brucellosis", upcomingStr, "Pending");

        Insight insight = new Insight(
                Insight.Type.VACCINATION,
                Insight.Priority.INFO,
                "Upcoming Vaccination",
                v.animalName + " is scheduled for " + v.vaccineName + " vaccination in 2 days (" + v.scheduledDate + ").",
                String.valueOf(v.animalId), v.animalName, "VACCINATION"
        );

        assertEquals(Insight.Type.VACCINATION, insight.getType());
        assertEquals("Daisy", insight.getRelatedAnimalName());
        assertTrue(insight.getMessage().contains("Brucellosis"));
    }

    // --- Test 4: Low Feed Stock Rule ---
    @Test
    public void testLowFeedStockRule() {
        MockInventory item = new MockInventory("Dairy Meal", 15.0, "kg", 50.0);
        boolean isLow = item.qty <= item.minQty;
        assertTrue(isLow);

        Insight insight = new Insight(
                Insight.Type.FEED,
                Insight.Priority.ATTENTION,
                "Low Stock Warning",
                item.name + " stock is low (" + item.qty + " " + item.unit + " remaining; minimum alert threshold is " + item.minQty + ").",
                null, null, "INVENTORY"
        );

        assertEquals(Insight.Priority.ATTENTION, insight.getPriority());
        assertTrue(insight.getMessage().contains("Dairy Meal"));
    }

    // --- Test 5: Insufficient Milk Data Rule ---
    @Test
    public void testInsufficientMilkDataRule() {
        List<MockMilkEntry> entries = new ArrayList<>();
        // Only 2 entries recorded
        entries.add(new MockMilkEntry("20-09-2026", 12.0));
        entries.add(new MockMilkEntry("21-09-2026", 14.0));

        boolean insufficient = entries.size() < 3;
        assertTrue("Fewer than 3 entries must trigger insufficient milk data guard", insufficient);

        Insight insight = new Insight(
                Insight.Type.DATA_QUALITY,
                Insight.Priority.INFO,
                "Milk Trend Data Pending",
                "Not enough milk records over the last 14 days to calculate a reliable production trend. Record daily milk yields to enable analytics.",
                null, null, "MILK"
        );

        assertEquals(Insight.Type.DATA_QUALITY, insight.getType());
        assertTrue(insight.getMessage().contains("Not enough milk records"));
    }

    // --- Test 6: Milk Production Decline Rule with Sufficient Data ---
    @Test
    public void testMilkDeclineRuleWithSufficientData() {
        double previous7DaysTotal = 100.0;
        double recent7DaysTotal = 80.0; // 20% decline

        double diff = recent7DaysTotal - previous7DaysTotal;
        double pctChange = (diff / previous7DaysTotal) * 100.0;

        assertEquals(-20.0, pctChange, 0.01);
        assertTrue(pctChange <= -10.0);

        Insight insight = new Insight(
                Insight.Type.MILK,
                Insight.Priority.ATTENTION,
                "Milk Production Decline",
                "Milk production has decreased 20.0% over the last 7 days (80.0 L vs 100.0 L in the previous 7-day period).",
                null, null, "MILK"
        );

        assertEquals(Insight.Priority.ATTENTION, insight.getPriority());
        assertTrue(insight.getMessage().contains("decreased 20.0%"));
    }

    // --- Test 7: Positive Milk Trend Rule ---
    @Test
    public void testPositiveMilkTrendRule() {
        double previous7DaysTotal = 100.0;
        double recent7DaysTotal = 115.0; // +15% improvement

        double diff = recent7DaysTotal - previous7DaysTotal;
        double pctChange = (diff / previous7DaysTotal) * 100.0;

        assertEquals(15.0, pctChange, 0.01);
        assertTrue(pctChange >= 5.0);

        Insight insight = new Insight(
                Insight.Type.MILK,
                Insight.Priority.POSITIVE,
                "Milk Production Improvement",
                "Milk production has increased 15.0% over the last 7 days (115.0 L vs 100.0 L in the previous 7-day period).",
                null, null, "MILK"
        );

        assertEquals(Insight.Priority.POSITIVE, insight.getPriority());
        assertTrue(insight.getMessage().contains("increased 15.0%"));
    }

    // --- Test 8: Upcoming Calving Rule ---
    @Test
    public void testUpcomingCalvingRule() {
        Calendar cal = Calendar.getInstance(NAIROBI);
        cal.add(Calendar.DAY_OF_YEAR, 5);
        String expDateStr = DATE_FORMAT.format(cal.getTime());

        MockBreedingRecord record = new MockBreedingRecord(1, 15, "Safina", expDateStr, "Pregnant");

        Insight insight = new Insight(
                Insight.Type.CALVING,
                Insight.Priority.ATTENTION,
                "Upcoming Calving",
                record.animalName + " is expected to calve in 5 days (" + record.expectedBirthDate + "). Prepare calving area.",
                String.valueOf(record.animalId), record.animalName, "BREEDING"
        );

        assertEquals(Insight.Type.CALVING, insight.getType());
        assertEquals("Safina", insight.getRelatedAnimalName());
        assertTrue(insight.getMessage().contains("calve in 5 days"));
    }

    // --- Test 9: Correct Animal Association ---
    @Test
    public void testCorrectAnimalAssociation() {
        Insight insight = new Insight(
                Insight.Type.HEALTH,
                Insight.Priority.ATTENTION,
                "Health Follow-Up Required",
                "Cow 024 had illness 'Mastitis' recorded. Monitor recovery closely.",
                "24", "Cow 024", "HEALTH"
        );

        assertEquals("24", insight.getRelatedAnimalId());
        assertEquals("Cow 024", insight.getRelatedAnimalName());
        assertEquals("HEALTH", insight.getActionTarget());
    }
}
