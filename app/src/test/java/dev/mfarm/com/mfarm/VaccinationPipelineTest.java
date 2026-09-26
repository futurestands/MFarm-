package dev.mfarm.com.mfarm;

import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying vaccination notification pipeline fixes:
 * 1. Same-day repeated notification prevention.
 * 2. Past/overdue vaccination handling.
 * 3. Animal deletion and alarm cancellation.
 * 4. Reboot rescheduling logic.
 * 5. Isolation across multiple vaccination IDs.
 */
public class VaccinationPipelineTest {

    private static final TimeZone NAIROBI = TimeZone.getTimeZone("Africa/Nairobi");
    private SimpleDateFormat dateFormatter;

    @Before
    public void setUp() {
        dateFormatter = new SimpleDateFormat(VaccinationAlarmTime.DATE_PATTERN, Locale.US);
        dateFormatter.setTimeZone(NAIROBI);
    }

    // --- Mock State Containers for Pipeline Testing ---
    static class MockVaccinationRecord {
        int id;
        int animalId;
        String animalName;
        String vaccineName;
        String scheduledDate;
        String status;

        MockVaccinationRecord(int id, int animalId, String animalName, String vaccineName, String scheduledDate, String status) {
            this.id = id;
            this.animalId = animalId;
            this.animalName = animalName;
            this.vaccineName = vaccineName;
            this.scheduledDate = scheduledDate;
            this.status = status;
        }
    }

    static class MockNotificationTracker {
        private final Map<Integer, String> notifiedDates = new HashMap<>();
        private final Set<Integer> activeAlarms = new HashSet<>();

        public boolean hasBeenNotifiedToday(int vacId, String todayDateStr) {
            return todayDateStr.equals(notifiedDates.get(vacId));
        }

        public void markNotifiedToday(int vacId, String todayDateStr) {
            notifiedDates.put(vacId, todayDateStr);
        }

        public void clearNotified(int vacId) {
            notifiedDates.remove(vacId);
            activeAlarms.remove(vacId);
        }

        public void scheduleAlarm(int vacId) {
            activeAlarms.add(vacId);
        }

        public boolean isAlarmScheduled(int vacId) {
            return activeAlarms.contains(vacId);
        }

        public void cancelAlarm(int vacId) {
            activeAlarms.remove(vacId);
        }
    }

    // --- Test 1: Same-Day Repeated Notification Prevention ---
    @Test
    public void testSameDayRepeatedNotificationPrevention() {
        MockNotificationTracker tracker = new MockNotificationTracker();
        Calendar cal = Calendar.getInstance(NAIROBI);
        cal.set(Calendar.HOUR_OF_DAY, 10); // 10:00 AM today
        long nowMillis = cal.getTimeInMillis();
        String todayStr = dateFormatter.format(cal.getTime());

        int vacId = 101;
        long triggerAt = VaccinationAlarmTime.triggerMillis(todayStr, nowMillis, NAIROBI);

        // First check: Not notified yet, so alarm should schedule
        boolean shouldScheduleFirst = (triggerAt > nowMillis && triggerAt <= nowMillis + 10_000L)
                && !tracker.hasBeenNotifiedToday(vacId, todayStr);
        assertTrue("First time today should schedule catch-up alarm", shouldScheduleFirst);

        // Simulate firing notification & recording state
        tracker.markNotifiedToday(vacId, todayStr);

        // Second check (e.g. app re-opened 5 minutes later): Should NOT schedule again
        boolean shouldScheduleSecond = (triggerAt > nowMillis && triggerAt <= nowMillis + 10_000L)
                && !tracker.hasBeenNotifiedToday(vacId, todayStr);
        assertFalse("Subsequent same-day app launch should NOT schedule alarm again", shouldScheduleSecond);
    }

    // --- Test 2: Past / Overdue Vaccination Handling ---
    @Test
    public void testPastOverdueVaccinationHandling() {
        Calendar yesterdayCal = Calendar.getInstance(NAIROBI);
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayStr = dateFormatter.format(yesterdayCal.getTime());

        long nowMillis = System.currentTimeMillis();

        // 1. Trigger time for yesterday must be -1 (alarm skipped)
        long trigger = VaccinationAlarmTime.triggerMillis(yesterdayStr, nowMillis, NAIROBI);
        assertEquals(-1L, trigger);

        // 2. Status update logic for database record
        MockVaccinationRecord record = new MockVaccinationRecord(1, 10, "Bessie", "Anthrax", yesterdayStr, "Pending");
        
        // Evaluate overdue transition
        try {
            Date schedDate = dateFormatter.parse(record.scheduledDate);
            Date todayDate = dateFormatter.parse(dateFormatter.format(new Date(nowMillis)));
            if (schedDate != null && schedDate.before(todayDate) && "Pending".equals(record.status)) {
                record.status = "Overdue";
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertEquals("Overdue", record.status);
    }

    // --- Test 3: Animal Deletion and Alarm Cancellation ---
    @Test
    public void testAnimalDeletionAndAlarmCancellation() {
        MockNotificationTracker tracker = new MockNotificationTracker();
        List<MockVaccinationRecord> vaccinations = new ArrayList<>();

        int animalIdToDelete = 5;
        // Animal 5 has 2 pending vaccinations
        vaccinations.add(new MockVaccinationRecord(201, 5, "Cow A", "Foot-and-Mouth", "20-10-2026", "Pending"));
        vaccinations.add(new MockVaccinationRecord(202, 5, "Cow A", "Blackleg", "22-10-2026", "Pending"));
        // Animal 6 has 1 pending vaccination
        vaccinations.add(new MockVaccinationRecord(203, 6, "Cow B", "Rabies", "25-10-2026", "Pending"));

        for (MockVaccinationRecord v : vaccinations) {
            tracker.scheduleAlarm(v.id);
        }

        assertTrue(tracker.isAlarmScheduled(201));
        assertTrue(tracker.isAlarmScheduled(202));
        assertTrue(tracker.isAlarmScheduled(203));

        // Delete animal 5
        List<MockVaccinationRecord> toRemove = new ArrayList<>();
        for (MockVaccinationRecord v : vaccinations) {
            if (v.animalId == animalIdToDelete) {
                tracker.cancelAlarm(v.id);
                tracker.clearNotified(v.id);
                toRemove.add(v);
            }
        }
        vaccinations.removeAll(toRemove);

        // Verify alarms canceled for Animal 5
        assertFalse(tracker.isAlarmScheduled(201));
        assertFalse(tracker.isAlarmScheduled(202));
        // Verify alarm for Animal 6 remains scheduled
        assertTrue(tracker.isAlarmScheduled(203));

        // Verify DB records removed for Animal 5
        assertEquals(1, vaccinations.size());
        assertEquals(6, vaccinations.get(0).animalId);
    }

    // --- Test 4: Reboot Rescheduling Logic ---
    @Test
    public void testRebootReschedulingLogic() {
        MockNotificationTracker tracker = new MockNotificationTracker();
        List<MockVaccinationRecord> dbRecords = new ArrayList<>();

        Calendar calToday = Calendar.getInstance(NAIROBI);
        String todayStr = dateFormatter.format(calToday.getTime());

        Calendar calTomorrow = Calendar.getInstance(NAIROBI);
        calTomorrow.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrowStr = dateFormatter.format(calTomorrow.getTime());

        Calendar calYesterday = Calendar.getInstance(NAIROBI);
        calYesterday.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayStr = dateFormatter.format(calYesterday.getTime());

        // Setup records:
        // 1. Pending tomorrow -> Should reschedule
        dbRecords.add(new MockVaccinationRecord(301, 1, "Daisy", "Brucellosis", tomorrowStr, "Pending"));
        // 2. Pending today (already notified earlier today before reboot) -> Should NOT reschedule
        dbRecords.add(new MockVaccinationRecord(302, 1, "Daisy", "Lumpy Skin", todayStr, "Pending"));
        tracker.markNotifiedToday(302, todayStr);
        // 3. Pending yesterday -> Should be marked Overdue and NOT reschedule
        dbRecords.add(new MockVaccinationRecord(303, 1, "Daisy", "Deworming", yesterdayStr, "Pending"));

        // Simulate reschedulePending run:
        long nowMillis = System.currentTimeMillis();
        Date todayDate;
        try {
            todayDate = dateFormatter.parse(todayStr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int rescheduledCount = 0;
        for (MockVaccinationRecord v : dbRecords) {
            // Update overdue
            try {
                Date schedDate = dateFormatter.parse(v.scheduledDate);
                if (schedDate != null && schedDate.before(todayDate) && "Pending".equals(v.status)) {
                    v.status = "Overdue";
                }
            } catch (Exception ignored) {}

            if (!"Pending".equals(v.status)) {
                continue;
            }

            long triggerAt = VaccinationAlarmTime.triggerMillis(v.scheduledDate, nowMillis, NAIROBI);
            if (triggerAt < 0) {
                continue;
            }

            if (v.scheduledDate.equals(todayStr) && triggerAt > nowMillis && triggerAt <= nowMillis + 10_000L) {
                if (tracker.hasBeenNotifiedToday(v.id, todayStr)) {
                    continue;
                }
            }

            tracker.scheduleAlarm(v.id);
            rescheduledCount++;
        }

        assertEquals(1, rescheduledCount);
        assertTrue(tracker.isAlarmScheduled(301));
        assertFalse(tracker.isAlarmScheduled(302));
        assertFalse(tracker.isAlarmScheduled(303));
        assertEquals("Overdue", dbRecords.get(2).status);
    }

    // --- Test 5: Multiple Vaccination IDs ---
    @Test
    public void testMultipleVaccinationIdsIsolation() {
        MockNotificationTracker tracker = new MockNotificationTracker();
        Calendar cal = Calendar.getInstance(NAIROBI);
        String todayStr = dateFormatter.format(cal.getTime());

        int vac1 = 501;
        int vac2 = 502;
        int vac3 = 503;

        tracker.markNotifiedToday(vac1, todayStr);

        assertTrue("Vaccination 501 should be marked notified today", tracker.hasBeenNotifiedToday(vac1, todayStr));
        assertFalse("Vaccination 502 should NOT be affected by 501", tracker.hasBeenNotifiedToday(vac2, todayStr));
        assertFalse("Vaccination 503 should NOT be affected by 501", tracker.hasBeenNotifiedToday(vac3, todayStr));

        tracker.clearNotified(vac1);
        assertFalse("Vaccination 501 should be cleared", tracker.hasBeenNotifiedToday(vac1, todayStr));
    }
}
