package ua.edu.cunl.tv.util;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Kyiv local time with compatibility for older Android/TV tzdata.
 *
 * Newer tzdata uses Europe/Kyiv. Older Android releases know the same
 * civil-time rules under the historical IANA alias Europe/Kiev.
 */
public final class KyivTime {
    private static final ZoneId ZONE = resolveZone();

    private KyivTime() {}

    private static ZoneId resolveZone() {
        try {
            return ZoneId.of("Europe/Kyiv");
        } catch (DateTimeException unsupportedKyiv) {
            try {
                return ZoneId.of("Europe/Kiev");
            } catch (DateTimeException unsupportedKiev) {
                // Last-resort only. Every supported production TV seen so far
                // has Europe/Kiev, but this prevents class initialization crash.
                return ZoneId.systemDefault();
            }
        }
    }

    public static ZoneId zone() {
        return ZONE;
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now(ZONE);
    }

    public static String displayName() {
        return "Europe/Kyiv";
    }
}
