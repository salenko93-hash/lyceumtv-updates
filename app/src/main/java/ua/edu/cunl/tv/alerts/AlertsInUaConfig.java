package ua.edu.cunl.tv.alerts;

public final class AlertsInUaConfig {
    private AlertsInUaConfig() {}

    public static final String UID = "81";
    public static final String ENDPOINT =
            "https://api.alerts.in.ua/v1/iot/active_air_raid_alerts/" + UID + ".json";
    public static final int CONNECT_TIMEOUT_MS = 10_000;
    public static final int READ_TIMEOUT_MS = 10_000;
    public static final long POLL_INTERVAL_MS = 15_000L;
}
