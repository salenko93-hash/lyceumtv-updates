package ua.edu.cunl.lyceummobile.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.edu.cunl.lyceummobile.core.ScheduleFile
import ua.edu.cunl.lyceummobile.core.SchoolCalendar
import ua.edu.cunl.lyceummobile.core.WeekType
import ua.edu.cunl.lyceummobile.network.HttpsClient
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class ScheduleRepository(private val context: Context) {
    companion object {
        val filenames = listOf(
            "schedule_numerator.json",
            "schedule_denominator.json",
            "shelter_numerator.json",
            "shelter_denominator.json"
        )
    }

    private val root = File(context.filesDir, "schedule-bundles").apply { mkdirs() }
    private val current = File(root, "current")
    private val previous = File(root, "previous")

    var version: String = "Вбудований"
        private set
    var status: String = "Вбудований розклад"
        private set
    var timetables: Map<String, ScheduleFile> = loadBundledSchedules()
        private set
    var calendar: SchoolCalendar = loadBundledCalendar()
        private set

    init {
        loadCachedIfValid()
    }

    fun schedule(week: WeekType, shelter: Boolean): ScheduleFile? {
        val prefix = if (shelter) "shelter_" else "schedule_"
        return timetables["$prefix${week.fileSuffix}.json"]
    }

    suspend fun refresh(
        manifestAddress: String,
        allowedToInstall: () -> Boolean
    ): String {
        if (manifestAddress.isBlank()) {
            status = "Адресу віддалених розкладів не налаштовано"
            return status
        }
        if (!allowedToInstall()) {
            status = "Оновлення відкладено, доки стан тривоги не підтверджено"
            return status
        }

        return try {
            val manifestBytes = HttpsClient.fetch(manifestAddress, 250_000)
            val manifest = JsonCodec.parseManifest(manifestBytes.toString(Charsets.UTF_8))
            if (manifest.version == version) {
                status = "Розклади актуальні"
                return status
            }

            val downloaded = linkedMapOf<String, ByteArray>()
            for (name in filenames) {
                val spec = manifest.schedules[name]
                    ?: error("Manifest missing $name")
                downloaded[name] = verifiedDownload(spec, manifestAddress, 700_000)
            }
            manifest.calendar?.let {
                downloaded["calendar.json"] = verifiedDownload(it, manifestAddress, 150_000)
            }

            for (name in filenames) {
                val decoded = JsonCodec.parseSchedule(
                    downloaded.getValue(name).toString(Charsets.UTF_8)
                )
                require(decoded.bellSchedule.size == 8 && decoded.days.isNotEmpty())
            }
            downloaded["calendar.json"]?.let {
                JsonCodec.parseSchoolCalendar(it.toString(Charsets.UTF_8))
            }

            if (!allowedToInstall()) error("Alarm blocks schedule update")

            withContext(Dispatchers.IO) {
                val staging = File(root, "staging-${UUID.randomUUID()}").apply { mkdirs() }
                try {
                    downloaded.forEach { (name, bytes) ->
                        File(staging, name).writeBytes(bytes)
                    }
                    if (!downloaded.containsKey("calendar.json")) {
                        when {
                            File(current, "calendar.json").isFile ->
                                File(current, "calendar.json").copyTo(
                                    File(staging, "calendar.json"), overwrite = true
                                )
                            else -> context.assets.open("calendar.json").use { input ->
                                File(staging, "calendar.json").outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                    File(staging, "version.txt").writeText(manifest.version, Charsets.UTF_8)

                    val validated = decodeFolder(staging)
                    if (!allowedToInstall()) error("Alarm blocks schedule update")

                    if (previous.exists()) previous.deleteRecursively()
                    val hadCurrent = current.exists()
                    if (hadCurrent && !current.renameTo(previous)) {
                        copyDirectory(current, previous)
                        current.deleteRecursively()
                    }

                    try {
                        if (!staging.renameTo(current)) {
                            copyDirectory(staging, current)
                            staging.deleteRecursively()
                        }
                    } catch (e: Exception) {
                        current.deleteRecursively()
                        if (hadCurrent && previous.exists()) {
                            if (!previous.renameTo(current)) {
                                copyDirectory(previous, current)
                            }
                        }
                        throw e
                    }

                    timetables = validated.first
                    calendar = validated.second
                    version = manifest.version
                } finally {
                    if (staging.exists()) staging.deleteRecursively()
                }
            }

            status = "Оновлено: $version"
            status
        } catch (e: Exception) {
            status = "Нові файли не встановлено: ${e.message ?: "помилка"}"
            status
        }
    }

    suspend fun rollback(): String = withContext(Dispatchers.IO) {
        if (!previous.exists()) {
            status = "Попереднього комплекту немає"
            return@withContext status
        }
        return@withContext try {
            val validated = decodeFolder(previous)
            val oldCurrent = File(root, "rollback-current")
            if (oldCurrent.exists()) oldCurrent.deleteRecursively()
            if (current.exists()) {
                if (!current.renameTo(oldCurrent)) {
                    copyDirectory(current, oldCurrent)
                    current.deleteRecursively()
                }
            }
            if (!previous.renameTo(current)) {
                copyDirectory(previous, current)
                previous.deleteRecursively()
            }
            if (oldCurrent.exists()) {
                if (!oldCurrent.renameTo(previous)) {
                    copyDirectory(oldCurrent, previous)
                    oldCurrent.deleteRecursively()
                }
            }
            timetables = validated.first
            calendar = validated.second
            version = File(current, "version.txt").takeIf { it.isFile }
                ?.readText(Charsets.UTF_8)?.trim().orEmpty().ifBlank { "Попередній" }
            status = "Відновлено: $version"
            status
        } catch (e: Exception) {
            status = "Rollback не виконано: ${e.message ?: "помилка"}"
            status
        }
    }

    private suspend fun verifiedDownload(
        spec: ResourceSpec,
        manifestAddress: String,
        maxBytes: Int
    ): ByteArray {
        val expected = spec.sha256.lowercase()
        require(expected.length == 64 && expected.all { it in "0123456789abcdef" }) {
            "Invalid SHA-256"
        }
        val resolved = HttpsClient.resolve(spec.url, manifestAddress)
        val bytes = HttpsClient.fetch(resolved, maxBytes)
        val actual = sha256(bytes)
        require(actual == expected) { "SHA-256 mismatch" }
        return bytes
    }

    private fun loadBundledSchedules(): Map<String, ScheduleFile> {
        val result = linkedMapOf<String, ScheduleFile>()
        for (name in filenames) {
            runCatching {
                val text = context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
                result[name] = JsonCodec.parseSchedule(text)
            }
        }
        return result
    }

    private fun loadBundledCalendar(): SchoolCalendar = runCatching {
        context.assets.open("calendar.json").bufferedReader(Charsets.UTF_8).use {
            JsonCodec.parseSchoolCalendar(it.readText())
        }
    }.getOrDefault(SchoolCalendar())

    private fun loadCachedIfValid() {
        runCatching {
            if (!current.exists()) return
            val validated = decodeFolder(current)
            timetables = validated.first
            calendar = validated.second
            version = File(current, "version.txt").takeIf { it.isFile }
                ?.readText(Charsets.UTF_8)?.trim().orEmpty().ifBlank { "Збережений" }
            status = "Збережений розклад: $version"
        }
    }

    private fun decodeFolder(directory: File): Pair<Map<String, ScheduleFile>, SchoolCalendar> {
        val result = linkedMapOf<String, ScheduleFile>()
        for (name in filenames) {
            val file = File(directory, name)
            require(file.isFile) { "$name missing" }
            result[name] = JsonCodec.parseSchedule(file.readText(Charsets.UTF_8))
        }
        val calendarFile = File(directory, "calendar.json")
        val cal = if (calendarFile.isFile) {
            JsonCodec.parseSchoolCalendar(calendarFile.readText(Charsets.UTF_8))
        } else {
            loadBundledCalendar()
        }
        return result to cal
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun copyDirectory(from: File, to: File) {
        if (!to.exists()) to.mkdirs()
        from.listFiles().orEmpty().forEach { source ->
            val target = File(to, source.name)
            if (source.isDirectory) {
                copyDirectory(source, target)
            } else {
                source.copyTo(target, overwrite = true)
            }
        }
    }
}
