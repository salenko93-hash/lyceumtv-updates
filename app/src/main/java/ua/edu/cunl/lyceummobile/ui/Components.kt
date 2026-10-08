package ua.edu.cunl.lyceummobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.edu.cunl.lyceummobile.core.LessonRow
import ua.edu.cunl.lyceummobile.core.ScheduleEngine
import ua.edu.cunl.lyceummobile.ui.theme.Blue
import ua.edu.cunl.lyceummobile.ui.theme.Danger
import ua.edu.cunl.lyceummobile.ui.theme.DeepNavy
import ua.edu.cunl.lyceummobile.ui.theme.LightBackground
import ua.edu.cunl.lyceummobile.ui.theme.Navy

@Composable
fun SchoolCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SectionCaption(text: String, color: Color = Blue) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
fun ClassPicker(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(50),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = Navy)
                Text(selected, fontWeight = FontWeight.Bold, color = Navy)
                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Navy)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScheduleEngine.classNames.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        expanded = false
                        onSelected(name)
                    }
                )
            }
        }
    }
}

@Composable
fun LessonRows(
    rows: List<LessonRow>,
    highlighted: Int?,
    emergency: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            val active = highlighted == row.bell.lesson
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (active) {
                            if (emergency) Color(0xFFFFECEF) else Color(0xFFEAF2FE)
                        } else {
                            Color.White
                        }
                    )
                    .padding(13.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (active) {
                                if (emergency) Danger else Blue
                            } else {
                                LightBackground
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = row.bell.lesson.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = if (active) Color.White else Navy
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        row.bell.time,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepNavy.copy(alpha = 0.72f)
                    )
                    if (row.entries.isEmpty() || row.entries.all { it.subject.isBlank() }) {
                        Text(
                            "Немає уроку",
                            color = DeepNavy.copy(alpha = 0.68f)
                        )
                    } else {
                        row.entries.forEachIndexed { index, entry ->
                            if (index > 0) {
                                Spacer(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Navy.copy(alpha = 0.08f))
                                )
                            }
                            if (entry.subject.isNotBlank()) {
                                Text(
                                    entry.subject,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Navy
                                )
                            }
                            val details = listOf(
                                entry.room.takeIf { it.isNotBlank() }?.let { "Каб. $it" }.orEmpty(),
                                entry.teacher
                            ).filter { it.isNotBlank() }.joinToString(" · ")
                            if (details.isNotBlank()) {
                                Text(
                                    details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepNavy.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }
                }
                if (active) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = "Поточний урок",
                        tint = if (emergency) Danger else Blue
                    )
                }
            }
        }
    }
}
