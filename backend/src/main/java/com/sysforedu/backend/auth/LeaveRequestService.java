package com.sysforedu.backend.auth;

import com.sysforedu.backend.business.BusinessDataRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LeaveRequestService {
    private final JdbcTemplate jdbc;
    private final BusinessDataRepository businessData;

    public LeaveRequestService(JdbcTemplate jdbc, BusinessDataRepository businessData) {
        this.jdbc = jdbc;
        this.businessData = businessData;
    }

    public List<LeaveRequestView> list(AuthPrincipal principal) {
        if (principal.isAdmin()) {
            return query("SELECT * FROM leave_requests ORDER BY created_at DESC");
        }
        if ("STUDENT".equals(principal.role())) {
            return query("SELECT * FROM leave_requests WHERE requester_person_id = ? ORDER BY created_at DESC",
                    principal.personId());
        }
        List<String> taughtClassIds = businessData.findAll("classes").stream()
                .filter(record -> principal.name().equals(record.get("teacher")))
                .map(record -> String.valueOf(record.get("id"))).toList();
        return query("SELECT * FROM leave_requests ORDER BY created_at DESC").stream()
                .filter(request -> principal.personId().equals(request.teacherId())
                        || taughtClassIds.contains(request.classId()))
                .toList();
    }

    @Transactional
    public LeaveRequestView create(AuthPrincipal principal, AuthRequests.CreateLeave request) {
        boolean studentLeave = "STUDENT_LEAVE".equals(request.type());
        if (studentLeave && !"STUDENT".equals(principal.role())) {
            throw forbidden("只有学生可以提交学生请假申请");
        }
        if (!studentLeave && !"TEACHER".equals(principal.role())) {
            throw forbidden("只有教师可以提交教师请假申请");
        }
        if (request.leaveDate() == null || request.leaveDate().isBefore(LocalDate.now())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "请假日期不能早于今天");
        }

        String studentId = studentLeave ? principal.personId() : null;
        String teacherId = studentLeave ? null : principal.personId();
        String classId = request.classId();
        if (studentLeave) {
            if (classId == null || classId.isBlank()
                    || !businessData.findById("class-memberships", classId)
                    .map(record -> record.get("studentIds") instanceof List<?> ids
                            && ids.contains(principal.personId())).orElse(false)) {
                throw forbidden("只能为自己所在的班级提交请假申请");
            }
        } else {
            classId = null;
        }
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO leave_requests
                (id, request_type, requester_role, requester_person_id, student_id, teacher_id,
                 class_id, leave_date, reason, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                """, id, request.type(), principal.role(), principal.personId(), studentId, teacherId,
                classId, Date.valueOf(request.leaveDate()), request.reason().trim());
        return findById(id);
    }

    @Transactional
    public LeaveRequestView review(AuthPrincipal principal, String id, AuthRequests.ReviewLeave request) {
        if (!principal.isAdmin() && !"TEACHER".equals(principal.role())) {
            throw forbidden("学生不能审批请假申请");
        }
        LeaveRequestView leave = findById(id);
        if (!principal.isAdmin()) {
            if (!"TEACHER".equals(principal.role()) || !"STUDENT_LEAVE".equals(leave.type())
                    || !teachesClass(principal, leave.classId())) {
                throw forbidden("教师只能审批本人所教班级的学生请假");
            }
        }
        if (!"PENDING".equals(leave.status())) {
            throw new AuthException(HttpStatus.CONFLICT, "该请假申请已处理");
        }
        jdbc.update("""
                UPDATE leave_requests SET status = ?, reviewed_by = ?, review_note = ?
                WHERE id = ? AND status = 'PENDING'
                """, request.status(), principal.username(),
                request.note() == null ? "" : request.note().trim(), id);
        return findById(id);
    }

    private boolean teachesClass(AuthPrincipal principal, String classId) {
        return businessData.findById("classes", classId)
                .map(record -> principal.name().equals(record.get("teacher"))).orElse(false);
    }

    private LeaveRequestView findById(String id) {
        return query("SELECT * FROM leave_requests WHERE id = ?", id).stream().findFirst()
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "请假申请不存在"));
    }

    private List<LeaveRequestView> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> new LeaveRequestView(rs.getString("id"),
                rs.getString("request_type"), rs.getString("requester_role"),
                rs.getString("requester_person_id"), rs.getString("student_id"),
                rs.getString("teacher_id"), rs.getString("class_id"),
                rs.getDate("leave_date").toLocalDate(), rs.getString("reason"), rs.getString("status"),
                rs.getString("reviewed_by"), rs.getString("review_note"),
                rs.getTimestamp("created_at").toLocalDateTime()), args);
    }

    private static AuthException forbidden(String message) {
        return new AuthException(HttpStatus.FORBIDDEN, message);
    }

    public record LeaveRequestView(String id, String type, String requesterRole, String requesterPersonId,
                                   String studentId, String teacherId, String classId, LocalDate leaveDate,
                                   String reason, String status, String reviewedBy, String reviewNote,
                                   LocalDateTime createdAt) {
    }
}
