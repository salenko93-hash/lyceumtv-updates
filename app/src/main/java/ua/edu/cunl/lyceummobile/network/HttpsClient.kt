package ua.edu.cunl.lyceummobile.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class NetworkException(message: String) : Exception(message)

object HttpsClient {
    suspend fun fetch(
        address: String,
        maxBytes: Int,
        headers: Map<String, String> = emptyMap()
    ): ByteArray = withContext(Dispatchers.IO) {
        val uri = validateHttps(address)
        fetch(URL(uri.toString()), maxBytes, headers)
    }

    suspend fun fetch(
        url: URL,
        maxBytes: Int,
        headers: Map<String, String> = emptyMap()
    ): ByteArray = withContext(Dispatchers.IO) {
        require(url.protocol.equals("https", ignoreCase = true)) { "HTTPS required" }

        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LyceumMobile/1.0 Android")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

        try {
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                throw NetworkException("HTTP $status")
            }
            val contentLength = connection.contentLengthLong
            if (contentLength > maxBytes) throw NetworkException("Response too large")

            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) throw NetworkException("Response too large")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    fun resolve(path: String, manifestUrl: String): String {
        val base = validateHttps(manifestUrl)
        val resolved = base.resolve(path)
        require(resolved.scheme.equals("https", ignoreCase = true)) { "HTTPS required" }
        require(!resolved.host.isNullOrBlank()) { "Host missing" }
        require(resolved.userInfo == null) { "Embedded credentials are not allowed" }
        return resolved.toString()
    }

    fun validateHttps(address: String): URI {
        val clean = address.trim()
        val uri = runCatching { URI(clean) }.getOrElse {
            throw IllegalArgumentException("Invalid URL")
        }
        require(uri.scheme.equals("https", ignoreCase = true)) { "HTTPS required" }
        require(!uri.host.isNullOrBlank()) { "Host missing" }
        require(uri.userInfo == null) { "Embedded credentials are not allowed" }
        return uri
    }
}
