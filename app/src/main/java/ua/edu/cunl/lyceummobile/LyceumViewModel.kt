package ua.edu.cunl.lyceummobile

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.edu.cunl.lyceummobile.audio.SilenceMetronome
import ua.edu.cunl.lyceummobile.core.ContentFeed
import ua.edu.cunl.lyceummobile.core.DistrictAlert
import ua.edu.cunl.lyceummobile.core.LessonRow
import ua.edu.cunl.lyceummobile.core.ScheduleEngine
import ua.edu.cunl.lyceummobile.core.ScheduleFile
import ua.edu.cunl.lyceummobile.core.SchoolClock
import ua.edu.cunl.lyceummobile.core.WeekCycle
import ua.edu.cunl.lyceummobile.core.TeacherDayStatus
import ua.edu.cunl.lyceummobile.core.TeacherLessonLocation
import ua.edu.cunl.lyceummobile.core.WeekType
import ua.edu.cunl.lyceummobile.data.AlertsRepository
import ua.edu.cunl.lyceummobile.data.ContentRepository
import ua.edu.cunl.lyceummobile.data.ScheduleRepository
import ua.edu.cunl.lyceummobile.data.SecureTokenStore
import ua.edu.cunl.lyceummobile.data.SettingsStore
import ua.edu.cunl.lyceummobile.data.WeatherRepository
import ua.edu.cunl.lyceummobile.data.WeatherSnapshot
import java.time.DayOfWeek
import kotlin.math.ceil

data class UiState(
    val nowMillis: Long = System.currentTimeMillis(),
    val alert: DistrictAlert = DistrictAlert.UNKNOWN,
    val alertOnline: Boolean = false,
    val alertLastVerified: Long? = null,
    val alertStatus: String = "Перевірка стану тривоги",
    val allClearUntil: Long? = null,
    val testSilenceUntil: Long? = null,
    val isSyncing: Boolean = false,
    val selectedClass: String = "10-А",
    val referenceMonday: String = WeekCycle.DEFAULT_REFERENCE,
    val googleSheetsUrl: String = "",
    val githubManifestUrl: String = "",
    val localManifestUrl: String = "",
    val source: String = "GITHUB",
    val metronomeEnabled: Boolean = true,
    val metronomeVolume: Float = 0.25f,
    val scheduleVersion: String = "Вбудований",
    val scheduleStatus: String = "Вбудований розклад",
    val contentStatus: String = "Локальна копія",
    val content: ContentFeed = ContentFeed(),
    val weather: WeatherSnapshot = WeatherSnapshot()
)

class LyceumViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsStore(application)
    private val tokenStore = SecureTokenStore(application)
    private val alerts = AlertsRepository()
    private val schedules = ScheduleRepository(application)
    private val content = ContentRepository(application)
    private val weather = WeatherRepository()
    private val metronome = SilenceMetronome(application)

    private val runtimePrefs =
        application.getSharedPreferences("lyceummobile_runtime", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        UiState(
            alert = DistrictAlert.entries.firstOrNull {
                it.code == runtimePrefs.getString("lastAlert", "?")
            } ?: DistrictAlert.UNKNOWN,
            alertLastVerified = runtimePrefs.takeIf {
                it.contains("lastAlertVerified")
            }?.getLong("lastAlertVerified", 0L)?.takeIf { it > 0L },
            alertStatus = if (runtimePrefs.contains("lastAlert")) {
                "Попередній підтверджений стан"
            } else {
                "API-токен не перевірено"
            },
            selectedClass = settings.selectedClass,
            referenceMonday = settings.referenceMonday,
            googleSheetsUrl = settings.googleSheetsUrl,
            githubManifestUrl = settings.githubManifestUrl,
            localManifestUrl = settings.localManifestUrl,
            source = settings.source,
            metronomeEnabled = settings.metronomeEnabled,
            metronomeVolume = settings.metronomeVolume,
            scheduleVersion = schedules.version,
            scheduleStatus = schedules.status,
            contentStatus = content.status,
            content = content.feed
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    @Volatile
    private var foreground = false
    private var clockJob: Job? = null
    private var alertJob: Job? = null
    private var contentJob: Job? = null
    private var scheduleJob: Job? = null
    private var weatherJob: Job? = null
    private var alertRequestInProgress = false

    fun setForeground(value: Boolean) {
        if (foreground == value) return
        foreground = value
        if (value) {
            startJobs()
        } else {
            stopJobs()
        }
    }

    private fun startJobs() {
        stopJobs(keepForeground = true)
        tick()

        clockJob = viewModelScope.launch {
            while (foreground) {
                tick()
                delay(1_000L)
            }
        }
        alertJob = viewModelScope.launch {
            while (foreground) {
                refreshAlert()
                delay(12_000L)
            }
        }
        contentJob = viewModelScope.launch {
            while (foreground) {
                refreshContent()
                delay(300_000L)
            }
        }
        scheduleJob = viewModelScope.launch {
            while (foreground) {
                refreshSchedules()
                delay(900_000L)
            }
        }
        weatherJob = viewModelScope.launch {
            while (foreground) {
                refreshWeather()
                delay(1_800_000L)
            }
        }
    }

    private fun stopJobs(keepForeground: Boolean = false) {
        if (!keepForeground) foreground = false
        clockJob?.cancel()
        alertJob?.cancel()
        contentJob?.cancel()
        scheduleJob?.cancel()
        weatherJob?.cancel()
        clockJob = null
        alertJob = null
        contentJob = null
        scheduleJob = null
        weatherJob = null
        metronome.stop()
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val old = _state.value
        val testDeadline = old.testSilenceUntil?.takeIf { it > now }
        val clearDeadline = old.allClearUntil?.takeIf { it > now }
        _state.value = old.copy(
            nowMillis = now,
            testSilenceUntil = testDeadline,
            allClearUntil = clearDeadline
        )
        updateMetronome()
    }

    val alertIsCurrent: Boolean
        get() {
            val s = _state.value
            val verified = s.alertLastVerified ?: return false
            return s.alertOnline && System.currentTimeMillis() - verified < 60_000L
        }

    val isAlarm: Boolean get() = _state.value.alert.isAlarm

    val isAllClear: Boolean
        get() {
            val s = _state.value
            return !s.alert.isAlarm && (s.allClearUntil ?: 0L) > s.nowMillis
        }

    val isSilence: Boolean
        get() {
            val s = _state.value
            if (s.alert.isAlarm || isAllClear) return false
            return SchoolClock.isDailySilence(s.nowMillis) ||
                (s.testSilenceUntil ?: 0L) > s.nowMillis
        }

    val currentWeek: WeekType
        get() = WeekCycle.type(_state.value.nowMillis, validReferenceMonday())

    fun weekFor(epochMillis: Long): WeekType =
        WeekCycle.type(epochMillis, validReferenceMonday())

    fun scheduleFor(epochMillis: Long, shelter: Boolean): ScheduleFile? =
        schedules.schedule(weekFor(epochMillis), shelter)

    fun rowsFor(epochMillis: Long, shelter: Boolean): List<LessonRow> {
        val s = _state.value
        val schedule = scheduleFor(epochMillis, shelter) ?: return emptyList()
        return ScheduleEngine.lessons(
            schedule = schedule,
            epochMillis = epochMillis,
            className = s.selectedClass,
            substitutions = s.content.substitutions,
            shelter = shelter,
            referenceMonday = validReferenceMonday()
        )
    }

    fun holidayFor(epochMillis: Long): String? =
        schedules.calendar.labelFor(SchoolClock.isoDate(epochMillis))

    fun currentBellLesson(epochMillis: Long, shelter: Boolean): Int? =
        scheduleFor(epochMillis, shelter)?.let {
            ScheduleEngine.currentBell(it, epochMillis)?.lesson
        }

    fun nextBell(epochMillis: Long, shelter: Boolean) =
        scheduleFor(epochMillis, shelter)?.let {
            ScheduleEngine.nextBell(it, epochMillis)
        }

    private fun cleanTeacherName(raw: String): String = raw
        .replace(Regex("(?i)^\\s*гр\\.?\\s*\\d+\\s*[.:]?\\s*"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun teacherKey(raw: String): String = cleanTeacherName(raw)
        .lowercase()
        .replace(Regex("[^а-яіїєґa-z0-9]"), "")

    fun teacherMatches(query: String, limit: Int = 6): List<String> {
        val needle = teacherKey(query)
        if (needle.isBlank()) return emptyList()

        val names = linkedMapOf<String, String>()
        listOf(WeekType.NUMERATOR, WeekType.DENOMINATOR).forEach { week ->
            schedules.schedule(week, shelter = false)?.days?.values?.forEach { lessons ->
                lessons.values.forEach { classes ->
                    classes.values.flatten().forEach { entry ->
                        val display = cleanTeacherName(entry.teacher)
                        val key = teacherKey(display)
                        if (key.isNotBlank()) names.putIfAbsent(key, display)
                    }
                }
            }
        }
        _state.value.content.substitutions.forEach { substitution ->
            substitution.entries.forEach { entry ->
                val display = cleanTeacherName(entry.teacher)
                val key = teacherKey(display)
                if (key.isNotBlank()) names.putIfAbsent(key, display)
            }
        }

        return names.entries
            .filter { (key, display) ->
                key.contains(needle) || display.lowercase().contains(query.trim().lowercase())
            }
            .map { it.value }
            .sortedBy { it.lowercase() }
            .take(limit)
    }

    fun teacherDayStatus(
        teacher: String,
        epochMillis: Long = _state.value.nowMillis,
        shelter: Boolean = false
    ): TeacherDayStatus {
        val wantedKey = teacherKey(teacher)
        val schedule = scheduleFor(epochMillis, shelter)
        if (wantedKey.isBlank() || schedule == null) {
            return TeacherDayStatus(teacher, emptyList(), emptyList(), emptyList())
        }

        val statusName = cleanTeacherName(teacher)
        val found = mutableListOf<TeacherLessonLocation>()
        ScheduleEngine.classNames.forEach { className ->
            val rows = ScheduleEngine.lessons(
                schedule = schedule,
                epochMillis = epochMillis,
                className = className,
                substitutions = _state.value.content.substitutions,
                shelter = shelter,
                referenceMonday = validReferenceMonday()
            )
            rows.forEach { row ->
                row.entries.forEach { entry ->
                    if (teacherKey(entry.teacher) == wantedKey) {
                        found += TeacherLessonLocation(
                            lesson = row.bell.lesson,
                            time = row.bell.time,
                            className = className,
                            subject = entry.subject,
                            room = entry.room
                        )
                    }
                }
            }
        }

        val lessons = found
            .distinctBy { listOf(it.lesson, it.time, it.className, it.subject, it.room) }
            .sortedWith(compareBy<TeacherLessonLocation> { it.lesson }.thenBy { it.className })

        val minute = SchoolClock.minuteOfDay(epochMillis)
        val currentLessonNumbers = schedule.bellSchedule
            .filter { bell -> bell.minuteBounds?.let { minute in it } == true }
            .map { it.lesson }
            .toSet()
        val current = lessons.filter { it.lesson in currentLessonNumbers }

        val nextLessonNumber = schedule.bellSchedule
            .filter { bell -> bell.minuteBounds?.first?.let { it > minute } == true }
            .map { it.lesson }
            .firstOrNull { lesson -> lessons.any { it.lesson == lesson } }
        val next = if (nextLessonNumber == null) emptyList() else lessons.filter { it.lesson == nextLessonNumber }

        return TeacherDayStatus(
            teacher = statusName,
            lessons = lessons,
            current = current,
            next = next
        )
    }

    fun silenceSecondsRemaining(): Int {
        val s = _state.value
        s.testSilenceUntil?.takeIf { it > s.nowMillis }?.let {
            return ceil((it - s.nowMillis) / 1000.0).toInt().coerceAtLeast(0)
        }
        return SchoolClock.secondsRemainingInSilenceMinute(s.nowMillis)
    }

    fun beginSilenceTest() {
        if (isAlarm) return
        _state.value = _state.value.copy(
            testSilenceUntil = System.currentTimeMillis() + 60_000L
        )
        updateMetronome()
    }

    fun endSilenceTest() {
        _state.value = _state.value.copy(testSilenceUntil = null)
        updateMetronome()
    }

    fun updateMetronome() {
        val s = _state.value
        metronome.setActive(
            active = foreground && isSilence && s.metronomeEnabled,
            volume = s.metronomeVolume
        )
    }

    suspend fun refreshAlert() {
        if (!foreground || alertRequestInProgress) return
        val token = tokenStore.load()
        if (token.isNullOrBlank()) {
            _state.value = _state.value.copy(
                alertOnline = false,
                alertStatus = "Укажи токен alerts.in.ua в налаштуваннях"
            )
            return
        }

        alertRequestInProgress = true
        try {
            val result = alerts.poll(token)
            val old = _state.value
            val now = System.currentTimeMillis()
            val clearDeadline =
                if (old.alert.isAlarm && result == DistrictAlert.NONE) now + 15_000L
                else if (result.isAlarm) null
                else old.allClearUntil

            if (result.isAlarm) {
                metronome.stop()
            }

            _state.value = old.copy(
                alert = result,
                alertOnline = true,
                alertLastVerified = now,
                alertStatus = "alerts.in.ua • UID 81 • оновлено",
                allClearUntil = clearDeadline,
                testSilenceUntil = if (result.isAlarm) null else old.testSilenceUntil
            )
            runtimePrefs.edit()
                .putString("lastAlert", result.code)
                .putLong("lastAlertVerified", now)
                .apply()
            updateMetronome()
        } catch (_: Exception) {
            _state.value = _state.value.copy(
                alertOnline = false,
                alertStatus = "OFFLINE — збережено останній підтверджений стан"
            )
        } finally {
            alertRequestInProgress = false
        }
    }

    suspend fun refreshContent() {
        val status = content.refresh(_state.value.googleSheetsUrl)
        _state.value = _state.value.copy(
            content = content.feed,
            contentStatus = status
        )
    }

    suspend fun refreshSchedules() {
        val snapshot = _state.value
        val source = snapshot.source
        val manifest = if (source == "LOCAL") {
            snapshot.localManifestUrl
        } else {
            snapshot.githubManifestUrl
        }
        val status = schedules.refresh(manifest) {
            val current = _state.value
            foreground &&
                current.alert == DistrictAlert.NONE &&
                alertIsCurrent &&
                !isAllClear &&
                current.source == source
        }
        _state.value = _state.value.copy(
            scheduleVersion = schedules.version,
            scheduleStatus = status
        )
    }

    suspend fun refreshWeather() {
        val updated = weather.refresh(_state.value.weather)
        _state.value = _state.value.copy(weather = updated)
    }

    fun refreshEverything() {
        if (_state.value.isSyncing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true)
            try {
                refreshAlert()
                refreshContent()
                refreshSchedules()
                refreshWeather()
            } finally {
                _state.value = _state.value.copy(isSyncing = false)
            }
        }
    }

    fun rollbackSchedules() {
        if (isAlarm) return
        viewModelScope.launch {
            val status = schedules.rollback()
            _state.value = _state.value.copy(
                scheduleVersion = schedules.version,
                scheduleStatus = status
            )
        }
    }

    fun saveToken(token: String): Boolean {
        val ok = tokenStore.save(token)
        if (ok) {
            runtimePrefs.edit().remove("lastAlert").remove("lastAlertVerified").apply()
            _state.value = _state.value.copy(
                alert = DistrictAlert.UNKNOWN,
                alertOnline = false,
                alertLastVerified = null,
                alertStatus = "Новий токен збережено — перевіряю"
            )
            viewModelScope.launch { refreshAlert() }
        }
        return ok
    }

    fun clearToken() {
        tokenStore.clear()
        runtimePrefs.edit().clear().apply()
        metronome.stop()
        _state.value = _state.value.copy(
            alert = DistrictAlert.UNKNOWN,
            alertOnline = false,
            alertLastVerified = null,
            alertStatus = "API-токен видалено",
            allClearUntil = null,
            testSilenceUntil = null
        )
    }

    fun setSelectedClass(value: String) {
        if (value !in ScheduleEngine.classNames) return
        settings.selectedClass = value
        _state.value = _state.value.copy(selectedClass = value)
    }

    fun setReferenceMonday(value: String) {
        settings.referenceMonday = value
        _state.value = _state.value.copy(referenceMonday = value)
    }

    fun setGoogleSheetsUrl(value: String) {
        settings.googleSheetsUrl = value
        _state.value = _state.value.copy(googleSheetsUrl = value)
    }

    fun setGithubManifestUrl(value: String) {
        settings.githubManifestUrl = value
        _state.value = _state.value.copy(githubManifestUrl = value)
    }

    fun setLocalManifestUrl(value: String) {
        settings.localManifestUrl = value
        _state.value = _state.value.copy(localManifestUrl = value)
    }

    fun setSource(value: String) {
        if (value != "GITHUB" && value != "LOCAL") return
        settings.source = value
        _state.value = _state.value.copy(source = value)
    }

    fun setMetronomeEnabled(value: Boolean) {
        settings.metronomeEnabled = value
        _state.value = _state.value.copy(metronomeEnabled = value)
        updateMetronome()
    }

    fun setMetronomeVolume(value: Float) {
        settings.metronomeVolume = value
        _state.value = _state.value.copy(metronomeVolume = value.coerceIn(0f, 1f))
        updateMetronome()
    }

    fun validReferenceMonday(): String {
        val current = _state.value.referenceMonday
        val parsed = SchoolClock.parseIsoDate(current)
        return if (parsed != null && parsed.dayOfWeek == DayOfWeek.MONDAY) {
            current
        } else {
            WeekCycle.DEFAULT_REFERENCE
        }
    }

    override fun onCleared() {
        stopJobs()
        metronome.release()
        super.onCleared()
    }
}
