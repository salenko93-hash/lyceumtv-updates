package ua.edu.cunl.tv.weather;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight weather client for Kropyvnytskyi.
 *
 * Uses Open-Meteo over HTTPS without an API key. In addition to current
 * conditions, five daily forecast rows are requested so the TV screen can
 * render today + the next four days in the approved layout.
 */
public final class WeatherRepository {
    private static final String ENDPOINT =
            "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=48.51&longitude=32.26"
                    + "&current=temperature_2m,apparent_temperature,weather_code"
                    + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                    + "&forecast_days=5"
                    + "&timezone=Europe%2FKyiv";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface Listener {
        void onWeather(Weather weather);
        void onWeatherError(String message);
    }

    public static final class ForecastDay {
        public final String dateIso;
        public final double maxC;
        public final double minC;
        public final int weatherCode;

        public ForecastDay(String dateIso, double maxC, double minC, int weatherCode) {
            this.dateIso = dateIso == null ? "" : dateIso;
            this.maxC = maxC;
            this.minC = minC;
            this.weatherCode = weatherCode;
        }

        public String rangeText() {
            return Math.round(maxC) + "° / " + Math.round(minC) + "°";
        }
    }

    public static final class Weather {
        public final double temperatureC;
        public final double apparentC;
        public final int weatherCode;
        public final List<ForecastDay> forecast;

        public Weather(double temperatureC, double apparentC, int weatherCode,
                       List<ForecastDay> forecast) {
            this.temperatureC = temperatureC;
            this.apparentC = apparentC;
            this.weatherCode = weatherCode;
            this.forecast = Collections.unmodifiableList(new ArrayList<>(
                    forecast == null ? Collections.emptyList() : forecast));
        }

        public String temperatureText() {
            return signedDegrees(temperatureC);
        }

        public String apparentText() {
            return signedDegrees(apparentC);
        }

        public String conditionText() {
            return describeCode(weatherCode);
        }

        private static String signedDegrees(double value) {
            long rounded = Math.round(value);
            return String.format(Locale.ROOT, "%+d°", rounded);
        }
    }

    public void refresh(Listener listener) {
        if (listener == null) return;
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(ENDPOINT);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(7_000);
                connection.setReadTimeout(7_000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");

                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("HTTP " + status);
                }

                String body;
                try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) >= 0) {
                        if (n > 0) out.write(buffer, 0, n);
                    }
                    body = out.toString(StandardCharsets.UTF_8.name());
                }

                JSONObject root = new JSONObject(body);
                JSONObject current = root.getJSONObject("current");
                double temperature = current.getDouble("temperature_2m");
                double apparent = current.optDouble("apparent_temperature", temperature);
                int code = current.optInt("weather_code", -1);

                List<ForecastDay> forecast = new ArrayList<>();
                JSONObject daily = root.optJSONObject("daily");
                if (daily != null) {
                    JSONArray dates = daily.optJSONArray("time");
                    JSONArray codes = daily.optJSONArray("weather_code");
                    JSONArray max = daily.optJSONArray("temperature_2m_max");
                    JSONArray min = daily.optJSONArray("temperature_2m_min");
                    int count = minLength(dates, codes, max, min);
                    for (int i = 0; i < count; i++) {
                        forecast.add(new ForecastDay(
                                dates.optString(i, ""),
                                max.optDouble(i, Double.NaN),
                                min.optDouble(i, Double.NaN),
                                codes.optInt(i, -1)
                        ));
                    }
                }

                Weather weather = new Weather(temperature, apparent, code, forecast);
                main.post(() -> listener.onWeather(weather));
            } catch (Exception e) {
                String message = e.getClass().getSimpleName();
                main.post(() -> listener.onWeatherError(message));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private static int minLength(JSONArray... arrays) {
        int n = Integer.MAX_VALUE;
        for (JSONArray array : arrays) {
            if (array == null) return 0;
            n = Math.min(n, array.length());
        }
        return n == Integer.MAX_VALUE ? 0 : n;
    }

    public static String describeCode(int code) {
        return switch (code) {
            case 0 -> "Ясно";
            case 1 -> "Переважно ясно";
            case 2 -> "Мінлива хмарність";
            case 3 -> "Хмарно";
            case 45, 48 -> "Туман";
            case 51, 53, 55 -> "Мряка";
            case 56, 57 -> "Крижана мряка";
            case 61 -> "Невеликий дощ";
            case 63 -> "Дощ";
            case 65 -> "Сильний дощ";
            case 66, 67 -> "Крижаний дощ";
            case 71 -> "Невеликий сніг";
            case 73 -> "Сніг";
            case 75, 77 -> "Сильний сніг";
            case 80 -> "Невеликі зливи";
            case 81 -> "Зливи";
            case 82 -> "Сильні зливи";
            case 85, 86 -> "Снігові заряди";
            case 95 -> "Гроза";
            case 96, 99 -> "Гроза з градом";
            default -> "Поточна погода";
        };
    }
}
