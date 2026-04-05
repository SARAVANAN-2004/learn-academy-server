package com.example.learnacademy.service;

import com.example.learnacademy.model.Course;
import com.example.learnacademy.model.CourseContent;
import com.example.learnacademy.model.LessonProgress;
import com.example.learnacademy.model.UserCourse;
import com.example.learnacademy.repository.CourseContentRepository;
import com.example.learnacademy.repository.CourseRepository;
import com.example.learnacademy.repository.LessonProgressRepository;
import com.example.learnacademy.repository.UserCourseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private static final DateTimeFormatter EXPORT_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    CourseRepository courseRepo;

    @Autowired
    UserCourseRepository userCourseRepo;

    @Autowired
    CourseContentRepository contentRepo;

    @Autowired
    LessonProgressRepository progressRepo;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    public List<Course> getAllCourses(Long userId) {

        List<Course> allCourses = courseRepo.findAll();

        List<UserCourse> enrolledCourses = userCourseRepo.findByUserId(userId);

        Set<Long> enrolledCourseIds = enrolledCourses.stream()
                .map(UserCourse::getCourseId)
                .collect(Collectors.toSet());

        return allCourses.stream()
                .filter(course -> !enrolledCourseIds.contains(course.getId()))
                .collect(Collectors.toList());
    }

    public List<Course> myLearning(Long userId) {

        List<UserCourse> uc = userCourseRepo.findByUserId(userId);

        List<Long> ids = uc.stream()
                .map(UserCourse::getCourseId)
                .collect(Collectors.toList());

        return courseRepo.findAllById(ids);
    }

    public Map<String, Object> viewCourse(Long courseId, Long userId) {

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        CourseContent content = contentRepo.findByCourseId(courseId);

        List<LessonProgress> progressList =
                progressRepo.findByUserIdAndCourseId(userId, courseId);

        List<Map<String, Object>> progress = progressList.stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("lesson_id", p.getLessonId());
                    m.put("completed", p.getCompleted());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> map = new HashMap<>();
        map.put("courseDetails", course);
        map.put("courseContent", content);
        map.put("progress", progress);

        return map;
    }

    public Map<String, Object> createCourse(Map<String, Object> body) {

        try {
            Long userId = requiredLong(body, "userId");

            Array badgesArray = toSqlTextArray(asStringList(body.get("badges")));

            Long id = jdbc.queryForObject("""
        INSERT INTO courses(
            user_id,
            course_type,
            title,
            image_url,
            category,
            description,
            requirements,
            target_audience,
            time_commitment,
            instructor,
            original_price,
            price,
            rating,
            badges
        )
        VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        RETURNING id
        """,
                    Long.class,
                    userId,
                    body.get("courseType"),
                    body.get("title"),
                    body.get("imageUrl"),
                    body.get("category"),
                    firstNonNull(body, "description", "learnObjectives"),
                    body.get("requirements"),
                    firstNonNull(body, "targetAudience", "whoIsThisFor"),
                    body.get("timeCommitment"),
                    firstNonNull(body, "instructor", "instructorName"),
                    toBigDecimal(body.get("originalPrice")),
                    toBigDecimal(firstNonNull(body, "price", "discountedPrice")),
                    toBigDecimal(body.get("rating")),
                    badgesArray
            );

            return Map.of(
                    "success", true,
                    "courseId", id
            );

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to create course", e);
        }
    }

    @Transactional
    public void createContent(Map<String, Object> body) {

        try {
            Long userId = requiredLong(body, "userId");
            Long courseId = requiredLong(body, "courseId");

            getOwnedCourseOrThrow(userId, courseId);
            String json = toNormalizedContentJson(body);

            if (contentRepo.findOptionalByCourseId(courseId).isPresent()) {
                jdbc.update("""
                UPDATE course_contents
                SET user_id = ?, content = ?::jsonb
                WHERE course_id = ?
                """,
                        userId,
                        json,
                        courseId);
            } else {
                jdbc.update("""
                INSERT INTO course_contents(user_id, course_id, content)
                VALUES(?, ?, ?::jsonb)
                """,
                        userId,
                        courseId,
                        json);
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error inserting course content", e);
        }
    }

    public void enroll(Long userId, Long courseId) {

        jdbc.update("""
        INSERT INTO user_courses(user_id,course_id)
        VALUES(?,?)
        ON CONFLICT DO NOTHING
        """,
                userId,
                courseId);
    }

    public void toggleLesson(Long userId, Long courseId, String lessonId, Boolean completed) {

        LessonProgress progress = progressRepo
                .findByUserIdAndCourseIdAndLessonId(userId, courseId, lessonId)
                .orElse(new LessonProgress());

        progress.setUserId(userId);
        progress.setCourseId(courseId);
        progress.setLessonId(lessonId);
        progress.setCompleted(completed);
        progress.setCompletedAt(LocalDateTime.now());

        progressRepo.save(progress);
    }

    public Map<String, Object> instructorDashboard(Long instructorId) {

        Map<String, Object> result = new HashMap<>();

        List<Course> courses = courseRepo.findByUserId(instructorId);

        List<Long> courseIds = courses.stream()
                .map(Course::getId)
                .toList();

        result.put("totalCourses", courses.size());

        if (courseIds.isEmpty()) {
            result.put("totalStudents", 0);
            result.put("totalLessons", 0);
            result.put("completedLessons", 0);
            result.put("avgCompletion", 0);
            result.put("courseAnalytics", List.of());
            result.put("studentGrowth", List.of());
            result.put("engagementTrend", List.of());
            result.put("growthRate", 0);
            result.put("peakActivityTime", null);
            result.put("topCourse", null);
            result.put("topCourses", List.of());
            return result;
        }

        String inSql = buildInClause(courseIds.size());
        Object[] params = courseIds.toArray();

        Integer totalStudents = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_courses WHERE course_id IN (" + inSql + ")",
                params,
                Integer.class
        );

        Integer totalLessons = jdbc.queryForObject(
                """
                SELECT COALESCE(SUM(
                    CASE
                        WHEN jsonb_typeof(content) = 'array' THEN (
                            SELECT COALESCE(SUM(
                                CASE
                                    WHEN jsonb_typeof(section->'lessons') = 'array'
                                    THEN jsonb_array_length(section->'lessons')
                                    ELSE 0
                                END
                            ), 0)
                            FROM jsonb_array_elements(content) AS section
                        )
                        WHEN jsonb_typeof(content) = 'object'
                             AND jsonb_typeof(content->'sections') = 'array' THEN (
                            SELECT COALESCE(SUM(
                                CASE
                                    WHEN jsonb_typeof(section->'lessons') = 'array'
                                    THEN jsonb_array_length(section->'lessons')
                                    ELSE 0
                                END
                            ), 0)
                            FROM jsonb_array_elements(content->'sections') AS section
                        )
                        ELSE 0
                    END
                ), 0)
                FROM course_contents
                WHERE course_id IN (""" + inSql + ")",
                params,
                Integer.class
        );

        Integer completedLessons = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lesson_progress WHERE course_id IN (" + inSql + ") AND completed = true",
                params,
                Integer.class
        );

        totalStudents = totalStudents == null ? 0 : totalStudents;
        totalLessons = totalLessons == null ? 0 : totalLessons;
        completedLessons = completedLessons == null ? 0 : completedLessons;

        double avgCompletion = 0;
        if (totalLessons != 0) {
            avgCompletion = Math.min(100, (completedLessons * 100.0) / totalLessons);
        }

        List<Map<String, Object>> courseAnalytics = jdbc.query("""
        SELECT
            c.id,
            c.title,
            COALESCE((
                SELECT COUNT(DISTINCT uc.user_id)
                FROM user_courses uc
                WHERE uc.course_id = c.id
            ), 0) AS students,
            COALESCE((
                SELECT COUNT(*)
                FROM lesson_progress lp
                WHERE lp.course_id = c.id AND lp.completed = true
            ), 0) AS completed_lessons
        FROM courses c
        WHERE c.user_id = ?
        ORDER BY c.id
    """, (rs, rowNum) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("courseId", rs.getLong("id"));
            m.put("title", rs.getString("title"));
            m.put("students", rs.getInt("students"));
            m.put("completedLessons", rs.getInt("completed_lessons"));
            return m;
        }, instructorId);

        List<Integer> studentGrowth = jdbc.query(
                "SELECT COUNT(*) FROM user_courses WHERE course_id IN (" + inSql + ") " +
                        "AND DATE(enrolled_at) >= CURRENT_DATE - INTERVAL '6 days' " +
                        "GROUP BY DATE(enrolled_at) ORDER BY DATE(enrolled_at)",
                params,
                (rs, rowNum) -> rs.getInt(1)
        );

        List<Integer> engagementTrend = jdbc.query(
                "SELECT COUNT(*) FROM lesson_progress WHERE course_id IN (" + inSql + ") " +
                        "AND completed = true AND completed_at >= CURRENT_DATE - INTERVAL '6 days' " +
                        "GROUP BY DATE(completed_at) ORDER BY DATE(completed_at)",
                params,
                (rs, rowNum) -> rs.getInt(1)
        );

        double growthRate = 0;
        if (studentGrowth.size() > 1) {
            growthRate = ((studentGrowth.get(studentGrowth.size() - 1) - studentGrowth.get(0)) * 100.0)
                    / Math.max(1, studentGrowth.get(0));
        }

        String peakActivityTime = jdbc.query(
                "SELECT TO_CHAR(completed_at, 'HH12:MI AM') AS time " +
                        "FROM lesson_progress WHERE course_id IN (" + inSql + ") " +
                        "AND completed = true GROUP BY time ORDER BY COUNT(*) DESC LIMIT 1",
                params,
                rs -> rs.next() ? rs.getString("time") : null
        );

        Map<String, Object> topCourse = jdbc.queryForObject("""
        SELECT c.title, COUNT(uc.user_id) AS students
        FROM courses c
        LEFT JOIN user_courses uc ON c.id = uc.course_id
        WHERE c.user_id = ?
        GROUP BY c.id, c.title
        ORDER BY students DESC
        LIMIT 1
    """, (rs, rowNum) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("title", rs.getString("title"));
            m.put("students", rs.getInt("students"));
            return m;
        }, instructorId);

        List<Map<String, Object>> topCourses = jdbc.query("""
        SELECT c.title, COUNT(uc.user_id) AS students
        FROM courses c
        LEFT JOIN user_courses uc ON c.id = uc.course_id
        WHERE c.user_id = ?
        GROUP BY c.id, c.title
        ORDER BY students DESC
        LIMIT 5
    """, (rs, rowNum) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("title", rs.getString("title"));
            m.put("students", rs.getInt("students"));
            return m;
        }, instructorId);

        result.put("totalStudents", totalStudents);
        result.put("totalLessons", totalLessons);
        result.put("completedLessons", completedLessons);
        result.put("avgCompletion", avgCompletion);
        result.put("courseAnalytics", courseAnalytics);
        result.put("studentGrowth", studentGrowth);
        result.put("engagementTrend", engagementTrend);
        result.put("growthRate", growthRate);
        result.put("peakActivityTime", peakActivityTime);
        result.put("topCourse", topCourse);
        result.put("topCourses", topCourses);

        return result;
    }

    public List<Map<String, Object>> getInstructorCourses(Long instructorId) {
        List<Course> courses = courseRepo.findByUserIdOrderByIdDesc(instructorId);
        return buildInstructorCourseDetails(courses);
    }

    public Map<String, Object> getInstructorCourse(Long instructorId, Long courseId) {
        Course course = getOwnedCourseOrThrow(instructorId, courseId);
        return buildInstructorCourseDetails(List.of(course)).get(0);
    }

    public Map<String, Object> getInstructorCourseContent(Long instructorId, Long courseId) {
        getOwnedCourseOrThrow(instructorId, courseId);
        CourseContent content = contentRepo.findOptionalByCourseId(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course content not found"));
        return buildCourseContentResponse(content);
    }

    @Transactional
    public Map<String, Object> updateInstructorCourse(Long instructorId, Long courseId, Map<String, Object> body) {
        Course course = getOwnedCourseOrThrow(instructorId, courseId);
        applyCourseUpdates(course, body);
        Course savedCourse = courseRepo.save(course);
        return buildInstructorCourseDetails(List.of(savedCourse)).get(0);
    }

    @Transactional
    public Map<String, Object> updateInstructorCourseContent(Long instructorId, Long courseId, Map<String, Object> body) {
        Course course = getOwnedCourseOrThrow(instructorId, courseId);
        String json = toNormalizedContentJson(body);

        if (contentRepo.findOptionalByCourseId(courseId).isPresent()) {
            jdbc.update("""
            UPDATE course_contents
            SET user_id = ?, content = ?::jsonb
            WHERE course_id = ?
            """,
                    instructorId,
                    json,
                    courseId);
        } else {
            jdbc.update("""
            INSERT INTO course_contents(user_id, course_id, content)
            VALUES(?, ?, ?::jsonb)
            """,
                    instructorId,
                    courseId,
                    json);
        }

        return buildInstructorCourseDetails(List.of(course)).get(0);
    }

    @Transactional
    public Map<String, Object> deleteInstructorCourse(Long instructorId, Long courseId) {
        return deleteInstructorCourses(instructorId, List.of(courseId));
    }

    @Transactional
    public Map<String, Object> deleteInstructorCourses(Long instructorId, List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "courseIds is required");
        }

        List<Long> uniqueCourseIds = courseIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uniqueCourseIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "courseIds is required");
        }

        List<Course> ownedCourses = courseRepo.findByIdInAndUserId(uniqueCourseIds, instructorId);
        if (ownedCourses.size() != uniqueCourseIds.size()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "One or more courses do not belong to this user");
        }

        progressRepo.deleteByCourseIdIn(uniqueCourseIds);
        userCourseRepo.deleteByCourseIdIn(uniqueCourseIds);
        contentRepo.deleteByCourseIdIn(uniqueCourseIds);
        courseRepo.deleteAll(ownedCourses);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", uniqueCourseIds.size() == 1
                ? "Course deleted successfully"
                : "Courses deleted successfully");
        result.put("deletedCourseIds", uniqueCourseIds);
        result.put("deletedCount", uniqueCourseIds.size());
        return result;
    }

    public ExportFile exportInstructorCourses(Long instructorId, List<Long> requestedCourseIds, String format) {
        List<Course> courses = getCoursesForExport(instructorId, requestedCourseIds);
        List<Map<String, Object>> courseDetails = buildInstructorCourseDetails(courses);

        String normalizedFormat = normalizeFormat(format);
        String timestamp = LocalDateTime.now().format(EXPORT_TIMESTAMP);

        return switch (normalizedFormat) {
            case "csv" -> new ExportFile(
                    "courses_" + timestamp + ".csv",
                    "text/csv",
                    buildCsvExport(courseDetails)
            );
            case "xlsx" -> new ExportFile(
                    "courses_" + timestamp + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    buildXlsxExport(courseDetails)
            );
            case "pdf" -> new ExportFile(
                    "courses_" + timestamp + ".pdf",
                    "application/pdf",
                    buildPdfExport(courseDetails)
            );
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported format. Use csv, xlsx, or pdf"
            );
        };
    }

    public List<Map<String, Object>> getInstructorCourseReports(Long instructorId) {
        List<Course> courses = courseRepo.findByUserIdOrderByIdDesc(instructorId);
        return buildCourseReports(courses);
    }

    @Transactional
    public Map<String, Object> removeUserFromInstructorCourse(Long instructorId, Long courseId, Long enrolledUserId) {
        Course course = getOwnedCourseOrThrow(instructorId, courseId);
        progressRepo.deleteByUserIdAndCourseId(enrolledUserId, courseId);
        userCourseRepo.deleteByUserIdAndCourseId(enrolledUserId, courseId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "User removed from course successfully");
        result.put("courseId", course.getId());
        result.put("removedUserId", enrolledUserId);
        return result;
    }

    public ExportFile exportInstructorCourseReports(Long instructorId, List<Long> requestedCourseIds, String format) {
        List<Course> courses = getCoursesForExport(instructorId, requestedCourseIds);
        List<Map<String, Object>> reports = buildCourseReports(courses);

        String normalizedFormat = normalizeFormat(format);
        String timestamp = LocalDateTime.now().format(EXPORT_TIMESTAMP);

        return switch (normalizedFormat) {
            case "csv" -> new ExportFile(
                    "course_report_" + timestamp + ".csv",
                    "text/csv",
                    buildReportCsvExport(reports)
            );
            case "xlsx" -> new ExportFile(
                    "course_report_" + timestamp + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    buildReportXlsxExport(reports)
            );
            case "pdf" -> new ExportFile(
                    "course_report_" + timestamp + ".pdf",
                    "application/pdf",
                    buildReportPdfExport(reports)
            );
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported format. Use csv, xlsx, or pdf"
            );
        };
    }

    private List<Course> getCoursesForExport(Long instructorId, List<Long> requestedCourseIds) {
        if (requestedCourseIds == null || requestedCourseIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "courseIds is required");
        }

        List<Long> uniqueCourseIds = requestedCourseIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Course> courses = courseRepo.findByIdInAndUserId(uniqueCourseIds, instructorId);

        if (courses.size() != uniqueCourseIds.size()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "One or more courses do not belong to this user");
        }

        return courses;
    }

    private List<Map<String, Object>> buildCourseReports(List<Course> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        Map<Long, CourseContent> contentByCourseId = contentRepo.findByCourseIdIn(courseIds).stream()
                .collect(Collectors.toMap(CourseContent::getCourseId, content -> content));

        List<UserCourse> enrollments = userCourseRepo.findByCourseIdIn(courseIds);
        Map<Long, List<UserCourse>> enrollmentsByCourseId = enrollments.stream()
                .collect(Collectors.groupingBy(UserCourse::getCourseId));

        Map<Long, Map<Long, Set<String>>> completedLessonIds = buildCompletedLessonMap(courseIds);
        Map<Long, Map<String, Object>> userDetails = fetchUserDetails(
                enrollments.stream().map(UserCourse::getUserId).distinct().toList()
        );

        List<Map<String, Object>> reports = new ArrayList<>();
        for (Course course : courses) {
            List<SectionDefinition> sections = extractSectionDefinitions(contentByCourseId.get(course.getId()));
            List<UserCourse> courseEnrollments = enrollmentsByCourseId.getOrDefault(course.getId(), List.of());
            List<Map<String, Object>> enrolledUsers = new ArrayList<>();

            for (UserCourse enrollment : courseEnrollments) {
                Map<String, Object> user = userDetails.getOrDefault(enrollment.getUserId(), Map.of());
                Set<String> completedIds = completedLessonIds
                        .getOrDefault(course.getId(), Map.of())
                        .getOrDefault(enrollment.getUserId(), Set.of());

                List<Map<String, Object>> sectionReports = new ArrayList<>();
                int totalCompletedLessons = 0;

                for (SectionDefinition section : sections) {
                    int completedCount = (int) section.lessonIds().stream()
                            .filter(completedIds::contains)
                            .count();
                    totalCompletedLessons += completedCount;

                    Map<String, Object> sectionMap = new LinkedHashMap<>();
                    sectionMap.put("sectionIndex", section.sectionIndex());
                    sectionMap.put("sectionTitle", section.sectionTitle());
                    sectionMap.put("totalLessons", section.totalLessons());
                    sectionMap.put("completedLessons", completedCount);
                    sectionReports.add(sectionMap);
                }

                Map<String, Object> enrolledUser = new LinkedHashMap<>();
                enrolledUser.put("userId", enrollment.getUserId());
                enrolledUser.put("userName", buildUserDisplayName(user));
                enrolledUser.put("userEmail", user.get("email"));
                enrolledUser.put("enrolledAt", enrollment.getEnrolledAt());
                enrolledUser.put("totalCompletedLessons", totalCompletedLessons);
                enrolledUser.put("sections", sectionReports);
                enrolledUsers.add(enrolledUser);
            }

            Map<String, Object> courseReport = new LinkedHashMap<>();
            courseReport.put("courseId", course.getId());
            courseReport.put("courseName", course.getTitle());
            courseReport.put("courseCategory", course.getCategory());
            courseReport.put("totalEnrolledUsers", courseEnrollments.size());
            courseReport.put("sections", sections.stream().map(section -> {
                Map<String, Object> sectionMap = new LinkedHashMap<>();
                sectionMap.put("sectionIndex", section.sectionIndex());
                sectionMap.put("sectionTitle", section.sectionTitle());
                sectionMap.put("totalLessons", section.totalLessons());
                return sectionMap;
            }).toList());
            courseReport.put("enrolledUsers", enrolledUsers);
            reports.add(courseReport);
        }

        return reports;
    }

    private Map<Long, Map<Long, Set<String>>> buildCompletedLessonMap(List<Long> courseIds) {
        Map<Long, Map<Long, Set<String>>> completedLessonIds = new HashMap<>();
        for (LessonProgress progress : progressRepo.findByCourseIdIn(courseIds)) {
            if (!Boolean.TRUE.equals(progress.getCompleted())) {
                continue;
            }
            completedLessonIds
                    .computeIfAbsent(progress.getCourseId(), ignored -> new HashMap<>())
                    .computeIfAbsent(progress.getUserId(), ignored -> new java.util.HashSet<>())
                    .add(progress.getLessonId());
        }
        return completedLessonIds;
    }

    private Map<Long, Map<String, Object>> fetchUserDetails(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        String inSql = buildInClause(userIds.size());
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, email, first_name, last_name FROM users WHERE id IN (" + inSql + ")",
                userIds.toArray()
        );

        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(((Number) row.get("id")).longValue(), row);
        }
        return result;
    }

    private String buildUserDisplayName(Map<String, Object> user) {
        String firstName = user.get("first_name") == null ? "" : String.valueOf(user.get("first_name")).trim();
        String lastName = user.get("last_name") == null ? "" : String.valueOf(user.get("last_name")).trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? String.valueOf(user.getOrDefault("email", "")) : fullName;
    }

    private List<SectionDefinition> extractSectionDefinitions(CourseContent content) {
        if (content == null || content.getContent() == null || content.getContent().isBlank()) {
            return List.of();
        }

        Object parsed = parseJson(content.getContent());
        List<Map<String, Object>> sections = new ArrayList<>();

        if (parsed instanceof List<?> list) {
            sections = objectMapper.convertValue(list, new TypeReference<>() {
            });
        } else if (parsed instanceof Map<?, ?> map && map.get("sections") instanceof List<?> list) {
            sections = objectMapper.convertValue(list, new TypeReference<>() {
            });
        }

        List<SectionDefinition> definitions = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            Map<String, Object> section = sections.get(i);
            List<Map<String, Object>> lessons = objectMapper.convertValue(
                    section.getOrDefault("lessons", List.of()),
                    new TypeReference<>() {
                    }
            );
            List<String> lessonIds = lessons.stream()
                    .map(lesson -> lesson.get("lesson_id"))
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toList();
            String title = firstNonBlank(
                    asString(section.get("title")),
                    asString(section.get("sectionTitle")),
                    asString(section.get("name")),
                    "Section " + (i + 1)
            );
            definitions.add(new SectionDefinition(i + 1, title, lessonIds.size(), lessonIds));
        }
        return definitions;
    }

    private List<Map<String, Object>> buildInstructorCourseDetails(List<Course> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        Map<Long, CourseContent> contentByCourseId = contentRepo.findByCourseIdIn(courseIds).stream()
                .collect(Collectors.toMap(CourseContent::getCourseId, content -> content));
        Map<Long, Integer> studentCounts = fetchStudentCounts(courseIds);
        Map<Long, Integer> completedLessonCounts = fetchCompletedLessonCounts(courseIds);

        List<Map<String, Object>> details = new ArrayList<>();
        for (Course course : courses) {
            CourseContent content = contentByCourseId.get(course.getId());
            details.add(buildInstructorCourseDetail(
                    course,
                    content,
                    studentCounts.getOrDefault(course.getId(), 0),
                    completedLessonCounts.getOrDefault(course.getId(), 0),
                    countLessons(content)
            ));
        }
        return details;
    }

    private Map<String, Object> buildInstructorCourseDetail(
            Course course,
            CourseContent content,
            int studentCount,
            int completedLessonCount,
            int lessonCount) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", course.getId());
        result.put("userId", course.getUserId());
        result.put("courseType", course.getCourseType());
        result.put("title", course.getTitle());
        result.put("imageUrl", course.getImageUrl());
        result.put("category", course.getCategory());
        result.put("description", course.getDescription());
        result.put("requirements", course.getRequirements());
        result.put("targetAudience", course.getTargetAudience());
        result.put("timeCommitment", course.getTimeCommitment());
        result.put("instructor", course.getInstructor());
        result.put("originalPrice", course.getOriginalPrice());
        result.put("price", course.getPrice());
        result.put("rating", course.getRating());
        result.put("badges", course.getBadges() == null ? List.of() : Arrays.asList(course.getBadges()));
        result.put("createdAt", course.getCreatedAt());
        result.put("studentCount", studentCount);
        result.put("completedLessonCount", completedLessonCount);
        result.put("lessonCount", lessonCount);
        result.put("courseContent", content == null ? null : buildCourseContentResponse(content));
        return result;
    }

    private Map<String, Object> buildCourseContentResponse(CourseContent content) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", content.getId());
        response.put("userId", content.getUserId());
        response.put("courseId", content.getCourseId());
        response.put("content", parseJson(content.getContent()));
        response.put("rawContent", content.getContent());
        return response;
    }

    private Map<Long, Integer> fetchStudentCounts(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }

        String inSql = buildInClause(courseIds.size());
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT course_id, COUNT(*) AS student_count FROM user_courses WHERE course_id IN (" + inSql + ") GROUP BY course_id",
                courseIds.toArray()
        );

        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(((Number) row.get("course_id")).longValue(), ((Number) row.get("student_count")).intValue());
        }
        return result;
    }

    private Map<Long, Integer> fetchCompletedLessonCounts(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }

        String inSql = buildInClause(courseIds.size());
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT course_id, COUNT(*) AS completed_count FROM lesson_progress WHERE course_id IN (" + inSql + ") AND completed = true GROUP BY course_id",
                courseIds.toArray()
        );

        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(((Number) row.get("course_id")).longValue(), ((Number) row.get("completed_count")).intValue());
        }
        return result;
    }

    private int countLessons(CourseContent content) {
        if (content == null || content.getContent() == null || content.getContent().isBlank()) {
            return 0;
        }

        Object parsed = parseJson(content.getContent());
        if (parsed instanceof List<?> sections) {
            return countLessonsFromSections(sections);
        }
        if (parsed instanceof Map<?, ?> map && map.get("sections") instanceof List<?> sections) {
            return countLessonsFromSections(sections);
        }
        return 0;
    }

    private int countLessonsFromSections(List<?> sections) {
        int total = 0;
        for (Object sectionObj : sections) {
            if (sectionObj instanceof Map<?, ?> section && section.get("lessons") instanceof List<?> lessons) {
                total += lessons.size();
            }
        }
        return total;
    }

    private Course getOwnedCourseOrThrow(Long instructorId, Long courseId) {
        return courseRepo.findByIdAndUserId(courseId, instructorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private void applyCourseUpdates(Course course, Map<String, Object> body) {
        if (body.containsKey("courseType")) {
            course.setCourseType(asString(body.get("courseType")));
        }
        if (body.containsKey("title")) {
            course.setTitle(asString(body.get("title")));
        }
        if (body.containsKey("imageUrl")) {
            course.setImageUrl(asString(body.get("imageUrl")));
        }
        if (body.containsKey("category")) {
            course.setCategory(asString(body.get("category")));
        }
        if (body.containsKey("description") || body.containsKey("learnObjectives")) {
            course.setDescription(asString(firstNonNull(body, "description", "learnObjectives")));
        }
        if (body.containsKey("requirements")) {
            course.setRequirements(asString(body.get("requirements")));
        }
        if (body.containsKey("targetAudience") || body.containsKey("whoIsThisFor")) {
            course.setTargetAudience(asString(firstNonNull(body, "targetAudience", "whoIsThisFor")));
        }
        if (body.containsKey("timeCommitment")) {
            course.setTimeCommitment(asString(body.get("timeCommitment")));
        }
        if (body.containsKey("instructor") || body.containsKey("instructorName")) {
            course.setInstructor(asString(firstNonNull(body, "instructor", "instructorName")));
        }
        if (body.containsKey("originalPrice")) {
            course.setOriginalPrice(toBigDecimal(body.get("originalPrice")));
        }
        if (body.containsKey("price") || body.containsKey("discountedPrice")) {
            course.setPrice(toBigDecimal(firstNonNull(body, "price", "discountedPrice")));
        }
        if (body.containsKey("rating")) {
            course.setRating(toBigDecimal(body.get("rating")));
        }
        if (body.containsKey("badges")) {
            List<String> badges = asStringList(body.get("badges"));
            course.setBadges(badges == null ? null : badges.toArray(String[]::new));
        }
    }

    private String toNormalizedContentJson(Map<String, Object> body) {
        Object normalizedContent = normalizeContentBody(body);
        try {
            return objectMapper.writeValueAsString(normalizedContent);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid course content payload", e);
        }
    }

    private Object normalizeContentBody(Map<String, Object> body) {
        if (body.containsKey("sections")) {
            List<Map<String, Object>> sections = objectMapper.convertValue(
                    body.get("sections"),
                    new TypeReference<>() {
                    }
            );
            assignLessonIds(sections);
            return sections;
        }

        if (body.containsKey("content")) {
            Object content = body.get("content");
            if (content instanceof List<?>) {
                List<Map<String, Object>> sections = objectMapper.convertValue(content, new TypeReference<>() {
                });
                assignLessonIds(sections);
                return sections;
            }
            if (content instanceof Map<?, ?>) {
                Map<String, Object> contentMap = objectMapper.convertValue(content, new TypeReference<>() {
                });
                if (contentMap.get("sections") instanceof List<?>) {
                    List<Map<String, Object>> sections = objectMapper.convertValue(contentMap.get("sections"), new TypeReference<>() {
                    });
                    assignLessonIds(sections);
                    contentMap.put("sections", sections);
                }
                return contentMap;
            }
            if (content instanceof String contentString) {
                Object parsed = parseJson(contentString);
                if (parsed instanceof List<?> parsedList) {
                    List<Map<String, Object>> sections = objectMapper.convertValue(parsedList, new TypeReference<>() {
                    });
                    assignLessonIds(sections);
                    return sections;
                }
                if (parsed instanceof Map<?, ?> parsedMap) {
                    Map<String, Object> contentMap = objectMapper.convertValue(parsedMap, new TypeReference<>() {
                    });
                    if (contentMap.get("sections") instanceof List<?>) {
                        List<Map<String, Object>> sections = objectMapper.convertValue(contentMap.get("sections"), new TypeReference<>() {
                        });
                        assignLessonIds(sections);
                        contentMap.put("sections", sections);
                    }
                    return contentMap;
                }
            }
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Course content must include sections or content"
        );
    }

    private void assignLessonIds(List<Map<String, Object>> sections) {
        for (int i = 0; i < sections.size(); i++) {
            Map<String, Object> section = sections.get(i);
            List<Map<String, Object>> lessons = objectMapper.convertValue(
                    section.getOrDefault("lessons", List.of()),
                    new TypeReference<>() {
                    }
            );
            for (int j = 0; j < lessons.size(); j++) {
                Map<String, Object> lesson = lessons.get(j);
                lesson.put("lesson_id", "S" + (i + 1) + "L" + (j + 1));
            }
            section.put("lessons", lessons);
        }
    }

    private Object parseJson(String value) {
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (Exception e) {
            return value;
        }
    }

    private byte[] buildCsvExport(List<Map<String, Object>> courseDetails) {
        StringBuilder csv = new StringBuilder();
        csv.append("Course ID,Title,Category,Course Type,Instructor,Price,Original Price,Rating,Students,Lessons,Completed Lessons,Badges,Created At,Content\n");

        for (Map<String, Object> course : courseDetails) {
            csv.append(csvValue(course.get("id"))).append(',')
                    .append(csvValue(course.get("title"))).append(',')
                    .append(csvValue(course.get("category"))).append(',')
                    .append(csvValue(course.get("courseType"))).append(',')
                    .append(csvValue(course.get("instructor"))).append(',')
                    .append(csvValue(course.get("price"))).append(',')
                    .append(csvValue(course.get("originalPrice"))).append(',')
                    .append(csvValue(course.get("rating"))).append(',')
                    .append(csvValue(course.get("studentCount"))).append(',')
                    .append(csvValue(course.get("lessonCount"))).append(',')
                    .append(csvValue(course.get("completedLessonCount"))).append(',')
                    .append(csvValue(course.get("badges"))).append(',')
                    .append(csvValue(course.get("createdAt"))).append(',')
                    .append(csvValue(extractContentForExport(course.get("courseContent"))))
                    .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildXlsxExport(List<Map<String, Object>> courseDetails) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Courses");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            String[] headers = {
                    "Course ID", "Title", "Category", "Course Type", "Instructor", "Price",
                    "Original Price", "Rating", "Students", "Lessons", "Completed Lessons",
                    "Badges", "Created At", "Content"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Map<String, Object> course : courseDetails) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(stringValue(course.get("id")));
                row.createCell(1).setCellValue(stringValue(course.get("title")));
                row.createCell(2).setCellValue(stringValue(course.get("category")));
                row.createCell(3).setCellValue(stringValue(course.get("courseType")));
                row.createCell(4).setCellValue(stringValue(course.get("instructor")));
                row.createCell(5).setCellValue(stringValue(course.get("price")));
                row.createCell(6).setCellValue(stringValue(course.get("originalPrice")));
                row.createCell(7).setCellValue(stringValue(course.get("rating")));
                row.createCell(8).setCellValue(stringValue(course.get("studentCount")));
                row.createCell(9).setCellValue(stringValue(course.get("lessonCount")));
                row.createCell(10).setCellValue(stringValue(course.get("completedLessonCount")));
                row.createCell(11).setCellValue(stringValue(course.get("badges")));
                row.createCell(12).setCellValue(stringValue(course.get("createdAt")));
                row.createCell(13).setCellValue(stringValue(extractContentForExport(course.get("courseContent"))));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate XLSX export", e);
        }
    }

    private byte[] buildPdfExport(List<Map<String, Object>> courseDetails) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(boldFont, 14);
            contentStream.newLineAtOffset(40, 800);
            contentStream.showText("Course Export");
            contentStream.setFont(font, 10);
            contentStream.newLineAtOffset(0, -24);

            float y = 776;
            for (Map<String, Object> course : courseDetails) {
                List<String> lines = buildPdfCourseLines(course);
                for (String line : lines) {
                    if (y <= 50) {
                        contentStream.endText();
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.beginText();
                        contentStream.setFont(font, 10);
                        contentStream.newLineAtOffset(40, 800);
                        y = 800;
                    }
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -14);
                    y -= 14;
                }
            }

            contentStream.endText();
            contentStream.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate PDF export", e);
        }
    }

    private byte[] buildReportCsvExport(List<Map<String, Object>> reports) {
        ReportExportData exportData = flattenReportRows(reports);
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", exportData.headers().stream().map(this::csvValue).toList())).append('\n');

        for (List<Object> row : exportData.rows()) {
            csv.append(String.join(",", row.stream().map(this::csvValue).toList())).append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildReportXlsxExport(List<Map<String, Object>> reports) {
        ReportExportData exportData = flattenReportRows(reports);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Course Report");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < exportData.headers().size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(exportData.headers().get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (List<Object> rowData : exportData.rows()) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < rowData.size(); i++) {
                    row.createCell(i).setCellValue(stringValue(rowData.get(i)));
                }
            }

            for (int i = 0; i < exportData.headers().size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate XLSX report export", e);
        }
    }

    private byte[] buildReportPdfExport(List<Map<String, Object>> reports) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(boldFont, 14);
            contentStream.newLineAtOffset(40, 800);
            contentStream.showText("Course Enrollment Report");
            contentStream.setFont(font, 10);
            contentStream.newLineAtOffset(0, -24);

            float y = 776;
            for (Map<String, Object> report : reports) {
                List<String> lines = buildReportPdfLines(report);
                for (String line : lines) {
                    if (y <= 50) {
                        contentStream.endText();
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.beginText();
                        contentStream.setFont(font, 10);
                        contentStream.newLineAtOffset(40, 800);
                        y = 800;
                    }
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -14);
                    y -= 14;
                }
            }

            contentStream.endText();
            contentStream.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate PDF report export", e);
        }
    }

    private ReportExportData flattenReportRows(List<Map<String, Object>> reports) {
        int maxSections = reports.stream()
                .map(report -> (List<?>) report.getOrDefault("sections", List.of()))
                .mapToInt(List::size)
                .max()
                .orElse(0);

        List<String> headers = new ArrayList<>(List.of(
                "Course Name",
                "Total Enrolled Users",
                "User ID",
                "User Name",
                "User Email",
                "Enrolled At"
        ));

        for (int i = 1; i <= maxSections; i++) {
            headers.add("Section " + i + " Name");
            headers.add("Section " + i + " Total Lessons");
            headers.add("Section " + i + " Completed Lessons");
        }

        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> report : reports) {
            List<Map<String, Object>> sections = castListOfMaps(report.get("sections"));
            List<Map<String, Object>> enrolledUsers = castListOfMaps(report.get("enrolledUsers"));

            for (Map<String, Object> enrolledUser : enrolledUsers) {
                List<Map<String, Object>> userSections = castListOfMaps(enrolledUser.get("sections"));
                List<Object> row = new ArrayList<>();
                row.add(report.get("courseName"));
                row.add(report.get("totalEnrolledUsers"));
                row.add(enrolledUser.get("userId"));
                row.add(enrolledUser.get("userName"));
                row.add(enrolledUser.get("userEmail"));
                row.add(enrolledUser.get("enrolledAt"));

                for (int i = 0; i < maxSections; i++) {
                    Map<String, Object> courseSection = i < sections.size() ? sections.get(i) : Map.of();
                    Map<String, Object> userSection = i < userSections.size() ? userSections.get(i) : Map.of();
                    row.add(courseSection.getOrDefault("sectionTitle", ""));
                    row.add(courseSection.getOrDefault("totalLessons", ""));
                    row.add(userSection.getOrDefault("completedLessons", ""));
                }

                rows.add(row);
            }
        }

        return new ReportExportData(headers, rows);
    }

    private List<String> buildReportPdfLines(Map<String, Object> report) {
        List<String> lines = new ArrayList<>();
        lines.addAll(wrapText(
                "Course: " + stringValue(report.get("courseName")) +
                        " | Total Enrolled Users: " + stringValue(report.get("totalEnrolledUsers")),
                95
        ));

        List<Map<String, Object>> enrolledUsers = castListOfMaps(report.get("enrolledUsers"));
        for (Map<String, Object> enrolledUser : enrolledUsers) {
            lines.addAll(wrapText(
                    "User: " + stringValue(enrolledUser.get("userName")) +
                            " | Email: " + stringValue(enrolledUser.get("userEmail")) +
                            " | Enrolled At: " + stringValue(enrolledUser.get("enrolledAt")),
                    95
            ));
            for (Map<String, Object> section : castListOfMaps(enrolledUser.get("sections"))) {
                lines.addAll(wrapText(
                        "  " + stringValue(section.get("sectionTitle")) +
                                " - Total Lessons: " + stringValue(section.get("totalLessons")) +
                                ", Completed Lessons: " + stringValue(section.get("completedLessons")),
                        95
                ));
            }
            lines.add(" ");
        }

        lines.add(" ");
        return lines;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castListOfMaps(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private List<String> buildPdfCourseLines(Map<String, Object> course) {
        List<String> rawLines = List.of(
                "Course: " + stringValue(course.get("title")),
                "ID: " + stringValue(course.get("id")) + " | Category: " + stringValue(course.get("category")),
                "Type: " + stringValue(course.get("courseType")) + " | Instructor: " + stringValue(course.get("instructor")),
                "Price: " + stringValue(course.get("price")) + " | Original: " + stringValue(course.get("originalPrice")),
                "Students: " + stringValue(course.get("studentCount")) + " | Lessons: " + stringValue(course.get("lessonCount")) +
                        " | Completed: " + stringValue(course.get("completedLessonCount")),
                "Badges: " + stringValue(course.get("badges")),
                "Created At: " + stringValue(course.get("createdAt")),
                "Description: " + stringValue(course.get("description")),
                "Requirements: " + stringValue(course.get("requirements")),
                "Target Audience: " + stringValue(course.get("targetAudience")),
                "Content: " + stringValue(extractContentForExport(course.get("courseContent"))),
                " "
        );

        List<String> wrapped = new ArrayList<>();
        for (String line : rawLines) {
            wrapped.addAll(wrapText(line, 95));
        }
        return wrapped;
    }

    private List<String> wrapText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return List.of("");
        }

        List<String> lines = new ArrayList<>();
        String remaining = text;
        while (remaining.length() > maxLength) {
            int breakPoint = remaining.lastIndexOf(' ', maxLength);
            if (breakPoint <= 0) {
                breakPoint = maxLength;
            }
            lines.add(remaining.substring(0, breakPoint));
            remaining = remaining.substring(breakPoint).trim();
        }
        lines.add(remaining);
        return lines;
    }

    private Object extractContentForExport(Object courseContent) {
        if (courseContent instanceof Map<?, ?> map) {
            return map.get("content");
        }
        return courseContent;
    }

    private String csvValue(Object value) {
        String text = stringValue(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            if (value instanceof Map<?, ?> || value instanceof List<?>) {
                return objectMapper.writeValueAsString(value);
            }
        } catch (JsonProcessingException ignored) {
        }
        return String.valueOf(value);
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "format is required");
        }
        return format.trim().toLowerCase(Locale.ROOT);
    }

    private String buildInClause(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }

    private Array toSqlTextArray(List<String> values) {
        if (values == null) {
            return null;
        }
        return jdbc.execute((Connection con) -> con.createArrayOf("text", values.toArray()));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long requiredLong(Map<String, Object> body, String key) {
        if (!body.containsKey(key) || body.get(key) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " is required");
        }
        return Long.valueOf(String.valueOf(body.get(key)));
    }

    private Object firstNonNull(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            if (body.containsKey(key)) {
                return body.get(key);
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private List<String> asStringList(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toList();
        }
        if (value instanceof String[] array) {
            return Arrays.asList(array);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "badges must be an array of strings");
    }

    private record SectionDefinition(int sectionIndex, String sectionTitle, int totalLessons, List<String> lessonIds) {
    }

    private record ReportExportData(List<String> headers, List<List<Object>> rows) {
    }

    public record ExportFile(String fileName, String contentType, byte[] content) {
    }
}
