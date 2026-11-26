package com.sysforedu.backend.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysforedu.backend.auth.TestAdminMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearItems() throws Exception {
        mockMvc = TestAdminMockMvc.withAdmin(context, mockMvc, objectMapper);
        jdbc.update("DELETE FROM schedule_item_students");
        jdbc.update("DELETE FROM schedule_items");
    }

    @Test
    void rejectsOverlappingSharedResourceButAllowsAdjacentAndOtherTimes() throws Exception {
        create(item("晨曦", "周一", "09:00", "10:00", "老师甲", "教室甲", "班级甲"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime", is("09:00")))
                .andExpect(jsonPath("$.endTime", is("10:00")));

        create(item("冲突课", "周一", "09:30", "10:30", "老师甲", "教室乙", "班级乙"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("时间重叠")))
                .andExpect(jsonPath("$.message", containsString("老师甲")));

        create(item("教室冲突", "周一", "09:15", "09:45", "老师丙", "教室甲", "班级丙"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("教室 教室甲")));

        create(item("相邻课", "周一", "10:00", "11:00", "老师甲", "教室乙", "班级乙"))
                .andExpect(status().isCreated());
        create(item("其他时间", "周一", "11:30", "12:00", "老师甲", "教室甲", "班级甲"))
                .andExpect(status().isCreated());
    }

    @Test
    void roomConflictsAreScopedToTheSameCampus() throws Exception {
        create(item("校区 A 课程", "周一", "09:00", "10:00", "老师甲", "教室 101", "班级甲"))
                .andExpect(status().isCreated());

        create(itemAtCampus("校区 B 课程", "周一", "09:00", "10:00",
                "老师乙", "教室 101", "班级乙", "校区B"))
                .andExpect(status().isCreated());
    }

    @Test
    void canceledItemsDoNotReserveResourcesAndUpdateExcludesItself() throws Exception {
        String id = create(item("待取消", "周二", "09:00", "10:00", "老师乙", "教室乙", "班级乙"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String scheduleId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(id).get("id").asText();
        mockMvc.perform(patch("/api/schedule-items/{id}/cancel", scheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("已取消")));
        create(item("接替课", "周二", "09:30", "10:30", "老师乙", "教室乙", "班级乙"))
                .andExpect(status().isCreated());

        String activeId = jdbc.queryForObject(
                "SELECT id FROM schedule_items WHERE course = '接替课'", String.class);
        mockMvc.perform(put("/api/schedule-items/{id}", activeId)
                        .contentType(APPLICATION_JSON)
                        .content(item("接替课", "周二", "09:30", "10:30", "老师乙", "教室乙", "班级乙")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(activeId)));
    }

    @Test
    void sharedStudentsConflictAcrossDifferentClassesAndStudentIdsAreNormalized() throws Exception {
        create(itemWithStudents("学生课", "周一", "09:00", "10:00", "老师甲", "教室甲", "班级甲",
                "[\"student-1\",\" student-1 \",\"student-2\"]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentIds", hasSize(2)));
        create(itemWithStudents("另一班", "周一", "09:30", "10:30", "老师乙", "教室乙", "班级乙",
                "[\"student-2\"]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("学生 student-2")));
    }

    @Test
    void nullStudentIdsBecomeEmptyAndBlankStudentIdsAreRejected() throws Exception {
        String nullStudents = """
                {"course":"空学生","className":"班级","teacher":"老师","room":"教室","campus":"校区A",
                 "weekday":"周一","startTime":"09:00","endTime":"10:00","type":"常规课","studentIds":null}
                """;
        create(nullStudents)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentIds", hasSize(0)));

        create(itemWithStudents("无效学生", "周二", "09:00", "10:00",
                "老师", "教室", "班级", "[\"  \"]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCanKeepItsOwnStudentsAndAdjacentStudentLessonsAreAllowed() throws Exception {
        String response = create(itemWithStudents("更新课", "周一", "09:00", "10:00", "老师甲", "教室甲", "班级甲",
                "[\"student-1\"]")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asText();

        mockMvc.perform(put("/api/schedule-items/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content(itemWithStudents("更新课", "周一", "09:00", "10:00",
                                "老师乙", "教室乙", "班级乙", "[\"student-1\"]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentIds[0]", is("student-1")));
        create(itemWithStudents("相邻学生课", "周一", "10:00", "11:00",
                "老师丙", "教室丙", "班级丙", "[\"student-1\"]"))
                .andExpect(status().isCreated());
    }

    @Test
    void canceledScheduleRetainsStudentSnapshotButReleasesStudent() throws Exception {
        String response = create(itemWithStudents("待取消", "周二", "09:00", "10:00",
                "老师甲", "教室甲", "班级甲", "[\"student-1\"]"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asText();

        mockMvc.perform(patch("/api/schedule-items/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentIds[0]", is("student-1")));
        create(itemWithStudents("接替课", "周二", "09:30", "10:30",
                "老师乙", "教室乙", "班级乙", "[\"student-1\"]"))
                .andExpect(status().isCreated());
    }

    @Test
    void batchConflictRollsBackAllRows() throws Exception {
        create(item("已有课", "周三", "09:00", "10:00", "老师丙", "教室丙", "班级丙"))
                .andExpect(status().isCreated());
        String batch = """
                {"items":[
                  {"course":"新建一","className":"新班一","teacher":"新老师","room":"新教室","campus":"校区A","weekday":"周四","startTime":"09:00","endTime":"10:00","type":"常规课"},
                  {"course":"冲突项","className":"班级丙","teacher":"老师丙","room":"其他教室","campus":"不同校区","weekday":"周三","startTime":"09:30","endTime":"10:30","type":"补课"}
                ]}
                """;
        mockMvc.perform(post("/api/schedule-items/batch").contentType(APPLICATION_JSON).content(batch))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("已有课")));
        mockMvc.perform(get("/api/schedule-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void batchStudentConflictRollsBackAllRows() throws Exception {
        create(itemWithStudents("已有学生课", "周三", "09:00", "10:00",
                "老师丙", "教室丙", "班级丙", "[\"student-1\"]"))
                .andExpect(status().isCreated());
        String batch = """
                {"items":[
                  {"course":"新建一","className":"新班一","teacher":"新老师","room":"新教室","campus":"校区A","weekday":"周四","startTime":"09:00","endTime":"10:00","type":"常规课","studentIds":["student-2"]},
                  {"course":"冲突项","className":"其他班","teacher":"其他老师","room":"其他教室","campus":"校区A","weekday":"周三","startTime":"09:30","endTime":"10:30","type":"补课","studentIds":["student-1"]}
                ]}
                """;
        mockMvc.perform(post("/api/schedule-items/batch").contentType(APPLICATION_JSON).content(batch))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("student-1")));
        mockMvc.perform(get("/api/schedule-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void concurrentCreatesCannotReserveTheSameResources() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        String request = item("并发课程", "周六", "09:00", "10:00", "同一教师", "同一教室", "同一班级");

        try {
            var first = executor.submit(() -> createConcurrently(request, ready, start));
            var second = executor.submit(() -> createConcurrently(request, ready, start));
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<Integer> statuses = List.of(first.get(), second.get());
            assertThat(statuses, containsInAnyOrder(201, 409));
            mockMvc.perform(get("/api/schedule-items"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void autoPlanAvoidsConflictsAndReturnsFixedResponseContract() throws Exception {
        create(item("已排课", "周一", "09:00", "10:00", "老师丁", "教室丁", "班级丁"))
                .andExpect(status().isCreated());
        String request = """
                {"course":"规划课","className":"班级丁","teacher":"老师丁","room":"教室丁","campus":"校区A",
                 "weeklySessions":2,"durationMinutes":60,"weekdays":["周一","周二"],
                 "startTime":"09:00","endTime":"11:00","type":"常规课"}
                """;
        mockMvc.perform(post("/api/schedule-items/auto-plan").contentType(APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete", is(true)))
                .andExpect(jsonPath("$.message", is("找到 2/2 节")))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].weekday", is("周一")))
                .andExpect(jsonPath("$.items[0].startTime", containsString("10:00")))
                .andExpect(jsonPath("$.items[1].weekday", is("周二")))
                .andExpect(jsonPath("$.items[0].studentIds", hasSize(0)))
                .andExpect(jsonPath("$.explanation", containsString("每 30 分钟")));
    }

    @Test
    void autoPlanCarriesStudentsAndAvoidsTheirExistingTime() throws Exception {
        create(itemWithStudents("学生已排", "周一", "09:00", "10:00",
                "其他老师", "其他教室", "其他班", "[\"student-1\"]"))
                .andExpect(status().isCreated());
        String request = """
                {"course":"规划课","className":"新班","teacher":"新老师","room":"新教室","campus":"校区A",
                 "weeklySessions":1,"durationMinutes":60,"weekdays":["周一"],
                 "startTime":"09:00","endTime":"11:00","type":"常规课","studentIds":["student-1","student-1"]}
                """;
        mockMvc.perform(post("/api/schedule-items/auto-plan").contentType(APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].startTime", containsString("10:00")))
                .andExpect(jsonPath("$.items[0].studentIds", hasSize(1)))
                .andExpect(jsonPath("$.items[0].studentIds[0]", is("student-1")));
    }

    @Test
    void autoPlanExplainsPartialResultsAndDoesNotClaimSuccess() throws Exception {
        create(item("已占用", "周五", "09:00", "11:00", "老师戊", "教室戊", "班级戊"))
                .andExpect(status().isCreated());
        String request = """
                {"course":"规划课","className":"班级戊","teacher":"老师戊","room":"教室戊","campus":"校区A",
                 "weeklySessions":1,"durationMinutes":60,"weekdays":["周五"],
                 "startTime":"09:00","endTime":"10:00","type":"试听课"}
                """;
        mockMvc.perform(post("/api/schedule-items/auto-plan").contentType(APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete", is(false)))
                .andExpect(jsonPath("$.message", is("找到 0/1 节")))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.explanation", containsString("不应保存")));
    }

    @Test
    void invalidInputReturnsReadableBadRequest() throws Exception {
        create(item("坏时间", "周一", "10:00", "09:00", "老师", "教室", "班级"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("endTime")));
        create(item("坏星期", "星期一", "09:00", "10:00", "老师", "教室", "班级"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("weekday")));

        mockMvc.perform(post("/api/schedule-items/auto-plan")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"course":"规划课","className":"班级","teacher":"老师","room":"教室","campus":"校区A",
                                 "weeklySessions":0,"durationMinutes":60,"weekdays":["周一"],
                                 "startTime":"09:00","endTime":"10:00","type":"常规课"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void listSupportsFiltersAndBatchPersistsItems() throws Exception {
        String batch = """
                {"items":[
                  {"course":"数学","className":"一班","teacher":"老师一","room":"101","campus":"东校区","weekday":"周一","startTime":"09:00","endTime":"10:00","type":"常规课"},
                  {"course":"英语","className":"二班","teacher":"老师二","room":"201","campus":"西校区","weekday":"周二","startTime":"10:00","endTime":"11:00","type":"试听课","status":"待上课"}
                ]}
                """;
        mockMvc.perform(post("/api/schedule-items/batch").contentType(APPLICATION_JSON).content(batch))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/schedule-items").param("campus", "东校区").param("course", "数学"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].status", is("待上课")));
    }

    private org.springframework.test.web.servlet.ResultActions create(String body) throws Exception {
        return mockMvc.perform(post("/api/schedule-items").contentType(APPLICATION_JSON).content(body));
    }

    private int createConcurrently(String body, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return create(body).andReturn().getResponse().getStatus();
    }

    private static String item(String course, String weekday, String start, String end,
                               String teacher, String room, String className) {
        return itemAtCampus(course, weekday, start, end, teacher, room, className, "校区A");
    }

    private static String itemAtCampus(String course, String weekday, String start, String end,
                                       String teacher, String room, String className, String campus) {
        return """
                {"course":"%s","className":"%s","teacher":"%s","room":"%s","campus":"%s",
                 "weekday":"%s","startTime":"%s","endTime":"%s","type":"常规课"}
                """.formatted(course, className, teacher, room, campus, weekday, start, end);
    }

    private static String itemWithStudents(String course, String weekday, String start, String end,
                                           String teacher, String room, String className, String studentIds) {
        return """
                {"course":"%s","className":"%s","teacher":"%s","room":"%s","campus":"校区A",
                 "weekday":"%s","startTime":"%s","endTime":"%s","type":"常规课","studentIds":%s}
                """.formatted(course, className, teacher, room, weekday, start, end, studentIds);
    }
}
