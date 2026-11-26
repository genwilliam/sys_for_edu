package com.sysforedu.backend.schedule;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class ScheduleRepository {
    private static final String SELECT_COLUMNS =
            "id, course, class_name, teacher, room, campus, weekday, start_time, end_time, type, status";
    private final JdbcTemplate jdbc;

    private final RowMapper<ScheduleItemDto> rowMapper = (rs, rowNum) -> new ScheduleItemDto(
            rs.getString("id"),
            rs.getString("course"),
            rs.getString("class_name"),
            rs.getString("teacher"),
            rs.getString("room"),
            rs.getString("campus"),
            rs.getString("weekday"),
            rs.getTime("start_time").toLocalTime(),
            rs.getTime("end_time").toLocalTime(),
            rs.getString("type"),
            rs.getString("status"));

    public ScheduleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lockScheduleWrites() {
        jdbc.queryForObject("SELECT id FROM schedule_write_lock WHERE id = 1 FOR UPDATE", Integer.class);
    }

    public List<ScheduleItemDto> findAll(String campus, String course, String teacher,
                                         String room, String weekday, String status) {
        StringBuilder sql = new StringBuilder("SELECT ").append(SELECT_COLUMNS).append(" FROM schedule_items WHERE 1=1");
        List<Object> args = new ArrayList<>();
        addFilter(sql, args, "campus", campus);
        addFilter(sql, args, "course", course);
        addFilter(sql, args, "teacher", teacher);
        addFilter(sql, args, "room", room);
        addFilter(sql, args, "weekday", weekday);
        addFilter(sql, args, "status", status);
        sql.append(" ORDER BY weekday, start_time, id");
        return jdbc.query(sql.toString(), rowMapper, args.toArray());
    }

    public List<ScheduleItemDto> findAllActive() {
        return jdbc.query("SELECT " + SELECT_COLUMNS + " FROM schedule_items WHERE status <> '已取消'",
                rowMapper);
    }

    public ScheduleItemDto findById(String id) {
        return jdbc.query("SELECT " + SELECT_COLUMNS + " FROM schedule_items WHERE id = ?",
                        rowMapper, id)
                .stream().findFirst().orElse(null);
    }

    public ScheduleItemDto insert(ScheduleRequests.Create create) {
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                        INSERT INTO schedule_items
                        (id, course, class_name, teacher, room, campus, weekday, start_time, end_time, type, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, create.course(), create.className(), create.teacher(), create.room(), create.campus(),
                create.weekday(), Time.valueOf(create.startTime()), Time.valueOf(create.endTime()), create.type(),
                normalizedStatus(create.status()));
        return findById(id);
    }

    public void update(String id, ScheduleRequests.Create create) {
        jdbc.update("""
                        UPDATE schedule_items
                        SET course = ?, class_name = ?, teacher = ?, room = ?, campus = ?, weekday = ?,
                            start_time = ?, end_time = ?, type = ?, status = ?
                        WHERE id = ?
                        """,
                create.course(), create.className(), create.teacher(), create.room(), create.campus(),
                create.weekday(), Time.valueOf(create.startTime()), Time.valueOf(create.endTime()), create.type(),
                normalizedStatus(create.status()), id);
    }

    public void cancel(String id) {
        jdbc.update("UPDATE schedule_items SET status = '已取消' WHERE id = ?", id);
    }

    private static String normalizedStatus(String status) {
        return status == null || status.isBlank() ? "待上课" : status;
    }

    private static void addFilter(StringBuilder sql, List<Object> args, String column, String value) {
        if (value != null && !value.isBlank()) {
            sql.append(" AND ").append(column).append(" = ?");
            args.add(value);
        }
    }
}
