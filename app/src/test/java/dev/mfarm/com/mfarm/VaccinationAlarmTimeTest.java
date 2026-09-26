package dev.mfarm.com.mfarm;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VaccinationAlarmTimeTest {

    private static final TimeZone NAIROBI = TimeZone.getTimeZone("Africa/Nairobi");

    @Test
    public void eightAmIsOnScheduledLocalMorning() {
        long millis = VaccinationAlarmTime.eightAmMillis("18-09-2026", NAIROBI);
        Calendar calendar = Calendar.getInstance(NAIROBI);
        calendar.setTimeInMillis(millis);
        assertEquals(2026, calendar.get(Calendar.YEAR));
        assertEquals(Calendar.SEPTEMBER, calendar.get(Calendar.MONTH));
        assertEquals(18, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(8, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
    }

    @Test
    public void futureMorningKeepsEightAm() {
        long eightAm = VaccinationAlarmTime.eightAmMillis("20-09-2026", NAIROBI);
        long now = eightAm - 86_400_000L;
        assertEquals(eightAm, VaccinationAlarmTime.triggerMillis("20-09-2026", now, NAIROBI));
    }

    @Test
    public void laterSameDayFiresSoonInsteadOfDropping() {
        long eightAm = VaccinationAlarmTime.eightAmMillis("18-09-2026", NAIROBI);
        long afternoon = eightAm + 6 * 60 * 60 * 1000L;
        long trigger = VaccinationAlarmTime.triggerMillis("18-09-2026", afternoon, NAIROBI);
        assertEquals(afternoon + 5_000L, trigger);
    }

    @Test
    public void pastDayIsSkipped() {
        long eightAm = VaccinationAlarmTime.eightAmMillis("17-09-2026", NAIROBI);
        long nextDay = eightAm + 30 * 60 * 60 * 1000L;
        assertEquals(-1L, VaccinationAlarmTime.triggerMillis("17-09-2026", nextDay, NAIROBI));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsGarbageDate() {
        VaccinationAlarmTime.eightAmMillis("not-a-date", NAIROBI);
    }

    @Test
    public void reminderHourIsMorning() {
        assertTrue(VaccinationAlarmTime.REMINDER_HOUR >= 6);
        assertTrue(VaccinationAlarmTime.REMINDER_HOUR <= 10);
    }
}
