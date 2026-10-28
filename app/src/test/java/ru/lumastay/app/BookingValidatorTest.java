package ru.lumastay.app;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public final class BookingValidatorTest {
    private static final long TODAY = 1_800_000_000_000L;
    private static final long DAY = 86_400_000L;

    private BookingDraft validDraft() {
        BookingDraft draft = new BookingDraft();
        draft.fullName = "Иван Иванов";
        draft.passport = "1234567890";
        draft.phone = "+7 999 123-45-67";
        draft.email = "ivan@example.ru";
        draft.checkIn = TODAY + DAY;
        draft.checkOut = TODAY + 3 * DAY;
        draft.guests = "2";
        draft.room = "Комфорт";
        return draft;
    }

    @Test public void validBookingAcceptsEmptyOptionalWishes() {
        assertTrue(BookingValidator.validate(validDraft(), TODAY).isEmpty());
    }

    @Test public void blankFormReportsEachRequiredFieldWithoutCrashing() {
        Map<String, String> errors = BookingValidator.validate(new BookingDraft(), TODAY);
        for (String key : new String[] {"name", "passport", "phone", "email", "checkIn", "checkOut", "guests", "room"})
            assertTrue(key, errors.containsKey(key));
        assertFalse(errors.containsKey("wishes"));
    }

    @Test public void malformedFieldsAndReversedDatesAreRejected() {
        BookingDraft draft = validDraft();
        draft.fullName = "   ";
        draft.passport = "123";
        draft.phone = "abc";
        draft.email = "not-an-email";
        draft.checkIn = TODAY - DAY;
        draft.checkOut = TODAY - 2 * DAY;
        draft.guests = "999999999999999999999999999";
        draft.room = "Несуществующий";
        assertEquals(8, BookingValidator.validate(draft, TODAY).size());
    }

    @Test public void dateRangeAndGuestLimitsAreEnforced() {
        BookingDraft draft = validDraft();
        draft.checkOut = draft.checkIn;
        draft.guests = "0";
        assertTrue(BookingValidator.validate(draft, TODAY).containsKey("checkOut"));
        assertTrue(BookingValidator.validate(draft, TODAY).containsKey("guests"));
        draft.checkOut += DAY;
        draft.guests = "8";
        assertTrue(BookingValidator.validate(draft, TODAY).isEmpty());
    }
}
