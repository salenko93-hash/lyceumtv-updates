package ua.edu.cunl.tv.alerts;

import android.content.Context;
import android.content.SharedPreferences;

public final class AlertsInUaStore {
    private static final String PREFS = "alerts_in_ua_monitor_v1";
    private static final String KEY_UID = "last_uid";
    private static final String KEY_CONFIRMED = "confirmed_state";
    private static final String KEY_LAST_RAW = "last_raw";
    private static final String KEY_LAST_HTTP = "last_http";

    private final SharedPreferences prefs;

    public AlertsInUaStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String cachedUid = prefs.getString(KEY_UID, null);
        if (!AlertsInUaConfig.UID.equals(cachedUid)) {
            prefs.edit().clear().putString(KEY_UID, AlertsInUaConfig.UID).apply();
        }
    }

    public String getConfirmedState() {
        return prefs.getString(KEY_CONFIRMED, "UNKNOWN");
    }

    public void saveSuccess(String state, String raw, int httpCode) {
        prefs.edit()
                .putString(KEY_UID, AlertsInUaConfig.UID)
                .putString(KEY_CONFIRMED, state)
                .putString(KEY_LAST_RAW, raw)
                .putInt(KEY_LAST_HTTP, httpCode)
                .apply();
    }

    public void saveHttpOnly(int httpCode) {
        prefs.edit().putInt(KEY_LAST_HTTP, httpCode).apply();
    }
}
