package ua.edu.cunl.lyceummobile.data

import org.json.JSONObject
import ua.edu.cunl.lyceummobile.network.HttpsClient

data class WeatherSnapshot(
    val temperature: Double? = null,
    val description: String = "Погода недоступна"
)

class WeatherRepository {
    private val endpoint =
        "https://api.open-meteo.com/v1/forecast?latitude=48.50834&longitude=32.26618&current=temperature_2m,weather_code&timezone=Europe%2FKyiv"

    suspend fun refresh(previous: WeatherSnapshot): WeatherSnapshot = try {
        val bytes = HttpsClient.fetch(endpoint, maxBytes = 24_000)
        val current = JSONObject(bytes.toString(Charsets.UTF_8)).getJSONObject("current")
        val temperature = current.getDouble("temperature_2m")
        val code = current.optInt("weather_code", -1)
        WeatherSnapshot(
            temperature = temperature,
            description = when (code) {
                0 -> "Ясно"
                in 1..3 -> "Мінлива хмарність"
                45, 48 -> "Туман"
                in 51..67 -> "Дощ"
                in 71..77 -> "Сніг"
                in 80..82 -> "Зливи"
                in 95..99 -> "Гроза"
                else -> "Кропивницький"
            }
        )
    } catch (_: Exception) {
        previous.copy(
            description = if (previous.temperature == null) {
                "Погода недоступна"
            } else {
                "Збережена погода"
            }
        )
    }
}
