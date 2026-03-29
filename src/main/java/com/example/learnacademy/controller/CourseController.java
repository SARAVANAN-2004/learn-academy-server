package com.example.learnacademy.controller;

import com.example.learnacademy.model.Course;
import com.example.learnacademy.model.User;
import com.example.learnacademy.repository.UserRepository;
import com.example.learnacademy.service.CourseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CourseController {

    @Autowired
    private CourseService service;

    @Autowired
    private UserRepository userRepo;


    private Long getUserId(Authentication authentication) {

        if (authentication == null) {
            throw new RuntimeException("User not authenticated");
        }

        String email;

        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getId();
    }


    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication authentication) {

        Long userId = getUserId(authentication);

        List<Course> courses = service.getAllCourses(userId);

        Map<String, Object> res = new HashMap<>();
        res.put("userId", userId);
        res.put("courses", courses);

        return res;
    }

    @GetMapping("/mylearning")
    public List<Course> myLearning(Authentication authentication) {

        Long userId = getUserId(authentication);

        return service.myLearning(userId);
    }


    @GetMapping("/viewCourse")
    public Map<String, Object> viewCourse(
            @RequestParam Long courseId,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return service.viewCourse(courseId, userId);
    }


    @PostMapping("/create-course")
    public Map<String, Object> createCourse(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        body.put("userId", userId);

        return service.createCourse(body);
    }


    @PostMapping("/create-course-content")
    public Map<String, String> createContent(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        body.put("userId", userId);

        service.createContent(body);

        Map<String, String> res = new HashMap<>();
        res.put("success", "true");

        return res;
    }

    // ================= ENROLL =================

    @PostMapping("/enroll")
    public Map<String, String> enroll(
            @RequestBody Map<String, Long> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        Long courseId = body.get("courseId");

        service.enroll(userId, courseId);

        Map<String, String> res = new HashMap<>();
        res.put("message", "Enrolled successfully!");

        return res;
    }

    // ================= TOGGLE LESSON =================

    @PostMapping("/toggle-lesson")
    public Map<String, String> toggleLesson(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        Long courseId = Long.valueOf(body.get("courseId").toString());
        String lessonId = body.get("lessonId").toString();
        Boolean completed = Boolean.valueOf(body.get("completed").toString());

        service.toggleLesson(userId, courseId, lessonId, completed);

        Map<String, String> res = new HashMap<>();
        res.put("message", "Updated");

        return res;
    }
}