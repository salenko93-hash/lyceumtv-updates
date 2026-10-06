package ua.edu.cunl.lyceummobile.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.edu.cunl.lyceummobile.core.ContentFeed
import ua.edu.cunl.lyceummobile.network.HttpsClient
import java.io.File

class ContentRepository(context: Context) {
    private val cache = File(context.filesDir, "content-feed.json")

    var feed: ContentFeed = loadCached()
        private set

    var status: String = if (cache.exists()) "Збережені оголошення" else "Локальна копія"
        private set

    suspend fun refresh(address: String): String {
        if (address.isBlank()) {
            status = "Укажи адресу Google Apps Script у налаштуваннях"
            return status
        }
        return try {
            val bytes = HttpsClient.fetch(address, maxBytes = 400_000)
            val decoded = JsonCodec.parseContentFeed(bytes.toString(Charsets.UTF_8))
            withContext(Dispatchers.IO) {
                val temp = File(cache.parentFile, "${cache.name}.tmp")
                temp.writeBytes(bytes)
                if (cache.exists()) cache.delete()
                if (!temp.renameTo(cache)) {
                    temp.copyTo(cache, overwrite = true)
                    temp.delete()
                }
            }
            feed = decoded
            status = "Оголошення та заміни оновлено"
            status
        } catch (_: Exception) {
            status = "Мережа недоступна — показано збережені оголошення"
            status
        }
    }

    private fun loadCached(): ContentFeed = runCatching {
        if (!cache.exists()) return ContentFeed()
        JsonCodec.parseContentFeed(cache.readText(Charsets.UTF_8))
    }.getOrDefault(ContentFeed())
}
