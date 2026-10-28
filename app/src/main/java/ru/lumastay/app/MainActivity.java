package ru.lumastay.app;

import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public final class MainActivity extends AppCompatActivity {
    private static final String[] FIELDS = {"checkIn", "checkOut", "name", "passport", "phone", "email", "guests", "room", "wishes"};
    private final Map<String, TextInputLayout> layouts = new HashMap<>();
    private final Set<String> touched = new HashSet<>();
    private SharedPreferences prefs;
    private ScrollView scroll;
    private TextInputEditText nameEdit, passportEdit, phoneEdit, emailEdit, guestsEdit, wishesEdit, checkInEdit, checkOutEdit;
    private MaterialAutoCompleteTextView roomEdit;
    private MaterialButton submitButton;
    private TextView staySummary;
    private Long checkIn, checkOut;
    private boolean restoring;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("booking_draft", MODE_PRIVATE);
        bindViews();
        installStatusScrim();
        setupInputs();
        restoreDraft();
        refreshValidation();
    }

    private void bindViews() {
        scroll = findViewById(R.id.form_scroll);
        nameEdit = findViewById(R.id.name);
        passportEdit = findViewById(R.id.passport);
        phoneEdit = findViewById(R.id.phone);
        emailEdit = findViewById(R.id.email);
        guestsEdit = findViewById(R.id.guests);
        wishesEdit = findViewById(R.id.wishes);
        checkInEdit = findViewById(R.id.check_in);
        checkOutEdit = findViewById(R.id.check_out);
        roomEdit = findViewById(R.id.room);
        submitButton = findViewById(R.id.submit_button);
        staySummary = findViewById(R.id.stay_summary);
        layouts.put("checkIn", findViewById(R.id.check_in_layout));
        layouts.put("checkOut", findViewById(R.id.check_out_layout));
        layouts.put("name", findViewById(R.id.name_layout));
        layouts.put("passport", findViewById(R.id.passport_layout));
        layouts.put("phone", findViewById(R.id.phone_layout));
        layouts.put("email", findViewById(R.id.email_layout));
        layouts.put("guests", findViewById(R.id.guests_layout));
        layouts.put("room", findViewById(R.id.room_layout));
        layouts.put("wishes", findViewById(R.id.wishes_layout));
    }

    private void installStatusScrim() {
        View scrim = findViewById(R.id.status_scrim);
        scrim.post(() -> {
            WindowInsets insets = scrim.getRootWindowInsets();
            if (insets == null) return;
            int height = Build.VERSION.SDK_INT >= 30
                    ? insets.getInsets(WindowInsets.Type.statusBars()).top
                    : insets.getSystemWindowInsetTop();
            if (height > 0) {
                android.view.ViewGroup.LayoutParams params = scrim.getLayoutParams();
                params.height = height;
                scrim.setLayoutParams(params);
            }
        });
    }

    private void setupInputs() {
        watch("name", nameEdit);
        watch("passport", passportEdit);
        watch("phone", phoneEdit);
        watch("email", emailEdit);
        watch("guests", guestsEdit);
        watch("wishes", wishesEdit);
        roomEdit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, BookingValidator.ROOM_CATEGORIES));
        roomEdit.setOnItemClickListener((parent, view, position, id) -> onEdited("room"));
        roomEdit.setOnFocusChangeListener((view, focused) -> { if (!focused) showError("room"); });
        checkInEdit.setOnClickListener(view -> pickDates());
        checkOutEdit.setOnClickListener(view -> pickDates());
        findViewById(R.id.check_button).setOnClickListener(view -> validateForm(true));
        submitButton.setOnClickListener(view -> confirmBooking());
    }

    private void watch(String key, TextInputEditText edit) {
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onEdited(key); }
            @Override public void afterTextChanged(Editable s) { }
        });
        edit.setOnFocusChangeListener((view, focused) -> { if (!focused) showError(key); });
    }

    private void onEdited(String key) {
        if (restoring) return;
        saveDraft();
        refreshValidation();
        if (touched.contains(key)) showError(key);
    }

    private static String value(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString();
    }

    private BookingDraft capture() {
        BookingDraft draft = new BookingDraft();
        draft.fullName = value(nameEdit);
        draft.passport = value(passportEdit);
        draft.phone = value(phoneEdit);
        draft.email = value(emailEdit);
        draft.checkIn = checkIn;
        draft.checkOut = checkOut;
        draft.guests = value(guestsEdit);
        draft.room = roomEdit.getText() == null ? "" : roomEdit.getText().toString();
        draft.wishes = value(wishesEdit);
        return draft;
    }

    private void refreshValidation() {
        BookingDraft draft = capture();
        submitButton.setEnabled(BookingValidator.validate(draft, BookingDraft.todayUtcMidnight()).isEmpty());
        staySummary.setText(draft.nights() > 0
                ? draft.nights() + " " + nightsWord(draft.nights()) + " · " + shortDate(checkIn) + " — " + shortDate(checkOut)
                : "Выберите даты заезда и выезда");
    }

    private boolean validateForm(boolean showErrors) {
        Map<String, String> errors = BookingValidator.validate(capture(), BookingDraft.todayUtcMidnight());
        if (showErrors) {
            for (String key : FIELDS) {
                touched.add(key);
                layouts.get(key).setError(errors.get(key));
            }
            for (String key : FIELDS) {
                if (errors.containsKey(key)) {
                    TextInputLayout target = layouts.get(key);
                    Rect rect = new Rect();
                    target.getDrawingRect(rect);
                    scroll.offsetDescendantRectToMyCoords(target, rect);
                    scroll.smoothScrollTo(0, Math.max(0, rect.top - 24));
                    target.animate().alpha(.55f).setDuration(90).withEndAction(() -> target.animate().alpha(1f).setDuration(180).start()).start();
                    break;
                }
            }
        }
        submitButton.setEnabled(errors.isEmpty());
        return errors.isEmpty();
    }

    private void showError(String key) {
        touched.add(key);
        layouts.get(key).setError(BookingValidator.validate(capture(), BookingDraft.todayUtcMidnight()).get(key));
    }

    private void saveDraft() {
        BookingDraft draft = capture();
        prefs.edit()
                .putString("name", draft.fullName)
                .putString("passport", draft.passport)
                .putString("phone", draft.phone)
                .putString("email", draft.email)
                .putLong("checkIn", draft.checkIn == null ? -1 : draft.checkIn)
                .putLong("checkOut", draft.checkOut == null ? -1 : draft.checkOut)
                .putString("guests", draft.guests)
                .putString("room", draft.room)
                .putString("wishes", draft.wishes)
                .apply();
    }

    private void restoreDraft() {
        restoring = true;
        nameEdit.setText(prefs.getString("name", ""));
        passportEdit.setText(prefs.getString("passport", ""));
        phoneEdit.setText(prefs.getString("phone", ""));
        emailEdit.setText(prefs.getString("email", ""));
        guestsEdit.setText(prefs.getString("guests", ""));
        wishesEdit.setText(prefs.getString("wishes", ""));
        roomEdit.setText(prefs.getString("room", ""), false);
        long in = prefs.getLong("checkIn", -1);
        long out = prefs.getLong("checkOut", -1);
        checkIn = in < 0 ? null : in;
        checkOut = out < 0 ? null : out;
        updateDateFields();
        restoring = false;
    }

    private void updateDateFields() {
        checkInEdit.setText(checkIn == null ? "" : date(checkIn, "dd MMMM yyyy"));
        checkOutEdit.setText(checkOut == null ? "" : date(checkOut, "dd MMMM yyyy"));
        refreshValidation();
    }

    private void pickDates() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now()).build();
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Выберите даты проживания");
        builder.setCalendarConstraints(constraints);
        if (checkIn != null && checkOut != null) builder.setSelection(new Pair<>(checkIn, checkOut));
        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null || selection.first == null || selection.second == null) return;
            checkIn = selection.first;
            checkOut = selection.second;
            updateDateFields();
            saveDraft();
            if (touched.contains("checkIn")) showError("checkIn");
            if (touched.contains("checkOut")) showError("checkOut");
        });
        picker.show(getSupportFragmentManager(), "stay_dates");
    }

    private void confirmBooking() {
        if (!validateForm(true)) return;
        BookingDraft draft = capture();
        String wishes = draft.wishes.trim().isEmpty() ? "" : "\nПожелания: " + draft.wishes.trim();
        String summary = draft.fullName.trim() + "\n" + shortDate(draft.checkIn) + " — " + shortDate(draft.checkOut)
                + " · " + draft.nights() + " " + nightsWord(draft.nights()) + "\n"
                + draft.room + " · " + draft.guests + " гост.\nПаспорт: •••• "
                + draft.passport.trim().substring(6) + "\nКонтакт: " + draft.phone.trim() + ", " + draft.email.trim() + wishes;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Проверьте заявку")
                .setMessage(summary)
                .setNegativeButton("Изменить", (dialog, which) -> { })
                .setPositiveButton("Подтвердить", (dialog, which) -> completeBooking(draft))
                .show();
    }

    private void completeBooking(BookingDraft draft) {
        prefs.edit().clear().apply();
        restoring = true;
        for (TextInputEditText edit : Arrays.asList(nameEdit, passportEdit, phoneEdit, emailEdit, guestsEdit, wishesEdit)) edit.setText("");
        roomEdit.setText("", false);
        checkIn = checkOut = null;
        updateDateFields();
        restoring = false;
        touched.clear();
        for (TextInputLayout layout : layouts.values()) layout.setError(null);
        refreshValidation();
        scroll.smoothScrollTo(0, 0);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Заявка подготовлена")
                .setMessage("Демо-заявка подтверждена на этом устройстве.")
                .setPositiveButton("Понятно", (dialog, which) -> { })
                .show();
    }

    private static String date(long millis, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, new Locale("ru", "RU"));
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(millis));
    }

    private static String shortDate(long millis) { return date(millis, "dd.MM.yyyy"); }

    private static String nightsWord(int count) {
        if (count % 10 == 1 && count % 100 != 11) return "ночь";
        if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 12 || count % 100 > 14)) return "ночи";
        return "ночей";
    }
}
