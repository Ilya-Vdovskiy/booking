package ru.lumastay.app;

import java.util.Calendar;
import java.util.TimeZone;

/** One local booking form. Dates are UTC midnights, as returned by MaterialDatePicker. */
public final class BookingDraft {
    public String fullName = "";
    public String passport = "";
    public String phone = "";
    public String email = "";
    public Long checkIn;
    public Long checkOut;
    public String guests = "";
    public String room = "";
    public String wishes = "";

    public int nights() {
        if (checkIn == null || checkOut == null) return 0;
        return (int) ((checkOut - checkIn) / 86_400_000L);
    }

    public static long todayUtcMidnight() {
        Calendar local = Calendar.getInstance();
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }
}
