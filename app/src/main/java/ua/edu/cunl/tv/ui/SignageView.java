package ua.edu.cunl.tv.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
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
 * TV-first renderer for the approved LyceumTV schedule pages.
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
     * Preferred weather setter for 2.7.0.14: current conditions + daily forecast.
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

        // The approved NORMAL/AIR_RAID pages intentionally have no debug footer.
        if (!"NORMAL".equals(mode) && !"AIR_RAID".equals(mode)) {
            drawFooter(canvas, w, h, pad);
        }
    }

    private void drawBackground(Canvas canvas, float w, float h) {
        if ("SILENCE".equals(mode)) {
            paint.setShader(new LinearGradient(0f, 0f, 0f, h,
                    Color.rgb(35, 37, 43), Color.rgb(8, 9, 12), Shader.TileMode.CLAMP));
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

        // Side columns are intentionally wide. The center remains fixed so
        // normal and shelter pages switch without visual jumping.
        float sideW = w * 0.292f;
        float leftX = pad;
        float rightX = w - pad - sideW;

        drawSideCards(canvas, leftX, sideW, rightX, sideW, h);
        drawCenterHeader(canvas, w, h, now, raid);

        if (raid) {
            drawRaidCenter(canvas, w, h, now);
        } else if (showAnnouncement && schedule.breakTime) {
            drawAnnouncementCenter(canvas, w, h);
        } else {
            drawNormalCenter(canvas, w, h, now);
        }
    }

    private void drawCenterHeader(Canvas canvas, float w, float h,
                                  ZonedDateTime now, boolean raid) {
        // White logo.
        float logoTop = h * 0.026f;
        float logoH = h * 0.145f;
        if (logo != null && logo.getWidth() > 0 && logo.getHeight() > 0) {
            float logoW = logoH * ((float) logo.getWidth() / (float) logo.getHeight());
            RectF dest = new RectF(w * 0.50f - logoW / 2f, logoTop,
                    w * 0.50f + logoW / 2f, logoTop + logoH);
            paint.setColor(Color.argb(34, 0, 0, 0));
            canvas.drawOval(new RectF(dest.left - 10f, dest.top + 8f,
                    dest.right + 10f, dest.bottom + 15f), paint);
            paint.setAlpha(255);
            canvas.drawBitmap(logo, null, dest, paint);
        }

        setText(bold, Color.WHITE, h * 0.049f);
        drawCentered(canvas, raid ? "РОЗКЛАД В УКРИТТІ" : "РОЗКЛАД УРОКІВ",
                w * 0.50f, h * 0.245f);

        String week = weekLabel(schedule.subtitle);
        RectF pill = new RectF(w * 0.389f, h * 0.267f, w * 0.611f, h * 0.331f);
        paint.setColor(ORANGE);
        canvas.drawRoundRect(pill, h * 0.018f, h * 0.018f, paint);

        setText(bold, Color.WHITE, h * 0.041f);
        drawCenteredVertically(canvas, week, pill.centerX(), pill);

        String day = now.format(DateTimeFormatter.ofPattern("EEEE", new Locale("uk", "UA")))
                .toUpperCase(new Locale("uk", "UA"));
        String date = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        setText(condensedBold, Color.WHITE, h * 0.043f);
        drawCentered(canvas, day, w * 0.50f, h * 0.402f);

        setText(condensed, Color.argb(242, 255, 255, 255), h * 0.026f);
        drawCentered(canvas, date, w * 0.50f, h * 0.445f);
    }

    private void drawSideCards(Canvas canvas, float leftX, float leftW,
                               float rightX, float rightW, float h) {
        int total = Math.min(8, schedule.rows.size());
        float top = h * 0.118f;
        float bottom = h * 0.775f;
        float gap = h * 0.026f;
        float cardH = (bottom - top - gap * 3f) / 4f;

        for (int i = 0; i < total; i++) {
            boolean right = i >= 4;
            int rowIndex = right ? i - 4 : i;
            float x = right ? rightX : leftX;
            float cardW = right ? rightW : leftW;
            float y = top + rowIndex * (cardH + gap);
            RectF card = new RectF(x, y, x + cardW, y + cardH);
            drawScheduleCard(canvas, card, schedule.rows.get(i));
        }
    }

    private void drawScheduleCard(Canvas canvas, RectF card, ScheduleSnapshot.Row row) {
        float radius = card.height() * 0.10f;
        paint.setColor(IVORY);
        canvas.drawRoundRect(card, radius, radius, paint);

        float classW = card.width() * 0.205f;
        RectF classCell = new RectF(card.left, card.top, card.left + classW, card.bottom);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, card.height() * 0.007f));
        stroke.setColor(GRID);
        canvas.drawLine(classCell.right, card.top, classCell.right, card.bottom, stroke);

        setText(bold, NAVY, card.height() * 0.255f);
        drawCentered(canvas, row.className, classCell.centerX(),
                classCell.centerY() - card.height() * 0.055f);

        setText(medium, NAVY_MUTED, card.height() * 0.095f);
        String lessonLabel = schedule.lesson > 0 ? schedule.lesson + " УРОК" : "УРОК";
        drawCentered(canvas, lessonLabel, classCell.centerX(),
                classCell.centerY() + card.height() * 0.205f);

        if (row.entries.isEmpty()) {
            setText(medium, NAVY, card.height() * 0.27f);
            drawCentered(canvas, "—", (classCell.right + card.right) / 2f,
                    card.centerY() - (paint.ascent() + paint.descent()) / 2f);
            return;
        }

        int count = Math.min(2, row.entries.size());
        float rowH = card.height() / count;
        if (count == 2) {
            canvas.drawLine(classCell.right, card.centerY(), card.right, card.centerY(), stroke);
        }

        for (int i = 0; i < count; i++) {
            RectF entryBox = new RectF(classCell.right, card.top + i * rowH,
                    card.right, card.top + (i + 1) * rowH);
            drawEntryRow(canvas, entryBox, row.entries.get(i), count == 2);
        }

        if (row.entries.size() > 2) {
            setText(medium, NAVY_MUTED, Math.max(11f, card.height() * 0.075f));
            String extra = "+" + (row.entries.size() - 2);
            canvas.drawText(extra, card.right - paint.measureText(extra) - 7f,
                    card.bottom - 5f, paint);
        }
    }

    private void drawEntryRow(Canvas canvas, RectF box, ScheduleSnapshot.Entry entry,
                              boolean compact) {
        float subjectW = box.width() * 0.46f;
        float roomW = box.width() * 0.18f;

        RectF subject = new RectF(box.left, box.top, box.left + subjectW, box.bottom);
        RectF room = new RectF(subject.right, box.top, subject.right + roomW, box.bottom);
        RectF teacher = new RectF(room.right, box.top, box.right, box.bottom);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, box.height() * 0.009f));
        stroke.setColor(GRID);
        canvas.drawLine(subject.right, box.top, subject.right, box.bottom, stroke);
        canvas.drawLine(room.right, box.top, room.right, box.bottom, stroke);

        String subjectText = entry.subject == null || entry.subject.trim().isEmpty()
                ? "—" : entry.subject.trim();

        if (entry.subgroup != null && !entry.subgroup.trim().isEmpty()) {
            setText(medium, Color.rgb(45, 105, 160), Math.max(10f, box.height() * 0.125f));
            canvas.drawText(entry.subgroup.trim(), subject.left + box.width() * 0.025f,
                    subject.top + box.height() * 0.21f, paint);
            RectF shifted = new RectF(subject.left + 4f, subject.top + box.height() * 0.15f,
                    subject.right - 4f, subject.bottom - 1f);
            setText(bold, NAVY, Math.max(14f, box.height() * (compact ? 0.245f : 0.215f)));
            drawCenteredMultiline(canvas, subjectText, shifted, 2);
        } else {
            setText(bold, NAVY, Math.max(14f, box.height() * (compact ? 0.255f : 0.225f)));
            drawCenteredMultiline(canvas, subjectText,
                    inset(subject, box.width() * 0.025f, box.height() * 0.08f), 2);
        }

        setText(medium, NAVY, Math.max(10f, box.height() * 0.14f));
        drawCentered(canvas, "КАБ.", room.centerX(), room.top + box.height() * 0.34f);

        setText(bold, NAVY, Math.max(16f, box.height() * 0.255f));
        String roomText = entry.room == null || entry.room.trim().isEmpty() ? "—" : entry.room.trim();
        drawCentered(canvas, roomText, room.centerX(), room.top + box.height() * 0.72f);

        setText(medium, NAVY, Math.max(13f, box.height() * (compact ? 0.205f : 0.185f)));
        String teacherText = entry.teacher == null || entry.teacher.trim().isEmpty()
                ? "—" : entry.teacher.trim();
        drawCenteredMultiline(canvas, teacherText,
                inset(teacher, box.width() * 0.025f, box.height() * 0.08f), 2);
    }

    private void drawNormalCenter(Canvas canvas, float w, float h, ZonedDateTime now) {
        setText(bold, Color.WHITE, h * 0.099f);
        drawCentered(canvas, now.format(DateTimeFormatter.ofPattern("HH:mm")),
                w * 0.50f, h * 0.555f);

        RectF status = new RectF(w * 0.382f, h * 0.594f, w * 0.618f, h * 0.755f);
        paint.setColor(IVORY);
        canvas.drawRoundRect(status, h * 0.018f, h * 0.018f, paint);

        long remaining = 0L;
        String phase = cleanPhase(schedule.phase);
        String countdownLabel = "";
        if (schedule.breakTime && schedule.secondsToNextLesson > 0L) {
            remaining = schedule.secondsToNextLesson;
            countdownLabel = "ДО КІНЦЯ ПЕРЕРВИ";
        } else if (schedule.secondsToLessonEnd > 0L && schedule.lesson > 0) {
            remaining = schedule.secondsToLessonEnd;
            countdownLabel = "ДО КІНЦЯ УРОКУ";
        }

        if (remaining > 0L) {
            setText(bold, NAVY, h * 0.038f);
            drawCentered(canvas, phase, w * 0.50f, status.top + status.height() * 0.28f);

            setText(bold, NAVY, h * 0.020f);
            drawCentered(canvas, countdownLabel, w * 0.50f,
                    status.top + status.height() * 0.52f);

            setText(bold, NAVY, h * 0.052f);
            drawCentered(canvas, formatRemaining(remaining), w * 0.50f,
                    status.top + status.height() * 0.87f);
        } else {
            setText(bold, NAVY, h * 0.038f);
            drawCenteredVertically(canvas, phase, w * 0.50f, status);
        }

        drawWeatherPanel(canvas, w, h);
    }

    private void drawAnnouncementCenter(Canvas canvas, float w, float h) {
        setText(bold, Color.WHITE, h * 0.080f);
        drawCentered(canvas, "ПЕРЕРВА", w * 0.50f, h * 0.545f);

        RectF box = new RectF(w * 0.375f, h * 0.585f, w * 0.625f, h * 0.755f);
        paint.setColor(IVORY);
        canvas.drawRoundRect(box, h * 0.018f, h * 0.018f, paint);

        setText(bold, ORANGE, h * 0.021f);
        drawCentered(canvas, "ОГОЛОШЕННЯ", w * 0.50f, box.top + box.height() * 0.24f);

        setText(medium, NAVY, h * 0.021f);
        drawCenteredMultiline(canvas, announcement,
                new RectF(box.left + 18f, box.top + box.height() * 0.27f,
                        box.right - 18f, box.bottom - 24f), 3);

        if (schedule.secondsToNextLesson > 0L) {
            setText(bold, NAVY, h * 0.020f);
            drawCentered(canvas, "ДО КІНЦЯ ПЕРЕРВИ • "
                            + formatRemaining(schedule.secondsToNextLesson),
                    w * 0.50f, box.bottom - h * 0.020f);
        }
        drawWeatherPanel(canvas, w, h);
    }

    private void drawRaidCenter(Canvas canvas, float w, float h, ZonedDateTime now) {
        setText(bold, Color.WHITE, h * 0.099f);
        drawCentered(canvas, now.format(DateTimeFormatter.ofPattern("HH:mm")),
                w * 0.50f, h * 0.555f);

        setText(bold, Color.WHITE, h * 0.043f);
        drawCentered(canvas, "ПОВІТРЯНА ТРИВОГА", w * 0.50f, h * 0.645f);

        setText(bold, Color.WHITE, h * 0.027f);
        drawCentered(canvas, "НЕГАЙНО ПРОЙДІТЬ В УКРИТТЯ", w * 0.50f, h * 0.693f);

        RectF lesson = new RectF(w * 0.382f, h * 0.726f, w * 0.618f, h * 0.810f);
        paint.setColor(IVORY);
        canvas.drawRoundRect(lesson, h * 0.016f, h * 0.016f, paint);

        setText(bold, NAVY, h * 0.038f);
        drawCenteredVertically(canvas, cleanPhase(schedule.phase), w * 0.50f, lesson);

        // A subtle source line is retained for staff diagnostics but kept well
        // below the emergency message so it never competes with the instruction.
        if (detail != null && !detail.trim().isEmpty()) {
            setText(regular, Color.argb(190, 255, 255, 255), h * 0.0125f);
            drawCentered(canvas, ellipsize(detail, w * 0.30f), w * 0.50f, h * 0.852f);
        }
    }

    private void drawWeatherPanel(Canvas canvas, float w, float h) {
        RectF box = new RectF(w * 0.284f, h * 0.790f, w * 0.716f, h * 0.952f);
        paint.setColor(Color.argb(48, 5, 70, 130));
        canvas.drawRoundRect(box, h * 0.018f, h * 0.018f, paint);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, h * 0.0013f));
        stroke.setColor(Color.argb(78, 255, 255, 255));
        canvas.drawRoundRect(box, h * 0.018f, h * 0.018f, stroke);

        float leftW = box.width() * 0.43f;
        RectF current = new RectF(box.left, box.top, box.left + leftW, box.bottom);
        canvas.drawLine(current.right, box.top + 14f, current.right, box.bottom - 14f, stroke);

        setText(bold, Color.WHITE, h * 0.023f);
        canvas.drawText("Кропивницький", current.left + h * 0.027f,
                current.top + h * 0.040f, paint);

        setText(regular, Color.argb(235, 255, 255, 255), h * 0.014f);
        String today = "Сьогодні, " + KyivTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        canvas.drawText(today, current.left + h * 0.027f,
                current.top + h * 0.066f, paint);

        if (weatherAvailable) {
            setText(bold, Color.WHITE, h * 0.051f);
            String temp = Math.round(weatherTemperatureC) + "°C";
            canvas.drawText(temp, current.left + h * 0.027f,
                    current.bottom - h * 0.026f, paint);

            drawWeatherIcon(canvas, weatherCode,
                    current.right - current.width() * 0.25f,
                    current.centerY() + h * 0.018f, h * 0.040f);

            setText(regular, Color.argb(225, 255, 255, 255), h * 0.0125f);
            String condition = weatherCondition;
            if (!Double.isNaN(weatherApparentC)) {
                condition += " • відч. " + Math.round(weatherApparentC) + "°";
            }
            canvas.drawText(ellipsize(condition, current.width() * 0.54f),
                    current.left + h * 0.027f, current.bottom - h * 0.007f, paint);
        } else {
            setText(medium, Color.WHITE, h * 0.018f);
            drawCenteredMultiline(canvas, weatherCondition,
                    inset(current, h * 0.025f, h * 0.035f), 2);
        }

        drawForecastDays(canvas, new RectF(current.right, box.top, box.right, box.bottom));
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

        setText(bold, Color.rgb(247, 215, 134), h * 0.065f);
        drawCentered(canvas, "ХВИЛИНА МОВЧАННЯ", w * 0.50f, h * 0.40f);

        float cx = w * 0.50f;
        float baseY = h * 0.71f;
        paint.setColor(Color.rgb(240, 238, 228));
        canvas.drawRoundRect(new RectF(cx - w * 0.026f, baseY - h * 0.18f,
                cx + w * 0.026f, baseY), 10f, 10f, paint);
        paint.setColor(Color.rgb(255, 178, 48));
        canvas.drawOval(new RectF(cx - w * 0.018f, baseY - h * 0.27f,
                cx + w * 0.018f, baseY - h * 0.175f), paint);

        setText(regular, Color.LTGRAY, h * 0.025f);
        drawCentered(canvas, detail, w * 0.50f, h * 0.82f);
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

    private void drawFooter(Canvas canvas, float w, float h, float pad) {
        setText(regular, Color.argb(165, 255, 255, 255), Math.max(11f, h * 0.012f));
        String left = productionDataPresent ? "DATA OK" : "DATA CHECK";
        left += " • " + screenProfile;
        if (!clockStatus.isEmpty()) left += " • " + clockStatus;
        canvas.drawText(ellipsize(left, w * 0.54f), pad, h - h * 0.016f, paint);
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
