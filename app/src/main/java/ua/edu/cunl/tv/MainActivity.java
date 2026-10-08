package ua.edu.cunl.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import ua.edu.cunl.tv.admin.AdminDialog;
import ua.edu.cunl.tv.alerts.AlertsInUaMonitor;
import ua.edu.cunl.tv.alerts.AlertsInUaStore;
import ua.edu.cunl.tv.audio.AlarmSoundPlayer;
import ua.edu.cunl.tv.audio.MinuteSilencePlayer;
import ua.edu.cunl.tv.content.AnnouncementRepository;
import ua.edu.cunl.tv.remote.RemoteSyncManager;
import ua.edu.cunl.tv.schedule.ScheduleRepository;
import ua.edu.cunl.tv.schedule.ScheduleSnapshot;
import ua.edu.cunl.tv.security.SecureTokenStore;
import ua.edu.cunl.tv.ui.SignageView;
import ua.edu.cunl.tv.util.KyivTime;
import ua.edu.cunl.tv.weather.WeatherRepository;

public final class MainActivity extends Activity
        implements AlertsInUaMonitor.Listener, AdminDialog.Listener {

    private static final long ALL_CLEAR_MS = 15_000L;
    private static final long LONG_PRESS_MS = 900L;
    private static final String PENDING_TOKEN_FILE = "pending_alerts_token.txt";
    private static final int MAX_PENDING_TOKEN_BYTES = 4096;
    private static final String TOKEN_SETUP_TAG = "LyceumTokenSetup";
    private static final long WEATHER_REFRESH_MS = 10L * 60L * 1000L;
    private static final long WEATHER_RETRY_MS = 2L * 60L * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SignageView view;
    private AlertsInUaMonitor monitor;
    private ScheduleRepository schedules;
    private AnnouncementRepository announcements;
    private WeatherRepository weather;
    private long nextWeatherRefreshAt;
    private boolean weatherHasData;
    private final MinuteSilencePlayer silencePlayer = new MinuteSilencePlayer();
    private final AlarmSoundPlayer alarmPlayer = new AlarmSoundPlayer();

    private String confirmedState = "UNKNOWN";
    private String monitorStatus = "UID 81 • запуск";
    private boolean manualAlarm = false;
    private long allClearUntil = 0L;
    private ZonedDateTime alertStartedAt;
    private LocalDate silenceDate;
    private boolean silenceActive;
    private boolean missingAudio;
    private boolean alarmAudioPlayed;
    private long keyDownAt;
    private boolean adminHoldArmed;
    private boolean adminHoldTriggered;
    private AlertDialog adminDialog;

    private final Runnable openAdminAfterHold = new Runnable() {
        @Override public void run() {
            if (!adminHoldArmed || adminHoldTriggered) return;
            adminHoldTriggered = true;
            showAdminSafely();
        }
    };

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            updateTimedState();
            refreshWeatherIfDue(false);
            if (view != null) view.invalidate();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        boolean tokenImportedFromPc = importPendingTokenFromPc();
        ensureDefaultPreferences();

        view = new SignageView(this);
        setContentView(view);
        view.requestFocus();
        if (tokenImportedFromPc) {
            Toast.makeText(this, "alerts.in.ua API token імпортовано з ПК", Toast.LENGTH_LONG).show();
        }

        SharedPreferences prefs = prefs();
        view.setScreenProfile(prefs.getString(AdminDialog.KEY_SCREEN_PROFILE, "AUTO"));
        view.setClockStatus(KyivTime.displayName() + " • UID 81");

        schedules = new ScheduleRepository(this);
        announcements = new AnnouncementRepository(this);
        weather = new WeatherRepository();
        view.setWeatherUnavailable("Оновлення…");
        confirmedState = new AlertsInUaStore(this).getConfirmedState();
        if ("ACTIVE".equals(confirmedState)) {
            alertStartedAt = KyivTime.now();
        }
        monitor = new AlertsInUaMonitor(this, this);

        updateProductionDataFlag();
        render();
    }


    /**
     * Debug-deployment helper used by SET_ALERTS_TOKEN.ps1.
     * The token is handed off through the app-private sandbox, encrypted into
     * SecureTokenStore on startup, then the plaintext hand-off file is deleted.
     */
    private boolean importPendingTokenFromPc() {
        File pending = new File(getFilesDir(), PENDING_TOKEN_FILE);
        if (!pending.exists()) return false;

        try {
            long length = pending.length();
            if (length <= 0 || length > MAX_PENDING_TOKEN_BYTES) {
                Log.w(TOKEN_SETUP_TAG, "Rejected pending token file length=" + length);
                return false;
            }

            byte[] data = new byte[(int) length];
            int offset = 0;
            try (FileInputStream in = new FileInputStream(pending)) {
                while (offset < data.length) {
                    int n = in.read(data, offset, data.length - offset);
                    if (n < 0) break;
                    offset += n;
                }
            }
            if (offset <= 0) return false;

            String token = new String(data, 0, offset, StandardCharsets.UTF_8).trim();
            if (token.isEmpty()) return false;

            new SecureTokenStore(this).save(token);
            Log.i(TOKEN_SETUP_TAG, "Pending ADB token imported into secure store.");
            return true;
        } catch (Exception e) {
            Log.e(TOKEN_SETUP_TAG, "Unable to import pending ADB token.", e);
            return false;
        } finally {
            if (pending.exists() && !pending.delete()) {
                Log.w(TOKEN_SETUP_TAG, "Unable to delete pending token hand-off file.");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        monitor.start();
        refreshWeatherIfDue(true);
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        monitor.stop();
        silencePlayer.stop();
        alarmPlayer.stop();
        silenceActive = false;
        handler.removeCallbacks(tick);
        handler.removeCallbacks(openAdminAfterHold);
        adminHoldArmed = false;
    }

    @Override
    protected void onDestroy() {
        silencePlayer.stop();
        alarmPlayer.stop();
        if (monitor != null) monitor.stop();
        if (weather != null) weather.shutdown();
        super.onDestroy();
    }

    @Override
    public void onConfirmedState(String state) {
        boolean wasAlarmActive = isAlarmActive();
        String old = confirmedState;
        confirmedState = state;
        boolean nowAlarmActive = isAlarmActive();

        if ("ACTIVE".equals(state)) {
            if (!"ACTIVE".equals(old) || alertStartedAt == null) {
                alertStartedAt = KyivTime.now();
            }
            allClearUntil = 0L;
            silencePlayer.stop();
            silenceActive = false;
            if (!wasAlarmActive && nowAlarmActive) {
                alarmAudioPlayed = false;
            }
            // Premium Broadcast keeps the admin dialog accessible.
        } else if ("ACTIVE".equals(old) && "NONE".equals(state) && !manualAlarm) {
            allClearUntil = System.currentTimeMillis() + ALL_CLEAR_MS;
        }

        if (wasAlarmActive && !nowAlarmActive) {
            alarmPlayer.stop();
            alarmAudioPlayed = false;
        }
        render();
    }

    @Override
    public void onMonitorMessage(String message) {
        monitorStatus = message == null ? "" : message;
        if (monitorStatus.contains("Помилка") || monitorStatus.contains("не налаштовано")
                || monitorStatus.contains("HTTP 4") || monitorStatus.contains("HTTP 5")) {
            Toast.makeText(this, monitorStatus, Toast.LENGTH_SHORT).show();
        }
        view.setClockStatus(KyivTime.displayName() + " • " + shortStatus(monitorStatus));
        render();
    }

    private void updateTimedState() {
        if (isAlarmActive()) {
            if (silenceActive) {
                silencePlayer.stop();
                silenceActive = false;
            }
            render();
            return;
        }

        ZonedDateTime now = KyivTime.now();
        LocalDate today = now.toLocalDate();
        boolean inStartWindow = now.getHour() == 9 && now.getMinute() == 0;

        if (inStartWindow && !today.equals(silenceDate) && !silenceActive
                && prefs().getBoolean(AdminDialog.KEY_SILENCE_ENABLED, true)) {
            silenceDate = today;
            startSilence();
        }
        render();
    }

    private void refreshWeatherIfDue(boolean force) {
        if (weather == null) return;
        long nowMs = System.currentTimeMillis();
        if (!force && nowMs < nextWeatherRefreshAt) return;

        // Reserve the next slot before starting I/O so the 1-second UI tick
        // cannot launch duplicate requests while a slow network call is running.
        nextWeatherRefreshAt = nowMs + WEATHER_REFRESH_MS;
        weather.refresh(new WeatherRepository.Listener() {
            @Override
            public void onWeather(WeatherRepository.Weather data) {
                if (view == null || data == null) return;
                view.setWeather(data);
                weatherHasData = true;
                nextWeatherRefreshAt = System.currentTimeMillis() + WEATHER_REFRESH_MS;
            }

            @Override
            public void onWeatherError(String message) {
                if (view != null && !weatherHasData) {
                    view.setWeatherUnavailable("Погода тимчасово недоступна");
                }
                // Retry sooner after a connectivity failure.
                nextWeatherRefreshAt = System.currentTimeMillis() + WEATHER_RETRY_MS;
            }
        });
    }

    private void startSilence() {
        silenceActive = true;
        missingAudio = false;
        float volume = prefs().getInt(AdminDialog.KEY_SILENCE_VOLUME, 25) / 100f;
        boolean started = silencePlayer.start(this, volume, new MinuteSilencePlayer.Listener() {
            @Override public void onCompleted() {
                silenceActive = false;
                render();
            }

            @Override public void onMissingAudio() {
                missingAudio = true;
                handler.postDelayed(() -> {
                    if (silenceActive && !silencePlayer.isPlaying()) {
                        silenceActive = false;
                        render();
                    }
                }, MinuteSilencePlayer.DOCUMENTED_DURATION_MS);
                render();
            }
        });
        if (!started) render();
    }

    private void startAlarmAudioIfNeeded() {
        if (alarmAudioPlayed) return;
        alarmAudioPlayed = true;
        if (!prefs().getBoolean(AdminDialog.KEY_ALARM_SOUND_ENABLED, true)) return;
        float volume = prefs().getInt(AdminDialog.KEY_ALARM_VOLUME, 85) / 100f;
        alarmPlayer.start(this, volume);
    }

    private void render() {
        if (view == null || schedules == null) return;

        ZonedDateTime now = KyivTime.now();
        if (isAlarmActive()) {
            if (alertStartedAt == null) alertStartedAt = now;
            startAlarmAudioIfNeeded();
            ScheduleSnapshot snapshot = schedules.snapshot(now, true, prefs());
            view.setScheduleSnapshot(snapshot);
            view.setBreakAnnouncement("", false);
            String source = manualAlarm
                    ? "РУЧНА ТРИВОГА"
                    : "alerts.in.ua • UID 81";
            String started = alertStartedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            view.setMode("AIR_RAID",
                    source + " • початок " + started + " • адмінка доступна: OK 1с");
            return;
        }

        if (allClearUntil > System.currentTimeMillis()) {
            long remainingMs = allClearUntil - System.currentTimeMillis();
            long seconds = Math.max(1L, (remainingMs + 999L) / 1000L);
            view.clearScheduleSnapshot();
            view.setBreakAnnouncement("", false);
            view.setMode("ALL_CLEAR",
                    "Повернення до звичайного розкладу через " + seconds + " с");
            return;
        }

        alertStartedAt = null;

        if (silenceActive) {
            view.clearScheduleSnapshot();
            view.setBreakAnnouncement("", false);
            // The memorial page is intentionally clean: no playback/diagnostic captions.
            view.setMode("SILENCE", "");
            return;
        }

        ScheduleSnapshot snapshot = schedules.snapshot(now, false, prefs());
        view.setScheduleSnapshot(snapshot);

        boolean announcementsEnabled = prefs().getBoolean(
                AdminDialog.KEY_BREAK_ANNOUNCEMENTS_ENABLED, true);
        boolean announcementWindow = snapshot.breakTime
                && snapshot.secondsToNextLesson > 60L
                && (snapshot.secondsIntoBreak % 45L) >= 30L;
        String announcement = "";
        if (announcementsEnabled && announcementWindow && announcements != null) {
            long cycle = snapshot.secondsIntoBreak / 45L;
            announcement = announcements.announcementForCycle(cycle);
        }
        view.setBreakAnnouncement(announcement, !announcement.isEmpty());

        String status = "NONE".equals(confirmedState)
                ? "UID 81 • тривоги немає"
                : "UID 81 • очікування підтвердження API";
        view.setMode("NORMAL", status);
    }

    private boolean isAdminKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER;
    }

    private void showAdminSafely() {
        if (adminDialog != null && adminDialog.isShowing()) return;
        adminDialog = AdminDialog.show(this, this, isAlarmActive(), monitorStatus, confirmedState);
        adminDialog.setOnDismissListener(ignored -> adminDialog = null);
    }

    private void activateManualAlarm() {
        boolean wasAlarmActive = isAlarmActive();
        if (!manualAlarm) alertStartedAt = KyivTime.now();
        manualAlarm = true;
        allClearUntil = 0L;
        silencePlayer.stop();
        silenceActive = false;
        if (!wasAlarmActive) alarmAudioPlayed = false;
        render();
    }

    private void requestManualAllClear() {
        manualAlarm = false;
        if (!"ACTIVE".equals(confirmedState)) {
            alarmPlayer.stop();
            alarmAudioPlayed = false;
            allClearUntil = System.currentTimeMillis() + ALL_CLEAR_MS;
            render();
        } else {
            Toast.makeText(this,
                    "alerts.in.ua досі підтверджує активну тривогу. "
                            + "Ручний режим вимкнено, але червоний екран залишається.",
                    Toast.LENGTH_LONG).show();
            render();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isAdminKey(keyCode)) {
            if (event.getRepeatCount() == 0) {
                keyDownAt = System.currentTimeMillis();
                adminHoldArmed = true;
                adminHoldTriggered = false;
                handler.removeCallbacks(openAdminAfterHold);
                handler.postDelayed(openAdminAfterHold, LONG_PRESS_MS);
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            if (event.getRepeatCount() == 0) showAdminSafely();
            return true;
        }

        if (event.getRepeatCount() == 0) keyDownAt = System.currentTimeMillis();
        return keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isAdminKey(keyCode)) {
            adminHoldArmed = false;
            handler.removeCallbacks(openAdminAfterHold);

            long held = System.currentTimeMillis() - keyDownAt;
            if (!adminHoldTriggered && held >= LONG_PRESS_MS) {
                adminHoldTriggered = true;
                showAdminSafely();
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            return true;
        }

        long held = System.currentTimeMillis() - keyDownAt;
        if (held >= LONG_PRESS_MS) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                activateManualAlarm();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                requestManualAllClear();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onSettingsSaved() {
        SharedPreferences prefs = prefs();
        view.setScreenProfile(prefs.getString(AdminDialog.KEY_SCREEN_PROFILE, "AUTO"));
        monitor.pollNow();
        updateProductionDataFlag();
        render();
    }

    @Override
    public void onTestSilence() {
        if (isAlarmActive()) {
            Toast.makeText(this,
                    "Налаштування доступні, але відтворення фонограми під час реальної тривоги заблоковано.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!prefs().getBoolean(AdminDialog.KEY_SILENCE_ENABLED, true)) {
            Toast.makeText(this, "Увімкніть фонограму в налаштуваннях",
                    Toast.LENGTH_LONG).show();
            return;
        }
        silencePlayer.stop();
        startSilence();
        render();
    }

    @Override
    public void onSyncNow() {
        String url = prefs().getString(AdminDialog.KEY_MANIFEST_URL, AdminDialog.DEFAULT_MANIFEST_URL);
        // This is an explicit administrator action. It is allowed during AIR_RAID
        // in 2.7.0.8, while validation and atomic commit remain enforced.
        new RemoteSyncManager(this).sync(url, prefs().getString(AdminDialog.KEY_SHEETS_URL, AdminDialog.DEFAULT_SHEETS_URL),
                this::isAlarmActive, true, (ok, msg) -> {
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    updateProductionDataFlag();
                    render();
                });
    }

    @Override
    public void onManualAlarm() {
        activateManualAlarm();
        Toast.makeText(this, "Ручну тривогу увімкнено", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onManualAllClear() {
        requestManualAllClear();
    }

    @Override
    public void onPollAlerts() {
        monitor.pollNow();
        Toast.makeText(this, "Оновлюю стан alerts.in.ua…", Toast.LENGTH_SHORT).show();
    }

    private boolean isAlarmActive() {
        return manualAlarm || "ACTIVE".equals(confirmedState);
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(AdminDialog.PREFS, Context.MODE_PRIVATE);
    }

    private void ensureDefaultPreferences() {
        SharedPreferences p = prefs();
        SharedPreferences.Editor editor = p.edit();
        boolean changed = false;

        String start = p.getString(AdminDialog.KEY_NUMERATOR_START_DATE, "").trim();
        if (start.isEmpty()) {
            String legacy = p.getString(AdminDialog.KEY_REFERENCE_MONDAY, "").trim();
            editor.putString(AdminDialog.KEY_NUMERATOR_START_DATE,
                    legacy.isEmpty() ? AdminDialog.DEFAULT_NUMERATOR_START_DATE : legacy);
            changed = true;
        }

        // Defaults are written only when a field is empty/missing. Existing admin
        // values survive APK upgrades because package id and preferences name stay unchanged.
        if (p.getString(AdminDialog.KEY_MANIFEST_URL, "").trim().isEmpty()) {
            editor.putString(AdminDialog.KEY_MANIFEST_URL, AdminDialog.DEFAULT_MANIFEST_URL);
            changed = true;
        }
        if (p.getString(AdminDialog.KEY_SHEETS_URL, "").trim().isEmpty()) {
            editor.putString(AdminDialog.KEY_SHEETS_URL, AdminDialog.DEFAULT_SHEETS_URL);
            changed = true;
        }
        if (!p.contains(AdminDialog.KEY_SCREEN_PROFILE)) {
            editor.putString(AdminDialog.KEY_SCREEN_PROFILE, "AUTO");
            changed = true;
        }
        if (!p.contains(AdminDialog.KEY_BREAK_ANNOUNCEMENTS_ENABLED)) {
            editor.putBoolean(AdminDialog.KEY_BREAK_ANNOUNCEMENTS_ENABLED, true);
            changed = true;
        }
        if (!p.contains(AdminDialog.KEY_SILENCE_ENABLED)) {
            editor.putBoolean(AdminDialog.KEY_SILENCE_ENABLED, true);
            changed = true;
        }
        if (!p.contains(AdminDialog.KEY_SILENCE_VOLUME)) {
            editor.putInt(AdminDialog.KEY_SILENCE_VOLUME, 25);
            changed = true;
        }
        if (!p.contains(AdminDialog.KEY_ALARM_SOUND_ENABLED)) {
            editor.putBoolean(AdminDialog.KEY_ALARM_SOUND_ENABLED, true);
            changed = true;
        }
        if (!p.contains(AdminDialog.KEY_ALARM_VOLUME)) {
            editor.putInt(AdminDialog.KEY_ALARM_VOLUME, 85);
            changed = true;
        }

        if (changed) editor.apply();
    }

    private void updateProductionDataFlag() {
        boolean schedulesOk = true;
        String[] names = {
                "schedule_numerator.json", "schedule_denominator.json",
                "shelter_numerator.json", "shelter_denominator.json"
        };
        for (String name : names) {
            try {
                String text;
                try (InputStream in = getAssets().open(name);
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) >= 0) {
                        if (n > 0) out.write(buffer, 0, n);
                    }
                    text = out.toString(java.nio.charset.StandardCharsets.UTF_8.name());
                }
                if (text.contains("\"placeholder\": true")
                        || !text.contains("\"bellSchedule\"")
                        || !text.contains("\"days\"")) schedulesOk = false;
            } catch (IOException e) {
                schedulesOk = false;
            }
        }
        int audioId = getResources().getIdentifier(
                "minute_silence", "raw", getPackageName());
        view.setProductionDataPresent(schedulesOk && audioId != 0);
    }

    private static String shortStatus(String s) {
        if (s == null || s.isEmpty()) return "UID 81";
        if (s.length() <= 54) return s;
        return s.substring(0, 53) + "…";
    }
}
