package ua.edu.cunl.tv.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScheduleSnapshot {
    public static final class Entry {
        public final String subgroup;
        public final String subject;
        public final String room;
        public final String teacher;

        public Entry(String subgroup, String subject, String room, String teacher) {
            this.subgroup = subgroup == null ? "" : subgroup;
            this.subject = subject == null ? "" : subject;
            this.room = room == null ? "" : room;
            this.teacher = teacher == null ? "" : teacher;
        }
    }

    public static final class Row {
        public final String className;
        public final List<Entry> entries;

        public Row(String className, List<Entry> entries) {
            this.className = className == null ? "" : className;
            this.entries = Collections.unmodifiableList(new ArrayList<>(
                    entries == null ? Collections.emptyList() : entries));
        }
    }

    public final String title;
    public final String subtitle;
    public final String phase;
    public final int lesson;
    public final boolean holiday;
    public final boolean afterSchool;
    public final boolean breakTime;
    public final long secondsToLessonEnd;
    public final long secondsToNextLesson;
    public final long secondsIntoBreak;
    public final List<Row> rows;

    public ScheduleSnapshot(String title, String subtitle, String phase, int lesson,
                            boolean holiday, boolean afterSchool, boolean breakTime,
                            long secondsToLessonEnd, long secondsToNextLesson,
                            long secondsIntoBreak, List<Row> rows) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.phase = phase == null ? "" : phase;
        this.lesson = lesson;
        this.holiday = holiday;
        this.afterSchool = afterSchool;
        this.breakTime = breakTime;
        this.secondsToLessonEnd = Math.max(0L, secondsToLessonEnd);
        this.secondsToNextLesson = Math.max(0L, secondsToNextLesson);
        this.secondsIntoBreak = Math.max(0L, secondsIntoBreak);
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
    }

    public static ScheduleSnapshot message(String title, String subtitle, String phase,
                                           boolean holiday, boolean afterSchool) {
        return new ScheduleSnapshot(title, subtitle, phase, 0, holiday, afterSchool,
                false, 0L, 0L, 0L, Collections.emptyList());
    }
}
