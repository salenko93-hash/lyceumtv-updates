package ua.edu.cunl.tv.admin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;

import ua.edu.cunl.tv.R;
import ua.edu.cunl.tv.alerts.AlertsInUaConfig;
import ua.edu.cunl.tv.security.SecureTokenStore;

public final class AdminDialog {
    public interface Listener {
        void onSettingsSaved();
        void onTestSilence();
        void onSyncNow();
        void onManualAlarm();
        void onManualAllClear();
        void onPollAlerts();
    }

    public static final String PREFS = "admin_settings_v1";
    // Kept for migration from 2.7.0.7 and older.
    public static final String KEY_REFERENCE_MONDAY = "reference_numerator_monday";
    public static final String KEY_NUMERATOR_START_DATE = "numerator_start_date";
    public static final String DEFAULT_NUMERATOR_START_DATE = "2026-09-01";
    public static final String KEY_BREAK_ANNOUNCEMENTS_ENABLED = "break_announcements_enabled";
    public static final String KEY_SILENCE_ENABLED = "minute_silence_enabled";
    public static final String KEY_SILENCE_VOLUME = "minute_silence_volume_percent";
    public static final String KEY_ALARM_SOUND_ENABLED = "alarm_sound_enabled";
    public static final String KEY_ALARM_VOLUME = "alarm_sound_volume_percent";
    public static final String KEY_MANIFEST_URL = "manifest_url";
    public static final String KEY_SHEETS_URL = "sheets_url";
    public static final String KEY_SCREEN_PROFILE = "screen_profile";

    public static final String DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/salenko93-hash/lyceumtv-updates/main/sample-server/content_manifest.json";
    public static final String DEFAULT_SHEETS_URL =
            "https://script.google.com/macros/s/AKfycbwoo2SlQh3ojGRycG9EmUMhAqOgIhMlN9lCw6BzVDGlihVWAYonx1YTmUE8ap7EDKgSEA/exec";

    private static final int BG = Color.rgb(13, 18, 27);
    private static final int CARD = Color.rgb(23, 31, 44);
    private static final int BORDER = Color.rgb(52, 78, 108);
    private static final int BLUE = Color.rgb(57, 157, 255);
    private static final int RED = Color.rgb(205, 37, 52);
    private static final int GREEN = Color.rgb(30, 156, 91);

    private AdminDialog() {}

    public static AlertDialog show(Context context, Listener listener,
                                   boolean alarmActive, String monitorStatus,
                                   String confirmedState) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        float density = context.getResources().getDisplayMetrics().density;
        int pad = (int) (22 * density);
        int small = (int) (12 * density);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(BG);

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.lyceum_logo_white);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (96 * density));
        logoParams.bottomMargin = small;
        root.addView(logo, logoParams);

        TextView title = new TextView(context);
        title.setText("LYCEUMTV  •  РОЗКЛАД ЛІЦЕЮ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, matchWrap());

        TextView institution = new TextView(context);
        institution.setText("Центральноукраїнський науковий ліцей Кіровоградської обласної ради");
        institution.setTextColor(Color.rgb(175, 194, 214));
        institution.setTextSize(15);
        institution.setPadding(0, 3, 0, small);
        root.addView(institution, matchWrap());

        TextView runtime = new TextView(context);
        String runtimeText = alarmActive
                ? "● ПОВІТРЯНА ТРИВОГА АКТИВНА  •  Адмінка доступна  •  " + safe(monitorStatus)
                : "● ЗВИЧАЙНИЙ РЕЖИМ  •  " + safe(monitorStatus);
        runtime.setText(runtimeText);
        runtime.setTextColor(Color.WHITE);
        runtime.setTextSize(17);
        runtime.setPadding(small, small, small, small);
        runtime.setBackground(rounded(alarmActive ? RED : GREEN, alarmActive ? RED : GREEN, 14));
        root.addView(runtime, matchWrap());

        addSection(root, "ТРИВОГА ТА API", small);

        TextView fixed = info(context,
                "ФІКСОВАНИЙ UID " + AlertsInUaConfig.UID
                        + "  •  Кропивницький район  •  підтверджений стан: "
                        + safe(confirmedState));
        root.addView(fixed, matchWrap());

        SecureTokenStore tokenStore = new SecureTokenStore(context);
        TextView tokenState = info(context,
                tokenStore.load().isEmpty()
                        ? "API token: НЕ НАЛАШТОВАНО"
                        : "API token: НАЛАШТОВАНО (значення приховано)");
        tokenState.setTextColor(tokenStore.load().isEmpty()
                ? Color.rgb(255, 190, 90) : Color.rgb(120, 230, 170));
        root.addView(tokenState, matchWrap());

        EditText token = input(context,
                "alerts.in.ua API token — залиште порожнім, щоб не змінювати");
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(token, matchWrap());

        LinearLayout alertButtons = horizontal(context);
        Button poll = actionButton(context, "ОНОВИТИ API", BLUE);
        poll.setOnClickListener(v -> listener.onPollAlerts());
        alertButtons.addView(poll, weighted());

        Button alarm = actionButton(context, "РУЧНА ТРИВОГА", RED);
        alarm.setOnClickListener(v -> listener.onManualAlarm());
        alertButtons.addView(alarm, weighted());

        Button clear = actionButton(context, "РУЧНИЙ ВІДБІЙ", GREEN);
        clear.setOnClickListener(v -> listener.onManualAllClear());
        alertButtons.addView(clear, weighted());
        root.addView(alertButtons, matchWrap());

        TextView safety = info(context,
                "Адмінка залишається доступною під час тривоги. "
                        + "Ручний відбій не приховує підтверджену alerts.in.ua тривогу.");
        safety.setTextColor(Color.rgb(255, 210, 160));
        root.addView(safety, matchWrap());

        CheckBox alarmSoundEnabled = new CheckBox(context);
        alarmSoundEnabled.setText("TRIVOGA.mp3 — відтворити один раз на початку тривоги");
        alarmSoundEnabled.setTextColor(Color.WHITE);
        alarmSoundEnabled.setTextSize(17);
        alarmSoundEnabled.setChecked(prefs.getBoolean(KEY_ALARM_SOUND_ENABLED, true));
        root.addView(alarmSoundEnabled, matchWrap());

        TextView alarmVolumeLabel = info(context, "");
        int savedAlarmVolume = prefs.getInt(KEY_ALARM_VOLUME, 85);
        alarmVolumeLabel.setText("Гучність сигналу тривоги: " + savedAlarmVolume + "%");
        root.addView(alarmVolumeLabel, matchWrap());

        SeekBar alarmVolume = new SeekBar(context);
        alarmVolume.setMax(100);
        alarmVolume.setProgress(savedAlarmVolume);
        alarmVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                alarmVolumeLabel.setText("Гучність сигналу тривоги: " + progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(alarmVolume, matchWrap());

        addSection(root, "НАВЧАЛЬНИЙ ТИЖДЕНЬ", small);

        EditText startDate = input(context, "Старт чисельника, YYYY-MM-DD");
        startDate.setInputType(InputType.TYPE_CLASS_DATETIME);
        String savedStart = prefs.getString(KEY_NUMERATOR_START_DATE, "").trim();
        if (savedStart.isEmpty()) {
            String legacy = prefs.getString(KEY_REFERENCE_MONDAY, "").trim();
            savedStart = legacy.isEmpty() ? DEFAULT_NUMERATOR_START_DATE : legacy;
        }
        startDate.setText(savedStart);
        root.addView(startDate, matchWrap());

        TextView startInfo = info(context,
                "Поточне правило: 01.09.2026 — чисельник; далі тижні чергуються. "
                        + "Дата може бути будь-яким днем тижня.");
        root.addView(startInfo, matchWrap());

        addSection(root, "СИНХРОНІЗАЦІЯ ТА ДЖЕРЕЛА", small);

        EditText manifest = input(context, "Manifest URL (GitHub/NAS)");
        manifest.setText(nonBlank(prefs.getString(KEY_MANIFEST_URL, ""), DEFAULT_MANIFEST_URL));
        root.addView(manifest, matchWrap());

        EditText sheets = input(context, "Google Apps Script Web App URL");
        sheets.setText(nonBlank(prefs.getString(KEY_SHEETS_URL, ""), DEFAULT_SHEETS_URL));
        root.addView(sheets, matchWrap());

        TextView syncInfo = info(context,
                "Ручна синхронізація з адмінки дозволена навіть під час тривоги. "
                        + "SHA-256, схема JSON і атомарна заміна файлів залишаються обов'язковими.");
        root.addView(syncInfo, matchWrap());

        addSection(root, "ПОГОДА", small);

        Button weather = actionButton(context, "ПОГОДА • КРОПИВНИЦЬКИЙ", Color.rgb(35, 118, 193));
        weather.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=weather+Kropyvnytskyi"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "Не вдалося відкрити сторінку погоди",
                        Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(weather, matchWrap());

        TextView weatherInfo = info(context,
                "Погода Кропивницького автоматично показується на звичайному екрані, "
                        + "під час перерви та в режимі укриття; оновлення виконується у фоні.");
        root.addView(weatherInfo, matchWrap());

        addSection(root, "ЕКРАН", small);

        Spinner profile = new Spinner(context);
        String[] profiles = {"AUTO", "FHD", "HD", "OTHER"};
        ArrayAdapter<String> profileAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_item, profiles);
        profileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profile.setAdapter(profileAdapter);
        String current = prefs.getString(KEY_SCREEN_PROFILE, "AUTO");
        for (int i = 0; i < profiles.length; i++) {
            if (profiles[i].equals(current)) profile.setSelection(i);
        }
        profile.setPadding(small, small, small, small);
        profile.setBackground(rounded(CARD, BORDER, 12));
        root.addView(profile, matchWrap());

        addSection(root, "ОГОЛОШЕННЯ НА ПЕРЕРВАХ", small);

        CheckBox breakAnnouncements = new CheckBox(context);
        breakAnnouncements.setText("Показувати активні оголошення під час перерв");
        breakAnnouncements.setTextColor(Color.WHITE);
        breakAnnouncements.setTextSize(17);
        breakAnnouncements.setChecked(
                prefs.getBoolean(KEY_BREAK_ANNOUNCEMENTS_ENABLED, true));
        root.addView(breakAnnouncements, matchWrap());

        TextView announcementInfo = info(context,
                "Під час реальної перерви: 30 с розклад → 15 с оголошення. "
                        + "Остання 1 хвилина перед уроком завжди показує розклад. "
                        + "Використовуються активні записи announcements з останньої синхронізації.");
        root.addView(announcementInfo, matchWrap());

        addSection(root, "ХВИЛИНА МОВЧАННЯ", small);

        CheckBox silenceEnabled = new CheckBox(context);
        silenceEnabled.setText("Фонограма хвилини мовчання — увімкнено");
        silenceEnabled.setTextColor(Color.WHITE);
        silenceEnabled.setTextSize(17);
        silenceEnabled.setChecked(prefs.getBoolean(KEY_SILENCE_ENABLED, true));
        root.addView(silenceEnabled, matchWrap());

        TextView volumeLabel = info(context, "");
        int savedVolume = prefs.getInt(KEY_SILENCE_VOLUME, 25);
        volumeLabel.setText("Гучність фонограми: " + savedVolume + "%");
        root.addView(volumeLabel, matchWrap());

        SeekBar volume = new SeekBar(context);
        volume.setMax(100);
        volume.setProgress(savedVolume);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeLabel.setText("Гучність фонограми: " + progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(volume, matchWrap());

        Button testSilence = actionButton(context,
                "ТЕСТ ХВИЛИНИ МОВЧАННЯ • ПОВНА ФОНОГРАМА", BLUE);
        testSilence.setOnClickListener(v -> listener.onTestSilence());
        root.addView(testSilence, matchWrap());

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(scroll)
                .setPositiveButton("ЗБЕРЕГТИ", null)
                .setNeutralButton("СИНХРОНІЗУВАТИ", null)
                .setNegativeButton("ЗАКРИТИ", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(BG));
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90f);
                lp.height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.90f);
                window.setAttributes(lp);
            }

            styleDialogButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), BLUE);
            styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEUTRAL), Color.rgb(101, 82, 190));
            styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), Color.rgb(70, 82, 98));

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    String start = startDate.getText().toString().trim();
                    if (start.isEmpty()) start = DEFAULT_NUMERATOR_START_DATE;
                    LocalDate.parse(start);

                    String t = token.getText().toString().trim();
                    if (!t.isEmpty()) new SecureTokenStore(context).save(t);

                    prefs.edit()
                            .putString(KEY_NUMERATOR_START_DATE, start)
                            .putString(KEY_MANIFEST_URL, manifest.getText().toString().trim())
                            .putString(KEY_SHEETS_URL, sheets.getText().toString().trim())
                            .putString(KEY_SCREEN_PROFILE, profile.getSelectedItem().toString())
                            .putBoolean(KEY_BREAK_ANNOUNCEMENTS_ENABLED,
                                    breakAnnouncements.isChecked())
                            .putBoolean(KEY_SILENCE_ENABLED, silenceEnabled.isChecked())
                            .putInt(KEY_SILENCE_VOLUME, volume.getProgress())
                            .putBoolean(KEY_ALARM_SOUND_ENABLED, alarmSoundEnabled.isChecked())
                            .putInt(KEY_ALARM_VOLUME, alarmVolume.getProgress())
                            .apply();

                    listener.onSettingsSaved();
                    Toast.makeText(context, "Налаштування збережено", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } catch (Exception e) {
                    Toast.makeText(context,
                            "Перевірте дату старту чисельника: YYYY-MM-DD",
                            Toast.LENGTH_LONG).show();
                }
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(v -> listener.onSyncNow());
        });

        dialog.show();
        return dialog;
    }

    private static void addSection(LinearLayout root, String text, int pad) {
        TextView t = new TextView(root.getContext());
        t.setText(text);
        t.setTextColor(Color.rgb(111, 188, 255));
        t.setTextSize(15);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, pad * 2, 0, pad / 2);
        root.addView(t, matchWrap());
    }

    private static TextView info(Context context, String text) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextColor(Color.rgb(190, 204, 220));
        t.setTextSize(15);
        t.setPadding(6, 8, 6, 8);
        return t;
    }

    private static EditText input(Context context, String hint) {
        EditText e = new EditText(context);
        e.setHint(hint);
        e.setHintTextColor(Color.rgb(120, 141, 165));
        e.setTextColor(Color.WHITE);
        e.setTextSize(17);
        e.setSingleLine(true);
        int p = (int) (12 * context.getResources().getDisplayMetrics().density);
        e.setPadding(p, p, p, p);
        e.setBackground(rounded(CARD, BORDER, 12));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, 6, 0, 6);
        e.setLayoutParams(lp);
        return e;
    }

    private static Button actionButton(Context context, String text, int baseColor) {
        Button b = new Button(context);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setPadding(10, 10, 10, 10);
        b.setBackground(rounded(baseColor, lighten(baseColor), 12));
        b.setOnFocusChangeListener((v, hasFocus) -> {
            int c = hasFocus ? lighten(baseColor) : baseColor;
            v.setBackground(rounded(c, Color.WHITE, 12));
            v.animate().scaleX(hasFocus ? 1.03f : 1f)
                    .scaleY(hasFocus ? 1.03f : 1f)
                    .setDuration(180L).start();
        });
        return b;
    }

    private static void styleDialogButton(Button b, int color) {
        if (b == null) return;
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setBackground(rounded(color, lighten(color), 10));
        b.setOnFocusChangeListener((v, hasFocus) -> {
            int c = hasFocus ? lighten(color) : color;
            v.setBackground(rounded(c, Color.WHITE, 10));
        });
    }

    private static LinearLayout horizontal(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);
        return row;
    }

    private static LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(4, 0, 4, 0);
        return lp;
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static GradientDrawable rounded(int fill, int border, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radiusDp * 3f);
        d.setStroke(1, border);
        return d;
    }

    private static int lighten(int color) {
        int r = Math.min(255, Color.red(color) + 36);
        int g = Math.min(255, Color.green(color) + 36);
        int b = Math.min(255, Color.blue(color) + 36);
        return Color.rgb(r, g, b);
    }

    private static String nonBlank(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        return v.isEmpty() ? fallback : v;
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }
}
