package com.example.learnacademy.controller;

import com.example.learnacademy.model.Course;
import com.example.learnacademy.model.CourseContent;
import com.example.learnacademy.model.UserCourse;
import com.example.learnacademy.repository.CourseContentRepository;
import com.example.learnacademy.repository.CourseRepository;
import com.example.learnacademy.repository.UserCourseRepository;
import com.example.learnacademy.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CourseController {

    @Autowired
    private CourseService service;

    // ================= DASHBOARD =================

    @GetMapping("/dashboard")
    public Map<String,Object> dashboard(@RequestParam Integer userId){

        List<Course> courses = service.getAllCourses();

        Map<String,Object> res = new HashMap<>();
        res.put("userId", userId);
        res.put("courses", courses);

        return res;
    }

    // ================= MY LEARNING =================

    @GetMapping("/mylearning")
    public List<Course> myLearning(@RequestParam Integer userId){
        return service.myLearning(userId);
    }

    // ================= VIEW COURSE =================

    @GetMapping("/viewCourse")
    public Map<String,Object> viewCourse(@RequestParam Integer courseId){
        return service.viewCourse(courseId);
    }

    // ================= CREATE COURSE =================

    @PostMapping("/create-course")
    public Map<String,Object> createCourse(@RequestBody Map<String,Object> body){
        return service.createCourse(body);
    }

    // ================= CREATE COURSE CONTENT =================

    @PostMapping("/create-course-content")
    public Map<String,String> createContent(@RequestBody Map<String,Object> body){
        service.createContent(body);
        return Map.of("success","true");
    }

    // ================= ENROLL =================

    @PostMapping("/enroll")
    public Map<String,String> enroll(@RequestBody Map<String,Integer> body){
        service.enroll(body.get("userId"), body.get("courseId"));
        return Map.of("message","Enrolled successfully!");
    }
}
