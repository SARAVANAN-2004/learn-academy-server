package com.example.learnacademy.service;

import com.example.learnacademy.model.Course;
import com.example.learnacademy.model.CourseContent;
import com.example.learnacademy.model.UserCourse;
import com.example.learnacademy.model.LessonProgress;

import com.example.learnacademy.repository.CourseContentRepository;
import com.example.learnacademy.repository.CourseRepository;
import com.example.learnacademy.repository.UserCourseRepository;
import com.example.learnacademy.repository.LessonProgressRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Array;
import java.sql.Connection;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseService {

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


    public List<Course> getAllCourses(Long userId) {


        List<Course> allCourses = courseRepo.findAll();

        // 2. Get enrolled course IDs
        List<UserCourse> enrolledCourses = userCourseRepo.findByUserId(userId);

        Set<Long> enrolledCourseIds = enrolledCourses.stream()
                .map(UserCourse::getCourseId)
                .collect(Collectors.toSet());

        // 3. Filter courses
        return allCourses.stream()
                .filter(course -> !enrolledCourseIds.contains(course.getId()))
                .collect(Collectors.toList());
    }


    public List<Course> myLearning(Long userId){

        List<UserCourse> uc = userCourseRepo.findByUserId(userId);

        List<Long> ids = uc.stream()
                .map(UserCourse::getCourseId)
                .collect(Collectors.toList());

        return courseRepo.findAllById(ids);
    }

    public Map<String,Object> viewCourse(Long courseId, Long userId){

        Course course = courseRepo.findById(courseId).orElseThrow();
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

        Map<String,Object> map = new HashMap<>();
        map.put("courseDetails", course);
        map.put("courseContent", content);
        map.put("progress", progress);

        return map;
    }

    // ================= CREATE COURSE =================

    public Map<String, Object> createCourse(Map<String, Object> b) {

        try {
            Long userId = Long.valueOf(b.get("userId").toString());

            Integer originalPrice = Integer.valueOf(b.get("originalPrice").toString());
            Integer discountedPrice = Integer.valueOf(b.get("discountedPrice").toString());
            Integer rating = Integer.valueOf(b.get("rating").toString());

            // 🔥 Convert List -> SQL Array
            List<String> badgesList = (List<String>) b.get("badges");

            Array badgesArray = jdbc.execute((Connection con) -> {
                return con.createArrayOf("text", badgesList.toArray());
            });

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
                    b.get("courseType"),
                    b.get("title"),
                    b.get("imageUrl"),
                    b.get("category"),
                    b.get("learnObjectives"),
                    b.get("requirements"),
                    b.get("whoIsThisFor"),
                    b.get("timeCommitment"),
                    b.get("instructorName"),
                    originalPrice,
                    discountedPrice,
                    rating,
                    badgesArray
            );

            return Map.of(
                    "success", true,
                    "courseId", id
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    public void createContent(Map<String,Object> body){

        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            Long courseId = Long.valueOf(body.get("courseId").toString());

            ObjectMapper mapper = new ObjectMapper();

            List<Map<String, Object>> sections =
                    (List<Map<String, Object>>) body.get("sections");

            for (int i = 0; i < sections.size(); i++) {

                Map<String, Object> section = sections.get(i);
                List<Map<String, Object>> lessons =
                        (List<Map<String, Object>>) section.get("lessons");

                for (int j = 0; j < lessons.size(); j++) {

                    Map<String, Object> lesson = lessons.get(j);

                    // ✅ Generate unique lesson_id
                    String lessonId = "S" + (i + 1) + "L" + (j + 1);

                    lesson.put("lesson_id", lessonId);
                }
            }

            // ✅ Convert updated sections to JSON
            String json = mapper.writeValueAsString(sections);

            jdbc.update("""
        INSERT INTO course_contents(user_id,course_id,content)
        VALUES(?,?,?::jsonb)
        """,
                    userId,
                    courseId,
                    json);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error inserting course content");
        }
    }

    // ================= ENROLL =================

    public void enroll(Long userId, Long courseId){

        jdbc.update("""
        INSERT INTO user_courses(user_id,course_id)
        VALUES(?,?)
        ON CONFLICT DO NOTHING
        """,
                userId,
                courseId);
    }

    // ================= TOGGLE LESSON =================

    public void toggleLesson(Long userId, Long courseId, String lessonId, Boolean completed){

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

        String inSql = String.join(",", Collections.nCopies(courseIds.size(), "?"));
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
        if (totalLessons != null && totalLessons != 0) {
            avgCompletion = Math.min(100, (completedLessons * 100.0) / totalLessons);
        }

        List<Map<String, Object>> courseAnalytics = jdbc.query("""
        SELECT 
            c.id,
            c.title,
            COUNT(DISTINCT uc.user_id) AS students,
            COUNT(lp.id) FILTER (WHERE lp.completed = true) AS completed_lessons
        FROM courses c
        LEFT JOIN user_courses uc ON c.id = uc.course_id
        LEFT JOIN lesson_progress lp ON c.id = lp.course_id
        WHERE c.user_id = ?
        GROUP BY c.id, c.title
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
        GROUP BY c.id
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
        GROUP BY c.id
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
}
