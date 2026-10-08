package ua.edu.cunl.lyceummobile.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.edu.cunl.lyceummobile.BuildConfig
import ua.edu.cunl.lyceummobile.LyceumViewModel
import ua.edu.cunl.lyceummobile.UiState
import ua.edu.cunl.lyceummobile.core.ScheduleEngine
import ua.edu.cunl.lyceummobile.core.SchoolClock
import ua.edu.cunl.lyceummobile.ui.theme.Blue
import ua.edu.cunl.lyceummobile.ui.theme.Danger
import ua.edu.cunl.lyceummobile.ui.theme.DeepNavy
import ua.edu.cunl.lyceummobile.ui.theme.Gold
import ua.edu.cunl.lyceummobile.ui.theme.LightBackground
import ua.edu.cunl.lyceummobile.ui.theme.Navy
import ua.edu.cunl.lyceummobile.ui.theme.Success
import java.time.LocalDate

@Composable
private fun ScreenFrame(
    background: Color = LightBackground,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
        ) {
            content()
        }
    }
}

@Composable
fun DashboardScreen(model: LyceumViewModel, state: UiState) {
    val rows = model.rowsFor(state.nowMillis, shelter = false)
    val schedule = model.scheduleFor(state.nowMillis, shelter = false)
    val currentLesson = schedule?.let {
        ScheduleEngine.currentBell(it, state.nowMillis)?.lesson
    }
    val nextBell = model.nextBell(state.nowMillis, shelter = false)
    val currentRow = rows.firstOrNull { it.bell.lesson == currentLesson }
    val nextRow = rows.firstOrNull { it.bell.lesson == nextBell?.lesson }
    var teacherQuery by rememberSaveable { mutableStateOf("") }
    var selectedTeacher by rememberSaveable { mutableStateOf("") }
    val teacherMatches = if (teacherQuery.isBlank() || selectedTeacher.isNotBlank()) {
        emptyList()
    } else {
        model.teacherMatches(teacherQuery)
    }
    val teacherStatus = selectedTeacher.takeIf { it.isNotBlank() }?.let {
        model.teacherDayStatus(it, state.nowMillis, shelter = false)
    }

    ScreenFrame {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Navy, Blue)
                            )
                        )
                        .padding(21.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Gold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "КРОПИВНИЦЬКИЙ",
                                        color = Gold,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "Мій ліцей",
                                    color = Color.White,
                                    fontSize = 31.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    SchoolClock.displayDate(state.nowMillis),
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                            ClassPicker(
                                selected = state.selectedClass,
                                onSelected = model::setSelectedClass
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                SchoolClock.displayTime(state.nowMillis),
                                color = Color.White,
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            state.weather.temperature?.let {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${it.toInt()}°",
                                        color = Color.White,
                                        fontSize = 31.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        state.weather.description,
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        Text(
                            model.currentWeek.label,
                            color = Gold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                SchoolCard {
                    SectionCaption("Пошук учителя")
                    OutlinedTextField(
                        value = teacherQuery,
                        onValueChange = {
                            teacherQuery = it
                            selectedTeacher = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Прізвище або ПІБ") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        singleLine = true
                    )

                    if (teacherQuery.isNotBlank() && selectedTeacher.isBlank()) {
                        if (teacherMatches.isEmpty()) {
                            Text(
                                "Вчителя не знайдено в розкладі 10–11 класів.",
                                color = DeepNavy.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                teacherMatches.forEach { teacher ->
                                    TextButton(
                                        onClick = {
                                            selectedTeacher = teacher
                                            teacherQuery = teacher
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            teacher,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Start,
                                            color = Navy,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    teacherStatus?.let { status ->
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Navy.copy(alpha = 0.08f))
                        )
                        Text(
                            status.teacher,
                            color = Navy,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        if (status.lessons.isEmpty()) {
                            Text(
                                "Сьогодні уроків немає.",
                                color = DeepNavy,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            if (status.current.isNotEmpty()) {
                                Text("ЗАРАЗ", color = Success, fontWeight = FontWeight.ExtraBold)
                                status.current.forEach { lesson ->
                                    Text(
                                        "${lesson.lesson} урок · ${lesson.time} · ${lesson.className}",
                                        color = Navy,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        buildString {
                                            append(lesson.subject.ifBlank { "Предмет не вказано" })
                                            if (lesson.room.isNotBlank()) append(" · ауд. ${lesson.room}")
                                        },
                                        color = DeepNavy
                                    )
                                }
                            } else {
                                Text(
                                    "Зараз уроку немає.",
                                    color = DeepNavy,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (status.next.isNotEmpty()) {
                                Text("НАСТУПНИЙ УРОК", color = Blue, fontWeight = FontWeight.ExtraBold)
                                status.next.forEach { lesson ->
                                    Text(
                                        "${lesson.lesson} урок · ${lesson.time} · ${lesson.className}",
                                        color = Navy,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        buildString {
                                            append(lesson.subject.ifBlank { "Предмет не вказано" })
                                            if (lesson.room.isNotBlank()) append(" · ауд. ${lesson.room}")
                                        },
                                        color = DeepNavy
                                    )
                                }
                            } else if (status.current.isNotEmpty()) {
                                Text(
                                    "Це останній урок учителя сьогодні.",
                                    color = DeepNavy.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    "На сьогодні уроки вже завершені.",
                                    color = DeepNavy.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Text(
                                "Сьогодні за розкладом",
                                color = Navy,
                                fontWeight = FontWeight.Bold
                            )
                            status.lessons.forEach { lesson ->
                                Text(
                                    buildString {
                                        append("${lesson.lesson}. ${lesson.time} · ${lesson.className} · ")
                                        append(lesson.subject.ifBlank { "—" })
                                        if (lesson.room.isNotBlank()) append(" · ауд. ${lesson.room}")
                                    },
                                    color = DeepNavy.copy(alpha = 0.78f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            model.holidayFor(state.nowMillis)?.let { holiday ->
                item {
                    SchoolCard {
                        SectionCaption("Календар")
                        Text(holiday, color = Navy, fontWeight = FontWeight.Bold)
                    }
                }
            } ?: item {
                SchoolCard {
                    SectionCaption("Сьогодні · ${state.selectedClass}")
                    if (currentRow != null) {
                        Text(
                            "Зараз ${currentRow.bell.time}",
                            color = Blue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            currentRow.entries
                                .map { it.subject }
                                .filter { it.isNotBlank() }
                                .joinToString(" / ")
                                .ifBlank { "Немає уроку" },
                            color = Navy,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text("Зараз перерва або занять немає.", color = DeepNavy)
                    }
                    if (nextRow != null) {
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Navy.copy(alpha = 0.08f))
                        )
                        Text(
                            "Далі о ${nextRow.bell.time}",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepNavy.copy(alpha = 0.65f)
                        )
                        Text(
                            nextRow.entries
                                .map { it.subject }
                                .filter { it.isNotBlank() }
                                .joinToString(" / ")
                                .ifBlank { "Немає уроку" },
                            color = Navy,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            state.content.announcements.firstOrNull { it.active }?.let { notice ->
                item {
                    SchoolCard {
                        SectionCaption("Оголошення")
                        Text(notice.title, color = Navy, fontWeight = FontWeight.Bold)
                        Text(notice.message, color = DeepNavy)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Розклад: ${state.scheduleVersion}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepNavy.copy(alpha = 0.62f)
                    )
                    TextButton(onClick = model::refreshEverything) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.width(20.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Оновити")
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableScreen(model: LyceumViewModel, state: UiState) {
    var selectedDate by rememberSaveable { mutableLongStateOf(state.nowMillis) }
    var shelter by rememberSaveable { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val localDate = SchoolClock.zoned(selectedDate).toLocalDate()
    val rows = model.rowsFor(selectedDate, shelter)
    val holiday = model.holidayFor(selectedDate)
    val highlighted = if (SchoolClock.isoDate(selectedDate) == SchoolClock.isoDate(state.nowMillis)) {
        model.currentBellLesson(state.nowMillis, shelter)
    } else {
        null
    }

    ScreenFrame {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            SchoolClock.displayDate(selectedDate),
                            color = Navy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            model.weekFor(selectedDate).label,
                            color = Blue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    ClassPicker(
                        selected = state.selectedClass,
                        onSelected = model::setSelectedClass
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                selectedDate = SchoolClock.epochMillis(
                                    LocalDate.of(year, month + 1, day)
                                )
                            },
                            localDate.year,
                            localDate.monthValue - 1,
                            localDate.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Дата: ${SchoolClock.isoDate(selectedDate)}")
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!shelter) {
                        Button(
                            onClick = { shelter = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("Звичайний") }
                    } else {
                        OutlinedButton(
                            onClick = { shelter = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("Звичайний") }
                    }
                    if (shelter) {
                        Button(
                            onClick = { shelter = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Danger)
                        ) { Text("Укриття") }
                    } else {
                        OutlinedButton(
                            onClick = { shelter = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("Укриття") }
                    }
                }
            }

            if (shelter) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Danger)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Дані укриття — тільки для авторизованих користувачів.",
                            color = Danger,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (holiday != null) {
                item {
                    SchoolCard {
                        Text(holiday, color = Navy, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                item {
                    LessonRows(
                        rows = rows,
                        highlighted = highlighted,
                        emergency = shelter
                    )
                }
            }

            item {
                Text(
                    "Версія даних: ${state.scheduleVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepNavy.copy(alpha = 0.62f)
                )
            }
        }
    }
}

@Composable
fun AnnouncementsScreen(state: UiState) {
    val notices = state.content.announcements.filter { it.active }
    val events = state.content.events.filter { it.active }

    ScreenFrame {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Оголошення",
                    color = Navy,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    state.contentStatus,
                    color = DeepNavy.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (notices.isEmpty() && events.isEmpty()) {
                item {
                    SchoolCard {
                        Text("Активних оголошень немає.", color = DeepNavy)
                    }
                }
            }

            items(notices) { notice ->
                SchoolCard {
                    SectionCaption("Оголошення")
                    Text(notice.title, color = Navy, fontWeight = FontWeight.Bold)
                    Text(notice.message, color = DeepNavy)
                    Text(
                        notice.priority,
                        color = Blue,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            items(events) { event ->
                SchoolCard {
                    SectionCaption("Подія", color = Gold)
                    Text(event.title, color = Navy, fontWeight = FontWeight.Bold)
                    val details = listOf(event.date, event.time, event.location)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (details.isNotBlank()) {
                        Text(details, color = DeepNavy.copy(alpha = 0.72f))
                    }
                }
            }
        }
    }
}

@Composable
fun MinuteSilenceScreen(
    model: LyceumViewModel,
    state: UiState,
    fullScreen: Boolean
) {
    ScreenFrame(background = DeepNavy) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(10.dp)) }
            item {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = "Свічка пам'яті",
                    tint = Gold,
                    modifier = Modifier.width(92.dp)
                )
            }
            item {
                Text(
                    "ХВИЛИНА\nМОВЧАННЯ",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
            item {
                Text(
                    "Вшановуємо пам’ять загиблих",
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center
                )
            }
            item {
                Text(
                    if (fullScreen) model.silenceSecondsRemaining().toString() else "09:00 — 09:01",
                    color = Gold,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light
                )
            }
            item {
                Text(
                    "Метроном: 60 ударів за хвилину",
                    color = Color.White.copy(alpha = 0.76f)
                )
            }

            if (!fullScreen) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.10f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Метроном працює лише коли застосунок відкритий.",
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = state.metronomeEnabled,
                                    onCheckedChange = model::setMetronomeEnabled
                                )
                                Text("Увімкнути метроном", color = Color.White)
                            }
                            Text(
                                "Гучність: ${(state.metronomeVolume * 100).toInt()}%",
                                color = Color.White
                            )
                            Slider(
                                value = state.metronomeVolume,
                                onValueChange = model::setMetronomeVolume,
                                valueRange = 0f..1f
                            )
                            Button(
                                onClick = model::beginSilenceTest,
                                enabled = !state.alert.isAlarm,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Gold,
                                    contentColor = Navy
                                )
                            ) {
                                Text("Тестувати 60 секунд")
                            }
                        }
                    }
                }
            } else if (state.testSilenceUntil != null) {
                item {
                    OutlinedButton(onClick = model::endSilenceTest) {
                        Text("Завершити тест", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyScreen(model: LyceumViewModel, state: UiState) {
    val rows = model.rowsFor(state.nowMillis, shelter = true)
    val highlighted = model.currentBellLesson(state.nowMillis, shelter = true)
    var showAnnouncements by rememberSaveable { mutableStateOf(false) }
    var showSubstitutions by rememberSaveable { mutableStateOf(false) }
    val notices = state.content.announcements.filter { it.active }
    val substitutions = state.content.substitutions.filter {
        it.active && it.className == state.selectedClass
    }

    ScreenFrame(background = Danger) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            item {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.width(72.dp)
                )
            }
            item {
                Text(
                    state.alert.label,
                    color = Color.White,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
            item {
                Text(
                    "Кропивницький район • alerts.in.ua UID 81",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            if (!model.alertIsCurrent) {
                item {
                    Text(
                        "Дані можуть бути застарілими. Дотримуйтесь офіційних сповіщень.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.22f))
                            .padding(12.dp)
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "КЕРУВАННЯ ПІД ЧАС ТРИВОГИ",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = model::refreshEverything,
                                enabled = !state.isSyncing,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Gold,
                                    contentColor = Navy
                                )
                            ) {
                                if (state.isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.width(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(if (state.isSyncing) "Оновлення…" else "Оновити")
                            }
                            OutlinedButton(
                                onClick = { showAnnouncements = !showAnnouncements },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text(if (showAnnouncements) "Сховати оголошення" else "Оголошення")
                            }
                        }
                        OutlinedButton(
                            onClick = { showSubstitutions = !showSubstitutions },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(if (showSubstitutions) "Сховати заміни" else "Заміни для ${state.selectedClass}")
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightBackground),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "РОЗКЛАД УКРИТТЯ",
                                color = Navy,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
                            ClassPicker(
                                selected = state.selectedClass,
                                onSelected = model::setSelectedClass
                            )
                        }
                        Text(
                            "${model.currentWeek.label} · ${state.selectedClass}",
                            color = Danger,
                            fontWeight = FontWeight.Bold
                        )
                        LessonRows(
                            rows = rows,
                            highlighted = highlighted,
                            emergency = true
                        )
                    }
                }
            }
            if (showAnnouncements) {
                if (notices.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(15.dp)) {
                                Text("ОГОЛОШЕННЯ", color = Navy, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(6.dp))
                                Text("Активних оголошень немає.", color = DeepNavy)
                            }
                        }
                    }
                } else {
                    items(notices) { notice ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(15.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("ОГОЛОШЕННЯ", color = Blue, fontWeight = FontWeight.Bold)
                                Text(notice.title, color = Navy, fontWeight = FontWeight.ExtraBold)
                                Text(notice.message, color = DeepNavy)
                            }
                        }
                    }
                }
            }
            if (showSubstitutions) {
                if (substitutions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(15.dp)) {
                                Text("ЗАМІНИ", color = Navy, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(6.dp))
                                Text("Активних замін для ${state.selectedClass} немає.", color = DeepNavy)
                            }
                        }
                    }
                } else {
                    items(substitutions) { substitution ->
                        val lessonText = if (substitution.lesson > 0) {
                            "Урок ${substitution.lesson}"
                        } else {
                            "Заміна"
                        }
                        val details = substitution.entries.map { entry ->
                            listOf(entry.subject, entry.teacher, entry.room)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                        }.filter { it.isNotBlank() }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightBackground),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(15.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("ЗАМІНИ", color = Blue, fontWeight = FontWeight.Bold)
                                Text(
                                    listOfNotNull(substitution.date, substitution.day)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" · ")
                                        .ifBlank { state.selectedClass },
                                    color = DeepNavy.copy(alpha = 0.72f)
                                )
                                Text("${state.selectedClass} · $lessonText", color = Navy, fontWeight = FontWeight.ExtraBold)
                                if (details.isEmpty()) {
                                    Text("Деталі заміни відсутні.", color = DeepNavy)
                                } else {
                                    details.forEach { line ->
                                        Text(line, color = DeepNavy)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Застосунок допоміжний. Орієнтуйтеся на офіційні канали тривоги та інструкції відповідальних осіб.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AllClearScreen() {
    ScreenFrame(background = Success) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.width(92.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "ВІДБІЙ ПОВІТРЯНОЇ ТРИВОГИ",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Перевірено через alerts.in.ua • UID 81",
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsScreen(model: LyceumViewModel, state: UiState) {
    var token by rememberSaveable { mutableStateOf("") }
    var tokenMessage by remember { mutableStateOf("") }

    ScreenFrame {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Налаштування",
                    color = Navy,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            item {
                SchoolCard {
                    SectionCaption("Мій клас")
                    ClassPicker(
                        selected = state.selectedClass,
                        onSelected = model::setSelectedClass
                    )
                    OutlinedTextField(
                        value = state.referenceMonday,
                        onValueChange = model::setReferenceMonday,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Базовий понеділок чисельника") },
                        supportingText = {
                            Text("YYYY-MM-DD · використовується ${model.validReferenceMonday()}")
                        },
                        singleLine = true
                    )
                }
            }

            item {
                SchoolCard {
                    SectionCaption("Тривоги — тільки alerts.in.ua")
                    Text(
                        "UID 81 · Кропивницький район",
                        color = Navy,
                        fontWeight = FontWeight.Bold
                    )
                    Text(state.alertStatus, color = DeepNavy.copy(alpha = 0.72f))
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Новий API-токен") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val ok = model.saveToken(token)
                            tokenMessage = if (ok) {
                                token = ""
                                "Токен зашифровано в Android Keystore"
                            } else {
                                "Не вдалося зберегти токен"
                            }
                        },
                        enabled = token.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Зберегти токен")
                    }
                    OutlinedButton(
                        onClick = { model.refreshEverything() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Перевірити зараз")
                    }
                    TextButton(
                        onClick = {
                            model.clearToken()
                            tokenMessage = "Токен видалено"
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Danger)
                        Spacer(Modifier.width(6.dp))
                        Text("Видалити токен", color = Danger)
                    }
                    if (tokenMessage.isNotBlank()) {
                        Text(tokenMessage, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "Перевірка alerts.in.ua у цій версії працює під час відкритого застосунку. LyceumMobile не є офіційним засобом сповіщення про небезпеку.",
                        color = Danger,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                SchoolCard {
                    SectionCaption("Google Sheets")
                    OutlinedTextField(
                        value = state.googleSheetsUrl,
                        onValueChange = model::setGoogleSheetsUrl,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("HTTPS URL Google Apps Script /exec") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        minLines = 2,
                        maxLines = 4
                    )
                    Text(state.contentStatus, style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                SchoolCard {
                    SectionCaption("Дистанційне оновлення розкладів")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.source == "GITHUB") {
                            Button(onClick = { model.setSource("GITHUB") }) { Text("GitHub") }
                        } else {
                            OutlinedButton(onClick = { model.setSource("GITHUB") }) { Text("GitHub") }
                        }
                        if (state.source == "LOCAL") {
                            Button(onClick = { model.setSource("LOCAL") }) { Text("HTTPS") }
                        } else {
                            OutlinedButton(onClick = { model.setSource("LOCAL") }) { Text("HTTPS") }
                        }
                    }

                    if (state.source == "GITHUB") {
                        OutlinedTextField(
                            value = state.githubManifestUrl,
                            onValueChange = model::setGithubManifestUrl,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("GitHub Raw content_manifest.json") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            minLines = 2,
                            maxLines = 4
                        )
                    } else {
                        OutlinedTextField(
                            value = state.localManifestUrl,
                            onValueChange = model::setLocalManifestUrl,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Локальний HTTPS content_manifest.json") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            minLines = 2,
                            maxLines = 4
                        )
                    }

                    Button(
                        onClick = model::refreshEverything,
                        enabled = !state.isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.isSyncing) "Синхронізація…" else "Синхронізувати всі дані")
                    }
                    OutlinedButton(
                        onClick = model::rollbackSchedules,
                        enabled = !state.alert.isAlarm,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Повернути попередні розклади")
                    }
                    Text("Версія: ${state.scheduleVersion}", fontWeight = FontWeight.SemiBold)
                    Text(state.scheduleStatus, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Новий комплект активується лише після SHA-256 перевірки всіх 4 файлів і свіжого підтвердження відсутності тривоги.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepNavy.copy(alpha = 0.66f)
                    )
                }
            }

            item {
                SchoolCard {
                    SectionCaption("Хвилина мовчання")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.metronomeEnabled,
                            onCheckedChange = model::setMetronomeEnabled
                        )
                        Text("Метроном 60 ударів/хв", color = Navy)
                    }
                    Text("Гучність: ${(state.metronomeVolume * 100).toInt()}%")
                    Slider(
                        value = state.metronomeVolume,
                        onValueChange = model::setMetronomeVolume,
                        valueRange = 0f..1f
                    )
                    Button(
                        onClick = model::beginSilenceTest,
                        enabled = !state.alert.isAlarm,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Тестувати 60 секунд")
                    }
                }
            }

            item {
                SchoolCard {
                    SectionCaption("Про застосунок")
                    Text("LyceumMobile Android ${BuildConfig.VERSION_NAME}", color = Navy, fontWeight = FontWeight.Bold)
                    Text("Пакет: ua.edu.cunl.lyceummobile")
                    Text("Мінімальна версія Android: 8.0 (API 26)")
                    Text("Оптимізовано для Redmi Note 15 Pro+ 5G / HyperOS 2")
                    Text("Оповіщення: alerts.in.ua • UID 81")
                    Text(
                        "Створено за функціональною моделлю LyceumMobile iOS 1.0.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
