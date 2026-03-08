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

    // ================= HELPER METHOD =================

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

        return user.getId(); // Long
    }

    // ================= DASHBOARD =================

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication authentication) {

        Long userId = getUserId(authentication);

        List<Course> courses = service.getAllCourses();

        Map<String, Object> res = new HashMap<>();
        res.put("userId", userId);
        res.put("courses", courses);

        return res;
    }

    // ================= MY LEARNING =================

    @GetMapping("/mylearning")
    public List<Course> myLearning(Authentication authentication) {

        Long userId = getUserId(authentication);

        return service.myLearning(userId);
    }

    // ================= VIEW COURSE =================

    @GetMapping("/viewCourse")
    public Map<String, Object> viewCourse(@RequestParam Long courseId) {
        return service.viewCourse(courseId);
    }

    // ================= CREATE COURSE =================

    @PostMapping("/create-course")
    public Map<String, Object> createCourse(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        body.put("userId", userId);

        return service.createCourse(body);
    }

    // ================= CREATE COURSE CONTENT =================

    @PostMapping("/create-course-content")
    public Map<String, String> createContent(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        body.put("userId", userId);

        service.createContent(body);

        return Map.of("success", "true");
    }

    // ================= ENROLL =================

    @PostMapping("/enroll")
    public Map<String, String> enroll(
            @RequestBody Map<String, Long> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        Long courseId = body.get("courseId");

        service.enroll(userId, courseId);

        return Map.of("message", "Enrolled successfully!");
    }
}