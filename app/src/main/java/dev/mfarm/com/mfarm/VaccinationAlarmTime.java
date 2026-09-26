package dev.mfarm.com.mfarm;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Pure date math for vaccination reminders so it can be unit-tested
 * without Android. Reminders fire at 08:00 local time on the scheduled date.
 */
public final class VaccinationAlarmTime {
    public static final int REMINDER_HOUR = 8;
    public static final String DATE_PATTERN = "dd-MM-yyyy";

    private VaccinationAlarmTime() {}

    public static long eightAmMillis(String scheduledDate, TimeZone timeZone) {
        if (scheduledDate == null || scheduledDate.trim().isEmpty()) {
            throw new IllegalArgumentException("scheduledDate is empty");
        }
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        format.setLenient(false);
        format.setTimeZone(timeZone);
        Date parsed;
        try {
            parsed = format.parse(scheduledDate.trim());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date: " + scheduledDate, e);
        }
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid date: " + scheduledDate);
        }
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(parsed);
        calendar.set(Calendar.HOUR_OF_DAY, REMINDER_HOUR);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * @return trigger time, or {@code -1} when the scheduled day is already over
     */
    public static long triggerMillis(String scheduledDate, long nowMillis, TimeZone timeZone) {
        long eightAm = eightAmMillis(scheduledDate, timeZone);
        if (eightAm >= nowMillis) {
            return eightAm;
        }
        Calendar scheduled = Calendar.getInstance(timeZone);
        scheduled.setTimeInMillis(eightAm);
        Calendar now = Calendar.getInstance(timeZone);
        now.setTimeInMillis(nowMillis);
        boolean sameDay = scheduled.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && scheduled.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
        if (sameDay) {
            return nowMillis + 5_000L;
        }
        return -1L;
    }
}
