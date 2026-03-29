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
}