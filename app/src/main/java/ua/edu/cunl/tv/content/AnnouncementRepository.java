package ua.edu.cunl.tv.content;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnnouncementRepository {
    private final Context context;

    public AnnouncementRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<String> activeAnnouncements() {
        JSONObject root = readOptionalContent();
        if (root == null) return Collections.emptyList();

        JSONArray announcements = root.optJSONArray("announcements");
        if (announcements == null || announcements.length() == 0) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<>();
        for (int i = 0; i < announcements.length(); i++) {
            Object raw = announcements.opt(i);
            if (raw instanceof String) {
                String value = ((String) raw).trim();
                if (!value.isEmpty()) out.add(value);
                continue;
            }

            JSONObject a = announcements.optJSONObject(i);
            if (a == null) continue;
            if (a.has("active") && !a.optBoolean("active", false)) continue;

            String title = firstNonEmpty(
                    a.optString("title", ""),
                    a.optString("heading", ""),
                    a.optString("name", ""));
            String message = firstNonEmpty(
                    a.optString("message", ""),
                    a.optString("text", ""),
                    a.optString("body", ""),
                    a.optString("content", ""));

            String combined;
            if (!title.isEmpty() && !message.isEmpty() && !title.equals(message)) {
                combined = title + "\n" + message;
            } else {
                combined = !message.isEmpty() ? message : title;
            }
            combined = combined.trim();
            if (!combined.isEmpty()) out.add(combined);
        }
        return out;
    }

    public String announcementForCycle(long cycle) {
        List<String> all = activeAnnouncements();
        if (all.isEmpty()) return "";
        int index = (int) Math.floorMod(cycle, all.size());
        return all.get(index);
    }

    private JSONObject readOptionalContent() {
        try {
            File remote = new File(new File(new File(context.getFilesDir(), "remote"), "current"),
                    "content.json");
            if (remote.isFile()) {
                try (InputStream in = new FileInputStream(remote)) {
                    return new JSONObject(readAll(in));
                } catch (Exception ignored) {
                    // Fall back to the packaged content.json.
                }
            }
            try (InputStream in = context.getAssets().open("content.json")) {
                return new JSONObject(readAll(in));
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private static String readAll(InputStream in) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) >= 0) {
                if (n > 0) out.write(buffer, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
