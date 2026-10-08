package ua.edu.cunl.tv.alerts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ua.edu.cunl.tv.security.SecureTokenStore;

public final class AlertsInUaMonitor {
    private static final String TAG = "LyceumAlertsApi";
    public interface Listener {
        void onConfirmedState(String state);
        void onMonitorMessage(String message);
    }

    private final Context context;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private ScheduledExecutorService executor;

    public AlertsInUaMonitor(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public synchronized void start() {
        if (executor != null) return;
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::pollSafe, 0,
                AlertsInUaConfig.POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public void pollNow() {
        ScheduledExecutorService ex;
        synchronized (this) { ex = executor; }
        if (ex != null) ex.execute(this::pollSafe);
    }

    private void pollSafe() {
        Log.i(TAG, "poll start uid=" + AlertsInUaConfig.UID);
        String token = new SecureTokenStore(context).load();
        if (token.isEmpty()) {
            postMessage("API token не налаштовано");
            return;
        }

        String uid = AlertsInUaConfig.UID;
        String endpoint = AlertsInUaConfig.ENDPOINT;
        AlertsInUaStore store = new AlertsInUaStore(context);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setConnectTimeout(AlertsInUaConfig.CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(AlertsInUaConfig.READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            store.saveHttpOnly(code);
            if (code < 200 || code >= 300) {
                postMessage("alerts.in.ua HTTP " + code + "; стан не змінено");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    conn.getInputStream(), StandardCharsets.UTF_8))) {
                String raw = reader.readLine();
                if (raw == null) raw = "";
                raw = raw.trim();
                String state = raw;
                if (state.length() >= 2 && state.startsWith("\"") && state.endsWith("\"")) {
                    state = state.substring(1, state.length() - 1);
                }

                final String normalized;
                if ("A".equals(state) || "P".equals(state)) normalized = "ACTIVE";
                else if ("N".equals(state)) normalized = "NONE";
                else {
                    postMessage("Невідома відповідь API: " + raw + "; стан не змінено");
                    return;
                }

                store.saveSuccess(normalized, raw, code);
                Log.i(TAG, "confirmed state=" + normalized + " http=" + code);
                postState(normalized);
                postMessage("UID " + uid + " • " + state + " • HTTP " + code);
            }
        } catch (Exception e) {
            Log.w(TAG, "poll failed; confirmed state preserved", e);
            postMessage("Помилка API; підтверджений стан збережено: " + e.getClass().getSimpleName());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void postState(String state) {
        main.post(() -> listener.onConfirmedState(state));
    }

    private void postMessage(String message) {
        main.post(() -> listener.onMonitorMessage(message));
    }
}
