package com.example.learnacademy.service;

import com.example.learnacademy.model.Course;
import com.example.learnacademy.model.CourseContent;
import com.example.learnacademy.model.UserCourse;
import com.example.learnacademy.repository.CourseContentRepository;
import com.example.learnacademy.repository.CourseRepository;
import com.example.learnacademy.repository.UserCourseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
    JdbcTemplate jdbc;

    // ================= GET ALL COURSES =================

    public List<Course> getAllCourses(){
        return courseRepo.findAll();
    }

    // ================= MY LEARNING =================

    public List<Course> myLearning(Long userId){

        List<UserCourse> uc = userCourseRepo.findByUserId(userId);

        List<Long> ids = uc.stream()
                .map(UserCourse::getCourseId)
                .collect(Collectors.toList());

        return courseRepo.findAllById(ids);
    }

    // ================= VIEW COURSE =================

    public Map<String,Object> viewCourse(Long courseId){

        Course course = courseRepo.findById(courseId).orElseThrow();

        CourseContent content = contentRepo.findByCourseId(courseId);

        Map<String,Object> map = new HashMap<>();
        map.put("courseDetails", course);
        map.put("courseContent", content);

        return map;
    }

    // ================= CREATE COURSE =================

    public Map<String,Object> createCourse(Map<String,Object> b){

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
            rating
        )
        VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
        RETURNING id
        """,
                Long.class,
                b.get("userId"),
                b.get("courseType"),
                b.get("title"),
                b.get("imageUrl"),
                b.get("category"),
                b.get("learnObjectives"),
                b.get("requirements"),
                b.get("whoIsThisFor"),
                b.get("timeCommitment"),
                b.get("instructorName"),
                b.get("originalPrice"),
                b.get("discountedPrice"),
                b.get("rating")
        );

        return Map.of(
                "success", true,
                "courseId", id
        );
    }

    // ================= CREATE COURSE CONTENT =================

    public void createContent(Map<String,Object> body){

        jdbc.update("""
        INSERT INTO course_contents(user_id,course_id,content)
        VALUES(?,?,?)
        """,
                body.get("userId"),
                body.get("courseId"),
                body.toString());
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
}