package ua.edu.cunl.lyceummobile.data

import org.json.JSONArray
import org.json.JSONObject
import ua.edu.cunl.lyceummobile.core.Announcement
import ua.edu.cunl.lyceummobile.core.BellSlot
import ua.edu.cunl.lyceummobile.core.ContentFeed
import ua.edu.cunl.lyceummobile.core.LessonEntry
import ua.edu.cunl.lyceummobile.core.ScheduleFile
import ua.edu.cunl.lyceummobile.core.SchoolCalendar
import ua.edu.cunl.lyceummobile.core.SchoolEvent
import ua.edu.cunl.lyceummobile.core.Substitution

object JsonCodec {
    fun parseSchedule(text: String): ScheduleFile {
        val root = JSONObject(text)
        val bellsJson = root.optJSONArray("bellSchedule")
            ?: throw IllegalArgumentException("bellSchedule missing")
        val bells = buildList {
            for (i in 0 until bellsJson.length()) {
                val item = bellsJson.getJSONObject(i)
                add(BellSlot(item.getInt("lesson"), item.getString("time")))
            }
        }

        val daysRoot = root.optJSONObject("days")
            ?: throw IllegalArgumentException("days missing")
        val days = linkedMapOf<String, Map<Int, Map<String, List<LessonEntry>>>>()
        for (dayKey in daysRoot.keys()) {
            val lessonRoot = daysRoot.getJSONObject(dayKey)
            val lessons = linkedMapOf<Int, Map<String, List<LessonEntry>>>()
            for (lessonKey in lessonRoot.keys()) {
                val lessonNumber = lessonKey.toIntOrNull() ?: continue
                val classRoot = lessonRoot.getJSONObject(lessonKey)
                val classes = linkedMapOf<String, List<LessonEntry>>()
                for (className in classRoot.keys()) {
                    classes[className] = parseEntries(classRoot.optJSONArray(className))
                }
                lessons[lessonNumber] = classes
            }
            days[dayKey] = lessons
        }

        val result = ScheduleFile(
            weekType = root.optString("weekType"),
            source = root.optString("source").takeIf { it.isNotBlank() },
            title = root.optString("title").takeIf { it.isNotBlank() },
            bellSchedule = bells,
            days = days
        )
        require(result.bellSchedule.size == 8) { "Expected 8 bells" }
        require(result.days.isNotEmpty()) { "Schedule has no days" }
        return result
    }

    fun parseContentFeed(text: String): ContentFeed {
        val root = JSONObject(text)
        val announcements = mutableListOf<Announcement>()
        root.optJSONArray("announcements")?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                announcements += Announcement(
                    active = o.optBoolean("active", true),
                    title = o.optString("title"),
                    message = o.optString("message"),
                    priority = o.optString("priority", "NORMAL")
                )
            }
        }

        val events = mutableListOf<SchoolEvent>()
        root.optJSONArray("events")?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                events += SchoolEvent(
                    active = o.optBoolean("active", true),
                    title = o.optString("title"),
                    date = o.optString("date"),
                    time = o.optString("time"),
                    location = o.optString("location")
                )
            }
        }

        val substitutions = mutableListOf<Substitution>()
        root.optJSONArray("substitutions")?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                substitutions += Substitution(
                    date = o.optNullableString("date"),
                    week = o.optNullableString("week"),
                    day = o.optNullableString("day"),
                    lesson = o.optInt("lesson", 0),
                    className = o.optString("class"),
                    scope = o.optNullableString("scope"),
                    active = o.optBoolean("active", true),
                    entries = parseEntries(o.optJSONArray("entries"))
                )
            }
        }

        return ContentFeed(announcements, events, substitutions)
    }

    fun parseSchoolCalendar(text: String): SchoolCalendar {
        val root = JSONObject(text)
        val days = mutableListOf<SchoolCalendar.DayOff>()
        root.optJSONArray("daysOff")?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val date = o.optString("date")
                if (date.isNotBlank()) {
                    days += SchoolCalendar.DayOff(
                        date = date,
                        label = o.optNullableString("label")
                    )
                }
            }
        }

        val ranges = mutableListOf<SchoolCalendar.Range>()
        root.optJSONArray("ranges")?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val from = o.optString("from")
                val to = o.optString("to")
                if (from.isNotBlank() && to.isNotBlank()) {
                    ranges += SchoolCalendar.Range(
                        from = from,
                        to = to,
                        label = o.optNullableString("label")
                    )
                }
            }
        }
        return SchoolCalendar(days, ranges)
    }

    fun parseManifest(text: String): ContentManifest {
        val root = JSONObject(text)
        val version = root.optString("version")
        require(version.isNotBlank() && version.length <= 80) { "Invalid manifest version" }

        val schedulesObject = root.optJSONObject("schedules")
            ?: throw IllegalArgumentException("Manifest schedules missing")
        val schedules = linkedMapOf<String, ResourceSpec>()
        for (key in schedulesObject.keys()) {
            val o = schedulesObject.getJSONObject(key)
            schedules[key] = ResourceSpec(
                url = o.getString("url"),
                sha256 = o.getString("sha256")
            )
        }

        val calendar = root.optJSONObject("calendar")?.let {
            ResourceSpec(it.getString("url"), it.getString("sha256"))
        }
        return ContentManifest(version, schedules, calendar)
    }

    private fun parseEntries(array: JSONArray?): List<LessonEntry> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                add(
                    LessonEntry(
                        subject = o.optString("subject"),
                        room = o.optString("room"),
                        teacher = o.optString("teacher")
                    )
                )
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }
}

data class ContentManifest(
    val version: String,
    val schedules: Map<String, ResourceSpec>,
    val calendar: ResourceSpec?
)

data class ResourceSpec(
    val url: String,
    val sha256: String
)
