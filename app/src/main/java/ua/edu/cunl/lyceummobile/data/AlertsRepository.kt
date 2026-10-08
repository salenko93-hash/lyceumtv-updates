package ua.edu.cunl.lyceummobile.data

import ua.edu.cunl.lyceummobile.core.DistrictAlert
import ua.edu.cunl.lyceummobile.network.HttpsClient
import ua.edu.cunl.lyceummobile.network.NetworkException

class AlertsRepository {
    private val endpoint =
        "https://api.alerts.in.ua/v1/iot/active_air_raid_alerts/81.json"

    suspend fun poll(token: String): DistrictAlert {
        if (token.isBlank()) throw NetworkException("Token missing")
        val bytes = HttpsClient.fetch(
            endpoint,
            maxBytes = 1024,
            headers = mapOf("Authorization" to "Bearer ${token.trim()}")
        )
        val parsed = DistrictAlert.fromCode(bytes.toString(Charsets.UTF_8))
        return parsed ?: throw NetworkException("Invalid alert response")
    }
}
