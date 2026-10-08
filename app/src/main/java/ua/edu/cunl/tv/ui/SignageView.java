package ua.edu.cunl.tv.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ua.edu.cunl.tv.R;
import ua.edu.cunl.tv.schedule.ScheduleSnapshot;
import ua.edu.cunl.tv.util.KyivTime;
import ua.edu.cunl.tv.weather.WeatherRepository;

/**
 * TV-first renderer for the final LyceumTV 2.7.0.16 schedule pages.
 *
 * The screen is intentionally drawn with Android platform Canvas APIs only:
 * no WebView, no external fonts and no third-party UI dependencies. The
 * system Roboto family is used because it remains crisp on older 720p/1080p
 * classroom TVs and is always present on Android.
 */
public final class SignageView extends View {
    private static final int NAVY = Color.rgb(8, 53, 111);
    private static final int NAVY_MUTED = Color.rgb(42, 83, 130);
    private static final int IVORY = Color.rgb(250, 248, 241);
    private static final int GRID = Color.rgb(194, 207, 218);
    private static final int ORANGE = Color.rgb(255, 112, 0);

    private static final int BLUE_EDGE = Color.rgb(5, 82, 174);
    private static final int BLUE_CENTER = Color.rgb(64, 199, 218);
    private static final int RED_EDGE = Color.rgb(169, 0, 9);
    private static final int RED_CENTER = Color.rgb(255, 35, 43);
    private static final int CLEAR_EDGE = Color.rgb(7, 90, 45);
    private static final int CLEAR_CENTER = Color.rgb(34, 163, 94);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap logo;

    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);
    private final Typeface condensed = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private final Typeface condensedBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);

    private String mode = "NORMAL";
    private String detail = "Запуск…";
    private String screenProfile = "AUTO";
    private String clockStatus = "";
    private boolean productionDataPresent;
    private ScheduleSnapshot schedule;

    private boolean showAnnouncement;
    private String announcement = "";

    private boolean weatherAvailable;
    private double weatherTemperatureC;
    private double weatherApparentC;
    private int weatherCode = -1;
    private String weatherCondition = "Оновлення…";
    private List<WeatherRepository.ForecastDay> forecast = Collections.emptyList();

    public SignageView(Context context) {
        super(context);
        logo = BitmapFactory.decodeResource(getResources(), R.drawable.lyceum_logo_white);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void setMode(String mode, String detail) {
        String next = mode == null ? "NORMAL" : mode;
        boolean changed = !next.equals(this.mode);
        this.mode = next;
        this.detail = detail == null ? "" : detail;
        if (changed) {
            setAlpha(0.90f);
            animate().alpha(1f).setDuration(180L).start();
        }
        invalidate();
    }

    public void setScheduleSnapshot(ScheduleSnapshot snapshot) {
        schedule = snapshot;
        invalidate();
    }

    public void clearScheduleSnapshot() {
        schedule = null;
        invalidate();
    }

    public void setBreakAnnouncement(String text, boolean visible) {
        announcement = text == null ? "" : text.trim();
        showAnnouncement = visible && !announcement.isEmpty();
        invalidate();
    }

    /**
     * Weather setter for 2.7.0.16. Current conditions are rendered in the compact TV card.
     */
    public void setWeather(WeatherRepository.Weather data) {
        if (data == null) {
            setWeatherUnavailable("Немає даних");
            return;
        }
        weatherAvailable = true;
        weatherTemperatureC = data.temperatureC;
        weatherApparentC = data.apparentC;
        weatherCode = data.weatherCode;
        weatherCondition = data.conditionText();
        forecast = data.forecast == null ? Collections.emptyList() : data.forecast;
        invalidate();
    }

    /**
     * Compatibility setter retained for older callers/tools.
     */
    public void setWeather(String temperature, String condition, String apparent) {
        weatherAvailable = true;
        weatherCondition = condition == null ? "" : condition.trim();
        weatherTemperatureC = parseSignedDegrees(temperature);
        weatherApparentC = parseSignedDegrees(apparent);
        weatherCode = -1;
        forecast = Collections.emptyList();
        invalidate();
    }

    public void setWeatherUnavailable(String message) {
        weatherAvailable = false;
        weatherCondition = message == null || message.trim().isEmpty()
                ? "Немає даних" : message.trim();
        forecast = Collections.emptyList();
        invalidate();
    }

    public void setProductionDataPresent(boolean present) {
        productionDataPresent = present;
        invalidate();
    }

    public void setScreenProfile(String profile) {
        String p = profile == null ? "AUTO" : profile.trim().toUpperCase(Locale.ROOT);
        if (!p.equals("AUTO") && !p.equals("FHD") && !p.equals("HD") && !p.equals("OTHER")) {
            p = "AUTO";
        }
        screenProfile = p;
        invalidate();
    }

    public void setClockStatus(String status) {
        clockStatus = status == null ? "" : status;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0f || h <= 0f) return;

        float pad = safePadding(w);
        drawBackground(canvas, w, h);

        if ("ALL_CLEAR".equals(mode)) {
            drawAllClear(canvas, w, h, pad);
        } else if ("SILENCE".equals(mode)) {
            drawSilence(canvas, w, h, pad);
        } else if (("NORMAL".equals(mode) || "AIR_RAID".equals(mode)) && schedule != null) {
            drawApprovedSchedule(canvas, w, h, pad, "AIR_RAID".equals(mode));
        } else {
            drawFallback(canvas, w, h, pad);
        }

        // TV pages stay presentation-clean. Diagnostics remain available in the admin dialog.
    }

    private void drawBackground(Canvas canvas, float w, float h) {
        if ("SILENCE".equals(mode)) {
            paint.setShader(new LinearGradient(0f, 0f, 0f, h,
                    Color.rgb(25, 52, 98), Color.rgb(6, 16, 39), Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, w, h, paint);
            paint.setShader(null);
            paint.setShader(new RadialGradient(
                    w * 0.50f, h * 0.53f, Math.max(w, h) * 0.43f,
                    Color.argb(28, 106, 155, 226), Color.TRANSPARENT,
                    Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, w, h, paint);
            paint.setShader(null);
            return;
        }

        int edge = BLUE_EDGE;
        int center = BLUE_CENTER;
        if ("AIR_RAID".equals(mode)) {
            edge = RED_EDGE;
            center = RED_CENTER;
        } else if ("ALL_CLEAR".equals(mode)) {
            edge = CLEAR_EDGE;
            center = CLEAR_CENTER;
        }

        paint.setShader(new LinearGradient(
                0f, 0f, w, 0f,
                new int[]{edge, center, edge},
                new float[]{0f, 0.50f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, w, h, paint);
        paint.setShader(null);

        // Gentle center glow gives the mockup-like luminous middle without
        // reducing contrast behind the schedule cards.
        paint.setShader(new RadialGradient(
                w * 0.50f, h * 0.52f, Math.max(w, h) * 0.47f,
                Color.argb(24, 255, 255, 255), Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, w, h, paint);
        paint.setShader(null);

        drawBackgroundArcs(canvas, w, h);
    }

    private void drawBackgroundArcs(Canvas canvas, float w, float h) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1.3f, h * 0.0017f));
        stroke.setColor(Color.argb(54, 255, 255, 255));

        float r = h * 0.22f;
        canvas.drawCircle(w * 0.10f, h * 0.08f, r, stroke);
        canvas.drawCircle(w * 0.10f, h * 0.08f, r * 0.74f, stroke);
        canvas.drawCircle(w * 0.90f, h * 0.08f, r, stroke);
        canvas.drawCircle(w * 0.90f, h * 0.08f, r * 0.74f, stroke);
    }

    private void drawApprovedSchedule(Canvas canvas, float w, float h, float pad, boolean raid) {
        ZonedDateTime now = KyivTime.now();

        // Final 16:9 composition: two stable schedule columns and one center information axis.
        // Geometry does not move when NORMAL/BREAK/AIR_RAID states switch.
        float sideW = w * 0.292f;
        float leftX = pad;
        float rightX = w - pad - sideW;

        drawSideCards(canvas, leftX, sideW, rightX, sideW, h, raid);
        drawCenterHeader(canvas, w, h, now, raid);

        if (raid) {
            drawRaidCenter(canvas, w, h, now);
        } else if (schedule.breakTime) {
            drawBreakCenter(canvas, w, h, now);
        } else {
            drawNormalCenter(canvas, w, h, now);
        }
    }

    private void drawCenterHeader(Canvas canvas, float w, float h,
                                  ZonedDateTime now, boolean raid) {
        float logoTop = h * 0.025f;
        float logoH = h * 0.145f;
        if (logo != null && logo.getWidth() > 0 && logo.getHeight() > 0) {
            float logoW = logoH * ((float) logo.getWidth() / (float) logo.getHeight());
            RectF dest = new RectF(w * 0.50f - logoW / 2f, logoTop,
                    w * 0.50f + logoW / 2f, logoTop + logoH);
            paint.setColor(Color.argb(30, 0, 0, 0));
            canvas.drawOval(new RectF(dest.left - 9f, dest.top + 7f,
                    dest.right + 9f, dest.bottom + 13f), paint);
            paint.setAlpha(255);
            canvas.drawBitmap(logo, null, dest, paint);
        }

        setText(bold, Color.WHITE, h * 0.049f);
        drawCentered(canvas, raid ? "РОЗКЛАД В УКРИТТІ" : "РОЗКЛАД УРОКІВ",
                w * 0.50f, h * 0.247f);

        String week = weekLabel(schedule.subtitle);
        RectF pill = new RectF(w * 0.389f, h * 0.270f, w * 0.611f, h * 0.336f);
        paint.setColor(ORANGE);
        canvas.drawRoundRect(pill, h * 0.018f, h * 0.018f, paint);

        setText(bold, Color.WHITE, h * 0.041f);
        drawCenteredVertically(canvas, week, pill.centerX(), pill);

        String day = now.format(DateTimeFormatter.ofPattern("EEEE", new Locale("uk", "UA")))
                .toUpperCase(new Locale("uk", "UA"));
        String date = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        setText(condensedBold, Color.WHITE, h * 0.043f);
        drawCentered(canvas, day, w * 0.50f, h * 0.407f);

        setText(condensed, Color.argb(244, 255, 255, 255), h * 0.026f);
        drawCentered(canvas, date, w * 0.50f, h * 0.449f);
    }

    private void drawSideCards(Canvas canvas, float leftX, float leftW,
                               float rightX, float rightW, float h, boolean raid) {
        int total = Math.min(8, schedule.rows.size());
        float top = h * 0.116f;
        float bottom = h * 0.922f;
        float gap = h * 0.020f;
        float cardH = (bottom - top - gap * 3f) / 4f;

        for (int i = 0; i < total; i++) {
            boolean right = i >= 4;
            int rowIndex = right ? i - 4 : i;
            float x = right ? rightX : leftX;
            float cardW = right ? rightW : leftW;
            float y = top + rowIndex * (cardH + gap);
            RectF card = new RectF(x, y, x + cardW, y + cardH);
            drawScheduleCard(canvas, card, schedule.rows.get(i), raid);
        }
    }

    private void drawScheduleCard(Canvas canvas, RectF card, ScheduleSnapshot.Row row, boolean raid) {
        float radius = card.height() * 0.085f;

        // Very light shadow keeps cards separated from both blue and red backgrounds.
        paint.setColor(Color.argb(28, 0, 35, 75));
        canvas.drawRoundRect(new RectF(card.left + 3f, card.top + 5f,
                card.right + 3f, card.bottom + 5f), radius, radius, paint);

        paint.setColor(IVORY);
        canvas.drawRoundRect(card, radius, radius, paint);

        int accent = raid ? Color.rgb(214, 28, 43) : Color.rgb(48, 111, 179);
        int headerBg = raid ? Color.rgb(255, 241, 242) : Color.rgb(239, 247, 252);

        float classW = card.width() * 0.215f;
        RectF classCell = new RectF(card.left, card.top, card.left + classW, card.bottom);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, card.height() * 0.005f));
        stroke.setColor(GRID);
        canvas.drawLine(classCell.right, card.top, classCell.right, card.bottom, stroke);

        setText(bold, NAVY, card.height() * 0.205f);
        drawCentered(canvas, row.className, classCell.centerX(),
                classCell.centerY() - card.height() * 0.035f);

        setText(medium, accent, card.height() * 0.084f);
        String lessonLabel = schedule.lesson > 0 ? schedule.lesson + " УРОК" : "УРОК";
        drawCentered(canvas, lessonLabel, classCell.centerX(),
                classCell.centerY() + card.height() * 0.190f);

        RectF table = new RectF(classCell.right, card.top, card.right, card.bottom);
        float headerH = card.height() * 0.225f;
        RectF header = new RectF(table.left, table.top, table.right, table.top + headerH);
        paint.setColor(headerBg);
        canvas.drawRect(header, paint);

        float subjectW = table.width() * 0.515f;
        float roomW = table.width() * 0.175f;
        float subjectRight = table.left + subjectW;
        float roomRight = subjectRight + roomW;

        canvas.drawLine(subjectRight, card.top, subjectRight, card.bottom, stroke);
        canvas.drawLine(roomRight, card.top, roomRight, card.bottom, stroke);
        canvas.drawLine(table.left, header.bottom, table.right, header.bottom, stroke);

        setText(medium, accent, card.height() * 0.063f);
        drawCentered(canvas, "ПРЕДМЕТ", (table.left + subjectRight) / 2f,
                header.centerY() - (paint.ascent() + paint.descent()) / 2f);
        drawCentered(canvas, "КАБ.", (subjectRight + roomRight) / 2f,
                header.centerY() - (paint.ascent() + paint.descent()) / 2f);
        drawCentered(canvas, "ВИКЛАДАЧ", (roomRight + table.right) / 2f,
                header.centerY() - (paint.ascent() + paint.descent()) / 2f);

        if (row.entries.isEmpty()) {
            setText(medium, NAVY, card.height() * 0.18f);
            drawCentered(canvas, "—", (table.left + table.right) / 2f,
                    header.bottom + (card.bottom - header.bottom) * 0.58f);
            return;
        }

        int count = Math.min(2, row.entries.size());
        float dataTop = header.bottom;
        float dataH = card.bottom - dataTop;
        float rowH = dataH / count;
        if (count == 2) {
            canvas.drawLine(table.left, dataTop + rowH, table.right, dataTop + rowH, stroke);
        }

        for (int i = 0; i < count; i++) {
            RectF entryBox = new RectF(table.left, dataTop + i * rowH,
                    table.right, dataTop + (i + 1) * rowH);
            drawEntryRow(canvas, entryBox, row.entries.get(i), subjectRight, roomRight,
                    card.height(), raid);
        }

        if (row.entries.size() > 2) {
            setText(medium, accent, Math.max(10f, card.height() * 0.055f));
            String extra = "+" + (row.entries.size() - 2);
            canvas.drawText(extra, card.right - paint.measureText(extra) - 7f,
                    card.bottom - 5f, paint);
        }
    }

    private void drawEntryRow(Canvas canvas, RectF box, ScheduleSnapshot.Entry entry,
                              float subjectRight, float roomRight, float cardHeight,
                              boolean raid) {
        RectF subject = new RectF(box.left, box.top, subjectRight, box.bottom);
        RectF room = new RectF(subjectRight, box.top, roomRight, box.bottom);
        RectF teacher = new RectF(roomRight, box.top, box.right, box.bottom);

        String subjectText = entry.subject == null || entry.subject.trim().isEmpty()
                ? "—" : entry.subject.trim();
        String roomText = entry.room == null || entry.room.trim().isEmpty()
                ? "—" : entry.room.trim();
        String teacherText = entry.teacher == null || entry.teacher.trim().isEmpty()
                ? "—" : entry.teacher.trim();

        int accent = raid ? Color.rgb(214, 28, 43) : Color.rgb(48, 111, 179);

        // Fixed sizes are based on card height, not text length. This keeps all
        // subject names visually equal, with wrapping instead of random shrinking.
        float subjectSize = Math.max(15f, cardHeight * 0.105f);
        float teacherSize = Math.max(12f, cardHeight * 0.078f);
        float roomSize = Math.max(16f, cardHeight * 0.118f);
        float groupSize = Math.max(10f, cardHeight * 0.066f);

        RectF subjectTextBox = inset(subject, box.width() * 0.025f, box.height() * 0.08f);
        String subgroup = entry.subgroup == null ? "" : entry.subgroup.trim();
        if (!subgroup.isEmpty()) {
            float badgeW = Math.min(subject.width() * 0.27f, cardHeight * 0.33f);
            RectF badge = new RectF(subject.left + box.width() * 0.022f,
                    box.centerY() - cardHeight * 0.075f,
                    subject.left + box.width() * 0.022f + badgeW,
                    box.centerY() + cardHeight * 0.075f);
            paint.setColor(raid ? Color.rgb(255, 226, 229) : Color.rgb(218, 238, 252));
            canvas.drawRoundRect(badge, badge.height() * 0.35f, badge.height() * 0.35f, paint);

            setText(bold, accent, groupSize);
            String badgeText = subgroup.replace("ГР. ", "ГР.").replace("ГР ", "ГР.");
            drawCenteredVertically(canvas, badgeText, badge.centerX(), badge);

            subjectTextBox.left = badge.right + box.width() * 0.018f;
        }

        setText(bold, NAVY, subjectSize);
        drawLeftCenteredMultiline(canvas, subjectText, subjectTextBox, 2);

        setText(bold, NAVY, roomSize);
        drawCenteredVertically(canvas, roomText, room.centerX(), room);

        setText(medium, NAVY, teacherSize);
        drawLeftCenteredMultiline(canvas, teacherText,
                inset(teacher, box.width() * 0.030f, box.height() * 0.08f), 2);
    }

    private void drawNormalCenter(Canvas canvas, float w, float h, ZonedDateTime now) {
        drawClock(canvas, w, h, now);

        RectF status = new RectF(w * 0.382f, h * 0.615f, w * 0.618f, h * 0.805f);
        drawStatusCard(canvas, status,
                schedule.lesson > 0 ? schedule.lesson + " УРОК" : cleanPhase(schedule.phase),
                schedule.secondsToLessonEnd > 0L ? "ДО КІНЦЯ УРОКУ" : "",
                schedule.secondsToLessonEnd > 0L
                        ? formatRemaining(schedule.secondsToLessonEnd) : "");

        drawWeatherPanel(canvas, w, h, h * 0.830f, h * 0.948f);
    }

    private void drawBreakCenter(Canvas canvas, float w, float h, ZonedDateTime now) {
        drawClock(canvas, w, h, now);

        RectF status = new RectF(w * 0.382f, h * 0.615f, w * 0.618f, h * 0.805f);
        String nextLesson = schedule.lesson > 0
                ? "ДО ПОЧАТКУ " + schedule.lesson + " УРОКУ"
                : "ДО НАСТУПНОГО УРОКУ";
        drawStatusCard(canvas, status, "ПЕРЕРВА", nextLesson,
                schedule.secondsToNextLesson > 0L
                        ? formatRemaining(schedule.secondsToNextLesson) : "");

        if (showAnnouncement && !announcement.isEmpty()) {
            RectF notice = new RectF(w * 0.365f, h * 0.812f, w * 0.635f, h * 0.862f);
            paint.setColor(Color.argb(238, 250, 248, 241));
            canvas.drawRoundRect(notice, h * 0.012f, h * 0.012f, paint);
            setText(bold, ORANGE, h * 0.0155f);
            String text = "ОГОЛОШЕННЯ • " + announcement;
            drawCentered(canvas, ellipsize(text, notice.width() * 0.92f),
                    notice.centerX(),
                    notice.centerY() - (paint.ascent() + paint.descent()) / 2f);
            drawWeatherPanel(canvas, w, h, h * 0.872f, h * 0.958f);
        } else {
            drawWeatherPanel(canvas, w, h, h * 0.830f, h * 0.948f);
        }
    }

    private void drawRaidCenter(Canvas canvas, float w, float h, ZonedDateTime now) {
        drawClock(canvas, w, h, now);

        RectF alert = new RectF(w * 0.370f, h * 0.585f, w * 0.630f, h * 0.682f);
        paint.setColor(Color.argb(246, 250, 248, 241));
        canvas.drawRoundRect(alert, h * 0.016f, h * 0.016f, paint);

        setText(bold, Color.rgb(211, 22, 38), h * 0.031f);
        drawCentered(canvas, "ПОВІТРЯНА ТРИВОГА", alert.centerX(),
                alert.top + alert.height() * 0.45f);
        setText(bold, NAVY, h * 0.0175f);
        drawCentered(canvas, "НЕГАЙНО ПРОЙДІТЬ В УКРИТТЯ", alert.centerX(),
                alert.top + alert.height() * 0.77f);

        RectF status = new RectF(w * 0.382f, h * 0.697f, w * 0.618f, h * 0.842f);
        if (schedule.breakTime) {
            String nextLesson = schedule.lesson > 0
                    ? "ДО ПОЧАТКУ " + schedule.lesson + " УРОКУ"
                    : "ДО НАСТУПНОГО УРОКУ";
            drawStatusCard(canvas, status, "ПЕРЕРВА", nextLesson,
                    schedule.secondsToNextLesson > 0L
                            ? formatRemaining(schedule.secondsToNextLesson) : "");
        } else {
            drawStatusCard(canvas, status,
                    schedule.lesson > 0 ? schedule.lesson + " УРОК" : cleanPhase(schedule.phase),
                    schedule.secondsToLessonEnd > 0L ? "ДО КІНЦЯ УРОКУ" : "",
                    schedule.secondsToLessonEnd > 0L
                            ? formatRemaining(schedule.secondsToLessonEnd) : "");
        }

        drawWeatherPanel(canvas, w, h, h * 0.855f, h * 0.958f);
    }

    private void drawClock(Canvas canvas, float w, float h, ZonedDateTime now) {
        setText(bold, Color.WHITE, h * 0.099f);
        drawCentered(canvas, now.format(DateTimeFormatter.ofPattern("HH:mm")),
                w * 0.50f, h * 0.570f);
    }

    private void drawStatusCard(Canvas canvas, RectF box,
                                String title, String subtitle, String value) {
        paint.setColor(IVORY);
        canvas.drawRoundRect(box, getHeight() * 0.018f, getHeight() * 0.018f, paint);

        setText(bold, NAVY, getHeight() * 0.038f);
        drawCentered(canvas, title == null ? "" : title,
                box.centerX(), box.top + box.height() * 0.27f);

        if (subtitle != null && !subtitle.isEmpty()) {
            setText(bold, NAVY, getHeight() * 0.020f);
            drawCentered(canvas, subtitle, box.centerX(),
                    box.top + box.height() * 0.52f);
        }

        if (value != null && !value.isEmpty()) {
            setText(bold, NAVY, getHeight() * 0.054f);
            drawCentered(canvas, value, box.centerX(),
                    box.top + box.height() * 0.88f);
        }
    }

    private void drawWeatherPanel(Canvas canvas, float w, float h,
                                  float top, float bottom) {
        RectF box = new RectF(w * 0.382f, top, w * 0.618f, bottom);

        paint.setColor(IVORY);
        canvas.drawRoundRect(box, h * 0.016f, h * 0.016f, paint);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, h * 0.0012f));
        stroke.setColor(Color.argb(38, 8, 53, 111));
        canvas.drawRoundRect(box, h * 0.016f, h * 0.016f, stroke);

        float iconS = Math.min(box.height() * 0.45f, box.width() * 0.10f);
        float iconX = box.left + box.width() * 0.115f;
        float iconY = box.centerY() + box.height() * 0.04f;
        drawWeatherIcon(canvas, weatherAvailable ? weatherCode : -1, iconX, iconY, iconS);

        float textLeft = box.left + box.width() * 0.255f;
        setText(bold, NAVY, Math.max(12f, box.height() * 0.19f));
        canvas.drawText("ПОГОДА • КРОПИВНИЦЬКИЙ", textLeft,
                box.top + box.height() * 0.39f, paint);

        String line;
        if (weatherAvailable) {
            long rounded = Math.round(weatherTemperatureC);
            String temp = String.format(Locale.ROOT, "%+d°", rounded);
            line = temp + " • " + weatherCondition.toLowerCase(new Locale("uk", "UA"));
        } else {
            line = weatherCondition;
        }

        setText(medium, NAVY, Math.max(12f, box.height() * 0.20f));
        canvas.drawText(ellipsize(line, box.right - textLeft - box.width() * 0.04f),
                textLeft, box.top + box.height() * 0.73f, paint);
    }

    private void drawForecastDays(Canvas canvas, RectF area) {
        // Open-Meteo includes today at index 0. The approved page shows the
        // following four days, so indices 1..4 are rendered when available.
        int available = Math.max(0, forecast.size() - 1);
        int count = Math.min(4, available);
        if (count <= 0) return;

        float colW = area.width() / 4f;
        for (int i = 0; i < 4; i++) {
            float left = area.left + i * colW;
            RectF col = new RectF(left, area.top, left + colW, area.bottom);
            if (i > 0) {
                stroke.setColor(Color.argb(66, 255, 255, 255));
                canvas.drawLine(col.left, col.top + 16f, col.left, col.bottom - 16f, stroke);
            }
            if (i >= count) continue;

            WeatherRepository.ForecastDay day = forecast.get(i + 1);
            String weekday = forecastWeekday(day.dateIso);
            String date = forecastDate(day.dateIso);

            setText(bold, Color.WHITE, getHeight() * 0.014f);
            drawCentered(canvas, weekday, col.centerX(), col.top + getHeight() * 0.031f);
            setText(regular, Color.argb(220, 255, 255, 255), getHeight() * 0.011f);
            drawCentered(canvas, date, col.centerX(), col.top + getHeight() * 0.052f);

            drawWeatherIcon(canvas, day.weatherCode, col.centerX(),
                    col.top + col.height() * 0.57f, getHeight() * 0.025f);

            setText(bold, Color.WHITE, getHeight() * 0.0135f);
            drawCentered(canvas, day.rangeText(), col.centerX(),
                    col.bottom - getHeight() * 0.018f);
        }
    }

    private void drawWeatherIcon(Canvas canvas, int code, float cx, float cy, float s) {
        boolean rain = code == 51 || code == 53 || code == 55 || code == 56 || code == 57
                || code == 61 || code == 63 || code == 65 || code == 66 || code == 67
                || code == 80 || code == 81 || code == 82 || code == 95
                || code == 96 || code == 99;
        boolean snow = code == 71 || code == 73 || code == 75 || code == 77
                || code == 85 || code == 86;
        boolean cloudy = code == 2 || code == 3 || rain || snow || code == 45 || code == 48;
        boolean sunny = code == 0 || code == 1 || code == 2 || code < 0;

        if (sunny) {
            paint.setColor(Color.rgb(255, 196, 0));
            canvas.drawCircle(cx - s * 0.35f, cy - s * 0.26f, s * 0.28f, paint);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Math.max(2f, s * 0.055f));
            stroke.setColor(Color.rgb(255, 196, 0));
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2.0 * i / 8.0;
                float x1 = cx - s * 0.35f + (float) Math.cos(a) * s * 0.39f;
                float y1 = cy - s * 0.26f + (float) Math.sin(a) * s * 0.39f;
                float x2 = cx - s * 0.35f + (float) Math.cos(a) * s * 0.50f;
                float y2 = cy - s * 0.26f + (float) Math.sin(a) * s * 0.50f;
                canvas.drawLine(x1, y1, x2, y2, stroke);
            }
        }

        if (cloudy) {
            paint.setColor(Color.rgb(240, 247, 252));
            canvas.drawCircle(cx - s * 0.20f, cy, s * 0.28f, paint);
            canvas.drawCircle(cx + s * 0.10f, cy - s * 0.10f, s * 0.35f, paint);
            canvas.drawCircle(cx + s * 0.42f, cy + s * 0.03f, s * 0.25f, paint);
            canvas.drawRoundRect(new RectF(cx - s * 0.48f, cy,
                    cx + s * 0.65f, cy + s * 0.30f), s * 0.12f, s * 0.12f, paint);
        }

        if (rain) {
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Math.max(2f, s * 0.07f));
            stroke.setColor(Color.rgb(188, 230, 255));
            for (int i = -1; i <= 1; i++) {
                float dx = i * s * 0.28f;
                canvas.drawLine(cx + dx, cy + s * 0.37f,
                        cx + dx - s * 0.08f, cy + s * 0.58f, stroke);
            }
        } else if (snow) {
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Math.max(1.5f, s * 0.055f));
            stroke.setColor(Color.WHITE);
            for (int i = -1; i <= 1; i++) {
                float dx = i * s * 0.28f;
                canvas.drawLine(cx + dx - s * 0.07f, cy + s * 0.43f,
                        cx + dx + s * 0.07f, cy + s * 0.55f, stroke);
                canvas.drawLine(cx + dx + s * 0.07f, cy + s * 0.43f,
                        cx + dx - s * 0.07f, cy + s * 0.55f, stroke);
            }
        }
    }

    private void drawAllClear(Canvas canvas, float w, float h, float pad) {
        drawStandaloneLogo(canvas, w, h);

        setText(bold, Color.WHITE, h * 0.060f);
        drawCentered(canvas, "ВІДБІЙ ПОВІТРЯНОЇ ТРИВОГИ", w * 0.50f, h * 0.41f);

        RectF box = new RectF(w * 0.30f, h * 0.47f, w * 0.70f, h * 0.66f);
        paint.setColor(IVORY);
        canvas.drawRoundRect(box, h * 0.022f, h * 0.022f, paint);

        setText(bold, Color.rgb(16, 105, 62), h * 0.030f);
        drawCenteredMultiline(canvas,
                "Можна повернутися до звичайного режиму\n" + detail, box, 3);

        setText(bold, Color.WHITE, h * 0.050f);
        drawCentered(canvas, KyivTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                w * 0.50f, h * 0.77f);
    }

    private void drawSilence(Canvas canvas, float w, float h, float pad) {
        drawStandaloneLogo(canvas, w, h);

        setText(bold, Color.rgb(248, 241, 224), h * 0.060f);
        drawCentered(canvas, "ХВИЛИНА МОВЧАННЯ", w * 0.50f, h * 0.395f);

        float cx = w * 0.50f;
        float candleTop = h * 0.515f;
        float candleBottom = h * 0.745f;
        float candleW = Math.min(w * 0.058f, h * 0.085f);
        float flameCy = candleTop - h * 0.060f;

        // Soft memorial glow around the flame.
        paint.setShader(new RadialGradient(
                cx, flameCy, h * 0.12f,
                new int[]{
                        Color.argb(150, 255, 177, 52),
                        Color.argb(62, 255, 139, 20),
                        Color.TRANSPARENT
                },
                new float[]{0f, 0.46f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, flameCy, h * 0.12f, paint);
        paint.setShader(null);

        // Candle body: warm ivory gradient with a subtle wax highlight.
        RectF body = new RectF(cx - candleW / 2f, candleTop,
                cx + candleW / 2f, candleBottom);
        paint.setShader(new LinearGradient(
                body.left, body.top, body.right, body.bottom,
                new int[]{
                        Color.rgb(255, 250, 232),
                        Color.rgb(246, 235, 209),
                        Color.rgb(224, 207, 178)
                },
                new float[]{0f, 0.58f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRoundRect(body, candleW * 0.17f, candleW * 0.17f, paint);
        paint.setShader(null);

        paint.setColor(Color.rgb(255, 248, 222));
        canvas.drawOval(new RectF(body.left, body.top - candleW * 0.10f,
                body.right, body.top + candleW * 0.11f), paint);

        // Small wax drip gives the candle a more natural silhouette.
        paint.setColor(Color.rgb(245, 232, 204));
        canvas.drawRoundRect(new RectF(body.right - candleW * 0.18f, body.top + candleW * 0.02f,
                body.right - candleW * 0.08f, body.top + candleW * 0.55f),
                candleW * 0.05f, candleW * 0.05f, paint);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(2f, h * 0.003f));
        stroke.setColor(Color.rgb(79, 54, 38));
        canvas.drawLine(cx, body.top - candleW * 0.03f,
                cx, body.top - candleW * 0.29f, stroke);

        Path outer = new Path();
        outer.moveTo(cx, flameCy - h * 0.070f);
        outer.cubicTo(cx + candleW * 0.40f, flameCy - h * 0.018f,
                cx + candleW * 0.30f, flameCy + h * 0.028f,
                cx, flameCy + h * 0.040f);
        outer.cubicTo(cx - candleW * 0.34f, flameCy + h * 0.020f,
                cx - candleW * 0.28f, flameCy - h * 0.020f,
                cx, flameCy - h * 0.070f);
        outer.close();

        paint.setShader(new LinearGradient(
                cx, flameCy - h * 0.070f, cx, flameCy + h * 0.040f,
                Color.rgb(255, 246, 166), Color.rgb(255, 133, 21),
                Shader.TileMode.CLAMP));
        canvas.drawPath(outer, paint);
        paint.setShader(null);

        Path inner = new Path();
        inner.moveTo(cx, flameCy - h * 0.040f);
        inner.cubicTo(cx + candleW * 0.17f, flameCy - h * 0.005f,
                cx + candleW * 0.13f, flameCy + h * 0.022f,
                cx, flameCy + h * 0.026f);
        inner.cubicTo(cx - candleW * 0.14f, flameCy + h * 0.016f,
                cx - candleW * 0.12f, flameCy - h * 0.010f,
                cx, flameCy - h * 0.040f);
        inner.close();
        paint.setColor(Color.rgb(255, 252, 220));
        canvas.drawPath(inner, paint);
    }

    private void drawFallback(Canvas canvas, float w, float h, float pad) {
        drawStandaloneLogo(canvas, w, h);
        setText(bold, Color.WHITE, h * 0.060f);
        drawCentered(canvas, titleForMode(mode), w * 0.50f, h * 0.48f);
        setText(regular, Color.WHITE, h * 0.028f);
        drawCentered(canvas, ellipsize(detail, w * 0.70f), w * 0.50f, h * 0.57f);
    }

    private void drawStandaloneLogo(Canvas canvas, float w, float h) {
        float logoH = h * 0.19f;
        if (logo != null && logo.getWidth() > 0 && logo.getHeight() > 0) {
            float logoW = logoH * ((float) logo.getWidth() / (float) logo.getHeight());
            RectF dest = new RectF(w * 0.50f - logoW / 2f, h * 0.10f,
                    w * 0.50f + logoW / 2f, h * 0.10f + logoH);
            canvas.drawBitmap(logo, null, dest, paint);
        }
    }

    private static String weekLabel(String subtitle) {
        if (subtitle == null) return "РОЗКЛАД";
        if (subtitle.contains("ЗНАМЕННИК")) return "ЗНАМЕННИК";
        if (subtitle.contains("ЧИСЕЛЬНИК")) return "ЧИСЕЛЬНИК";
        return subtitle.replace("• УКРИТТЯ", "").trim();
    }

    private static String cleanPhase(String phase) {
        if (phase == null || phase.trim().isEmpty()) return "РОЗКЛАД";
        if (phase.startsWith("УРОК ")) return phase.replace("УРОК ", "") + " УРОК";
        if (phase.startsWith("ПЕРЕРВА")) return phase;
        if ("AFTER_SCHOOL".equals(phase)) return "ПІСЛЯ УРОКІВ";
        if ("BEFORE_SCHOOL".equals(phase)) return "ДО УРОКІВ";
        if ("HOLIDAY".equals(phase)) return "КАНІКУЛИ";
        if ("NO_SCHEDULE".equals(phase)) return "НЕМАЄ ЗАНЯТЬ";
        if ("ERROR".equals(phase)) return "ПОМИЛКА РОЗКЛАДУ";
        return phase.replace('_', ' ');
    }

    private static String formatRemaining(long seconds) {
        long m = seconds / 60L;
        long s = seconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d", m, s);
    }

    private static String titleForMode(String mode) {
        if ("AIR_RAID".equals(mode)) return "ПОВІТРЯНА ТРИВОГА";
        if ("ALL_CLEAR".equals(mode)) return "ВІДБІЙ ТРИВОГИ";
        if ("SILENCE".equals(mode)) return "ХВИЛИНА МОВЧАННЯ";
        return "РОЗКЛАД УРОКІВ";
    }

    private float safePadding(float w) {
        float base = Math.max(24f, w * 0.026f);
        if ("HD".equals(screenProfile)) return Math.max(20f, w * 0.022f);
        if ("OTHER".equals(screenProfile)) return Math.max(28f, w * 0.030f);
        return base;
    }

    private void setText(Typeface typeface, int color, float size) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(typeface);
        paint.setColor(color);
        paint.setTextSize(Math.max(10f, size));
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawCentered(Canvas canvas, String text, float cx, float baseline) {
        if (text == null) return;
        canvas.drawText(text, cx - paint.measureText(text) / 2f, baseline, paint);
    }

    private void drawCenteredVertically(Canvas canvas, String text, float cx, RectF box) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        float y = box.centerY() - (fm.ascent + fm.descent) / 2f;
        drawCentered(canvas, text, cx, y);
    }

    private void drawCenteredMultiline(Canvas canvas, String text, RectF box, int maxLines) {
        String[] wrapped = wrapLines(text == null ? "" : text, box.width() - 12f, maxLines);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float lineH = (fm.descent - fm.ascent) * 1.12f;
        float totalH = lineH * wrapped.length;
        float y = box.centerY() - totalH / 2f - fm.ascent;
        for (String line : wrapped) {
            drawCentered(canvas, line, box.centerX(), y);
            y += lineH;
        }
    }

    private void drawLeftCenteredMultiline(Canvas canvas, String text, RectF box, int maxLines) {
        String[] wrapped = wrapLines(text == null ? "" : text, box.width() - 4f, maxLines);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float lineH = (fm.descent - fm.ascent) * 1.10f;
        float totalH = lineH * wrapped.length;
        float y = box.centerY() - totalH / 2f - fm.ascent;
        for (String line : wrapped) {
            canvas.drawText(line, box.left, y, paint);
            y += lineH;
        }
    }

    private String[] wrapLines(String value, float maxWidth, int maxLines) {
        String normalized = value == null ? "" : value.replace('\n', ' ').trim();
        if (normalized.isEmpty()) return new String[]{""};
        if (paint.measureText(normalized) <= maxWidth) return new String[]{normalized};

        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] words = normalized.split("\\s+");

        for (int wi = 0; wi < words.length; wi++) {
            String word = words[wi];
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (paint.measureText(candidate) <= maxWidth || current.length() == 0) {
                current.setLength(0);
                current.append(candidate);
            } else {
                out.add(current.toString());
                current.setLength(0);
                current.append(word);
                if (out.size() >= maxLines - 1) {
                    // Consume remaining words into the last line, then ellipsize.
                    for (int j = wi + 1; j < words.length; j++) {
                        current.append(" ").append(words[j]);
                    }
                    break;
                }
            }
        }
        if (current.length() > 0 && out.size() < maxLines) out.add(current.toString());
        if (out.isEmpty()) out.add(normalized);

        int last = out.size() - 1;
        if (paint.measureText(out.get(last)) > maxWidth) {
            out.set(last, ellipsize(out.get(last), maxWidth));
        }
        return out.toArray(new String[0]);
    }

    private String ellipsize(String text, float maxWidth) {
        if (text == null) return "";
        if (paint.measureText(text) <= maxWidth) return text;
        String ell = "…";
        float ellW = paint.measureText(ell);
        int end = text.length();
        while (end > 0 && paint.measureText(text.substring(0, end)) + ellW > maxWidth) {
            end--;
        }
        return end <= 0 ? ell : text.substring(0, end).trim() + ell;
    }

    private static RectF inset(RectF source, float dx, float dy) {
        return new RectF(source.left + dx, source.top + dy,
                source.right - dx, source.bottom - dy);
    }

    private static double parseSignedDegrees(String value) {
        if (value == null) return Double.NaN;
        try {
            String cleaned = value.replace("°", "").replace("C", "").trim();
            return Double.parseDouble(cleaned);
        } catch (Exception ignored) {
            return Double.NaN;
        }
    }

    private static String forecastWeekday(String iso) {
        try {
            LocalDate d = LocalDate.parse(iso);
            return d.format(DateTimeFormatter.ofPattern("EE", new Locale("uk", "UA")))
                    .replace(".", "").toUpperCase(new Locale("uk", "UA"));
        } catch (Exception e) {
            return "";
        }
    }

    private static String forecastDate(String iso) {
        try {
            return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd.MM"));
        } catch (Exception e) {
            return "";
        }
    }
}
