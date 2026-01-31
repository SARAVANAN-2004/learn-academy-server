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

    @Autowired CourseRepository courseRepo;
    @Autowired UserCourseRepository userCourseRepo;
    @Autowired CourseContentRepository contentRepo;
    @Autowired JdbcTemplate jdbc;

    public List<Course> getAllCourses(){
        return courseRepo.findAll();
    }

    public List<Course> myLearning(Integer userId){

        List<UserCourse> uc = userCourseRepo.findByUserId(userId);

        List<Integer> ids = uc.stream()
                .map(UserCourse::getCourseId)
                .collect(Collectors.toList());

        return courseRepo.findAllById(ids);
    }

    public Map<String,Object> viewCourse(Integer courseId){

        Course course = courseRepo.findById(courseId).orElseThrow();

        CourseContent content = contentRepo.findByCourseId(courseId);

        Map<String,Object> map = new HashMap<>();
        map.put("courseDetails", course);
        map.put("courseContent", content);

        return map;
    }

    public Map<String,Object> createCourse(Map<String,Object> b){

        Integer id = jdbc.queryForObject("""
        INSERT INTO courses(user_id,course_type,title,image_url,category,description,
        requirements,target_audience,time_commitment,instructor,original_price,price,rating)
        VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id
        """, Integer.class,
                b.get("userId"), b.get("courseType"), b.get("title"), b.get("imageUrl"),
                b.get("category"), b.get("learnObjectives"), b.get("requirements"),
                b.get("whoIsThisFor"), b.get("timeCommitment"), b.get("instructorName"),
                b.get("originalPrice"), b.get("discountedPrice"), b.get("rating"));

        return Map.of("success",true,"courseId",id);
    }

    public void createContent(Map<String,Object> body){

        jdbc.update("""
        INSERT INTO course_contents(user_id,course_id,content)
        VALUES(?,?,?)
        """, body.get("userId"), body.get("courseId"), body.toString());
    }

    public void enroll(Integer userId,Integer courseId){

        jdbc.update("""
        INSERT INTO user_courses(user_id,course_id)
        VALUES(?,?) ON CONFLICT DO NOTHING
        """, userId,courseId);
    }
}
