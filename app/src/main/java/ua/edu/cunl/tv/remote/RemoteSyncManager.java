package ua.edu.cunl.tv.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

public final class RemoteSyncManager {
    public interface Listener {
        void onSyncResult(boolean ok, String message);
    }

    private static final String[] REQUIRED = {
            "schedule_numerator.json",
            "schedule_denominator.json",
            "shelter_numerator.json",
            "shelter_denominator.json"
    };

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public RemoteSyncManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void sync(String manifestUrl, BooleanSupplier alarmActive, Listener listener) {
        sync(manifestUrl, "", alarmActive, false, listener);
    }

    public void sync(String manifestUrl, String sheetsUrl,
                     BooleanSupplier alarmActive, Listener listener) {
        sync(manifestUrl, sheetsUrl, alarmActive, false, listener);
    }

    public void sync(String manifestUrl, String sheetsUrl,
                     BooleanSupplier alarmActive, boolean allowDuringAlarm,
                     Listener listener) {
        if (manifestUrl == null || manifestUrl.trim().isEmpty()) {
            listener.onSyncResult(false, "Manifest URL не налаштовано");
            return;
        }
        if (!allowDuringAlarm && alarmActive.getAsBoolean()) {
            listener.onSyncResult(false, "Синхронізацію відкладено: активна тривога");
            return;
        }
        executor.execute(() -> {
            try {
                validateUrlPolicy(manifestUrl);
                byte[] manifestBytes = download(manifestUrl);
                JSONObject manifest = new JSONObject(
                        new String(manifestBytes, java.nio.charset.StandardCharsets.UTF_8));
                JSONObject schedules = manifest.getJSONObject("schedules");

                File base = new File(context.getFilesDir(), "remote");
                File stage = new File(base, "stage");
                deleteRecursive(stage);
                if (!stage.mkdirs() && !stage.isDirectory()) {
                    throw new IllegalStateException("Cannot create staging directory");
                }

                URI baseUri = URI.create(manifestUrl).resolve(".");
                for (String name : REQUIRED) {
                    JSONObject item = schedules.getJSONObject(name);
                    byte[] data = downloadAndVerify(baseUri, item, name);
                    validateScheduleJson(name, data);
                    write(new File(stage, name), data);
                }

                if (manifest.has("calendar")) {
                    byte[] data = downloadAndVerify(baseUri,
                            manifest.getJSONObject("calendar"), "calendar.json");
                    validateCalendarJson(data);
                    write(new File(stage, "calendar.json"), data);
                }

                if (manifest.has("content")) {
                    byte[] data = downloadAndVerify(baseUri,
                            manifest.getJSONObject("content"), "content.json");
                    validateContentJson(data);
                    write(new File(stage, "content.json"), data);
                }

                // In GitHub + Google Sheets mode a configured HTTPS Sheets Web App
                // is authoritative for announcements/substitutions. It intentionally
                // is not SHA-bound to the GitHub manifest, but is schema-validated.
                if (sheetsUrl != null && !sheetsUrl.trim().isEmpty()) {
                    validateUrlPolicy(sheetsUrl.trim());
                    byte[] data = download(sheetsUrl.trim());
                    validateContentJson(data);
                    write(new File(stage, "content.json"), data);
                }

                // Automatic/background callers still defer during AIR_RAID.
                // An explicit administrator sync can opt in to committing during AIR_RAID;
                // schema/SHA validation and the atomic directory swap remain mandatory.
                if (!allowDuringAlarm && alarmActive.getAsBoolean()) {
                    deleteRecursive(stage);
                    post(listener, false,
                            "Синхронізацію відкладено: тривога активувалась до commit");
                    return;
                }

                File current = new File(base, "current");
                File previous = new File(base, "previous");
                deleteRecursive(previous);
                if (current.exists() && !current.renameTo(previous)) {
                    throw new IllegalStateException("Cannot rotate current bundle");
                }
                if (!stage.renameTo(current)) {
                    if (previous.exists()) previous.renameTo(current);
                    throw new IllegalStateException("Cannot commit downloaded bundle");
                }

                post(listener, true,
                        "4 розклади синхронізовано; SHA-256 і схема перевірені");
            } catch (Exception e) {
                post(listener, false,
                        "Синхронізація не виконана: " + safeMessage(e));
            } finally {
                executor.shutdown();
            }
        });
    }

    private static void validateScheduleJson(String name, byte[] data) throws Exception {
        JSONObject root = new JSONObject(
                new String(data, java.nio.charset.StandardCharsets.UTF_8));
        if (root.optBoolean("placeholder", false)) {
            throw new IllegalArgumentException(name + " is a placeholder");
        }
        JSONArray bells = root.optJSONArray("bellSchedule");
        JSONObject days = root.optJSONObject("days");
        if (bells == null || bells.length() < 1 || days == null) {
            throw new IllegalArgumentException(name + " has invalid schema");
        }
        String[] requiredDays = {
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
        };
        for (String day : requiredDays) {
            if (days.optJSONObject(day) == null) {
                throw new IllegalArgumentException(name + " missing day " + day);
            }
        }
    }

    private static void validateCalendarJson(byte[] data) throws Exception {
        JSONObject root = new JSONObject(
                new String(data, java.nio.charset.StandardCharsets.UTF_8));
        if (!root.has("daysOff") || !root.has("ranges")) {
            throw new IllegalArgumentException("calendar.json has invalid schema");
        }
    }

    private static void validateContentJson(byte[] data) throws Exception {
        JSONObject root = new JSONObject(
                new String(data, java.nio.charset.StandardCharsets.UTF_8));
        if (!root.has("announcements") && !root.has("substitutions")
                && !root.has("events") && !root.has("schedule")) {
            throw new IllegalArgumentException("content feed has no supported fields");
        }
    }

    private static byte[] downloadAndVerify(URI baseUri, JSONObject item,
                                            String logicalName) throws Exception {
        String relative = item.getString("url");
        String expected = item.getString("sha256").toLowerCase(Locale.ROOT);
        if (!expected.matches("[0-9a-f]{64}")) {
            throw new SecurityException(logicalName + " invalid SHA-256 field");
        }
        String resolved = baseUri.resolve(relative).toString();
        validateUrlPolicy(resolved);
        byte[] data = download(resolved);
        String actual = sha256(data);
        if (!actual.equals(expected)) {
            throw new SecurityException(logicalName + " SHA-256 mismatch");
        }
        return data;
    }

    private static byte[] download(String initialUrl) throws Exception {
        String current = initialUrl;
        for (int redirect = 0; redirect <= 3; redirect++) {
            validateUrlPolicy(current);
            HttpURLConnection c = (HttpURLConnection) new URL(current).openConnection();
            c.setConnectTimeout(12_000);
            c.setReadTimeout(20_000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("Accept", "application/json, application/octet-stream");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = c.getHeaderField("Location");
                c.disconnect();
                if (location == null || location.trim().isEmpty()) {
                    throw new IllegalStateException("Redirect without Location");
                }
                current = URI.create(current).resolve(location).toString();
                continue;
            }
            if (code < 200 || code >= 300) {
                c.disconnect();
                throw new IllegalStateException("HTTP " + code);
            }
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 java.io.InputStream in = c.getInputStream()) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) >= 0) {
                    if (n > 0) out.write(buffer, 0, n);
                }
                return out.toByteArray();
            } finally {
                c.disconnect();
            }
        }
        throw new IllegalStateException("Too many redirects");
    }

    private static void validateUrlPolicy(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme() == null
                ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if ("https".equals(scheme)) return;
        if (!"http".equals(scheme)) {
            throw new SecurityException("Only HTTPS or private-LAN HTTP is allowed");
        }

        String host = uri.getHost();
        if (host == null || host.equalsIgnoreCase("localhost")) {
            throw new SecurityException("Invalid HTTP host");
        }
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            throw new SecurityException(
                    "Plain HTTP requires a numeric private IPv4 host");
        }
        int[] octets = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                if (parts[i].isEmpty()) throw new NumberFormatException();
                octets[i] = Integer.parseInt(parts[i]);
                if (octets[i] < 0 || octets[i] > 255) {
                    throw new NumberFormatException();
                }
            }
        } catch (NumberFormatException e) {
            throw new SecurityException(
                    "Plain HTTP requires a numeric private IPv4 host");
        }
        boolean privateV4 =
                octets[0] == 10
                        || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
                        || (octets[0] == 192 && octets[1] == 168);
        if (!privateV4) {
            throw new SecurityException(
                    "Plain HTTP is allowed only for private IPv4 LAN hosts");
        }
    }

    private static String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte v : hash) {
            sb.append(String.format(Locale.ROOT, "%02x", v));
        }
        return sb.toString();
    }

    private static void write(File f, byte[] data) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }
    }

    private static void deleteRecursive(File f) {
        if (!f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    private static String safeMessage(Exception e) {
        String m = e.getMessage();
        if (m == null || m.trim().isEmpty()) return e.getClass().getSimpleName();
        return m.length() > 160 ? m.substring(0, 160) : m;
    }

    private void post(Listener l, boolean ok, String msg) {
        main.post(() -> l.onSyncResult(ok, msg));
    }
}
