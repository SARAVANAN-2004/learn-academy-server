package com.example.learnacademy.controller;

import com.example.learnacademy.model.Course;
import com.example.learnacademy.model.User;
import com.example.learnacademy.repository.UserRepository;
import com.example.learnacademy.service.CourseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/instructor-dashboard")
    public Map<String, Object> instructorDashboard(Authentication authentication) {

        Long userId = getUserId(authentication);

        return service.instructorDashboard(userId);
    }

    @GetMapping("/instructor/courses")
    public List<Map<String, Object>> instructorCourses(Authentication authentication) {
        Long userId = getUserId(authentication);
        return service.getInstructorCourses(userId);
    }

    @GetMapping("/instructor/courses/{courseId}")
    public Map<String, Object> instructorCourse(
            @PathVariable Long courseId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return service.getInstructorCourse(userId, courseId);
    }

    @GetMapping("/instructor/courses/{courseId}/content")
    public Map<String, Object> instructorCourseContent(
            @PathVariable Long courseId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return service.getInstructorCourseContent(userId, courseId);
    }

    @PutMapping("/instructor/courses/{courseId}")
    public Map<String, Object> updateInstructorCourse(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return service.updateInstructorCourse(userId, courseId, body);
    }

    @PutMapping("/instructor/courses/{courseId}/content")
    public Map<String, Object> updateInstructorCourseContent(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return service.updateInstructorCourseContent(userId, courseId, body);
    }

    @DeleteMapping("/instructor/courses/{courseId}")
    public Map<String, Object> deleteInstructorCourse(
            @PathVariable Long courseId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return service.deleteInstructorCourse(userId, courseId);
    }

    @DeleteMapping("/instructor/courses")
    public Map<String, Object> bulkDeleteInstructorCourses(
            @RequestBody Map<String, List<Long>> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return service.deleteInstructorCourses(userId, body.get("courseIds"));
    }

    @PostMapping("/instructor/courses/delete")
    public Map<String, Object> bulkDeleteInstructorCoursesPost(
            @RequestBody Map<String, List<Long>> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return service.deleteInstructorCourses(userId, body.get("courseIds"));
    }

    @PostMapping("/instructor/courses/export")
    public ResponseEntity<byte[]> exportInstructorCourses(
            @RequestParam String format,
            @RequestBody Map<String, List<Long>> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        CourseService.ExportFile exportFile =
                service.exportInstructorCourses(userId, body.get("courseIds"), format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFile.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, exportFile.contentType())
                .body(exportFile.content());
    }

    @GetMapping("/instructor/course-report")
    public List<Map<String, Object>> instructorCourseReport(Authentication authentication) {
        Long userId = getUserId(authentication);
        return service.getInstructorCourseReports(userId);
    }

    @DeleteMapping("/instructor/courses/{courseId}/users/{enrolledUserId}")
    public Map<String, Object> removeUserFromCourse(
            @PathVariable Long courseId,
            @PathVariable Long enrolledUserId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return service.removeUserFromInstructorCourse(userId, courseId, enrolledUserId);
    }

    @PostMapping("/instructor/course-report/export")
    public ResponseEntity<byte[]> exportInstructorCourseReport(
            @RequestParam String format,
            @RequestBody Map<String, List<Long>> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        CourseService.ExportFile exportFile =
                service.exportInstructorCourseReports(userId, body.get("courseIds"), format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFile.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, exportFile.contentType())
                .body(exportFile.content());
    }
}
