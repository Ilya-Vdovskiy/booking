package ru.lumastay.app;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Pure validation so every field and the submit state use the same rules. */
public final class BookingValidator {
    public static final String[] ROOM_CATEGORIES = {"Стандарт", "Комфорт", "Люкс", "Семейный"};
    private static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L}'’-]*(?:\\s+\\p{L}[\\p{L}'’-]*){1,3}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private BookingValidator() { }

    public static Map<String, String> validate(BookingDraft draft, long todayUtc) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = draft.fullName.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) errors.put("name", "Укажите фамилию и имя");
        else if (!NAME.matcher(name).matches()) errors.put("name", "Введите фамилию и имя буквами");

        String passport = draft.passport.trim();
        if (passport.isEmpty()) errors.put("passport", "Укажите паспортные данные");
        else if (!passport.matches("[0-9]{10}")) errors.put("passport", "Серия и номер: ровно 10 цифр");

        String phone = draft.phone.replaceAll("\\D", "");
        if (phone.isEmpty()) errors.put("phone", "Укажите номер телефона");
        else if (!phone.matches("(?:[78][0-9]{10}|[0-9]{10})"))
            errors.put("phone", "Введите российский номер из 10 или 11 цифр");

        String email = draft.email.trim();
        if (email.isEmpty()) errors.put("email", "Укажите электронную почту");
        else if (email.length() > 150 || !EMAIL.matcher(email).matches())
            errors.put("email", "Проверьте адрес электронной почты");

        if (draft.checkIn == null) errors.put("checkIn", "Выберите дату заезда");
        else if (draft.checkIn < todayUtc) errors.put("checkIn", "Дата заезда уже прошла");
        if (draft.checkOut == null) errors.put("checkOut", "Выберите дату выезда");
        else if (draft.checkIn != null && draft.checkOut <= draft.checkIn)
            errors.put("checkOut", "Выезд должен быть позже заезда");

        if (draft.guests.trim().isEmpty()) errors.put("guests", "Укажите число гостей");
        else {
            try {
                int guests = Integer.parseInt(draft.guests.trim());
                if (guests < 1 || guests > 8) errors.put("guests", "Допустимо от 1 до 8 гостей");
            } catch (NumberFormatException e) {
                errors.put("guests", "Допустимо от 1 до 8 гостей");
            }
        }

        boolean knownRoom = false;
        for (String category : ROOM_CATEGORIES) if (category.equals(draft.room)) knownRoom = true;
        if (!knownRoom) errors.put("room", "Выберите категорию номера");
        if (draft.wishes.length() > 500) errors.put("wishes", "Не больше 500 символов");
        return errors;
    }
}
