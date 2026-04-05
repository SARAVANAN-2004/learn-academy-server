package com.example.learnacademy.controller.quiz;

import com.example.learnacademy.dto.quiz.QuizDtos.CreateOptionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.CreateQuestionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.CreateSectionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.CreateTestRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.BulkTestActionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.OptionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.QuestionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.SectionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.TestResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.TestExportRequest;
import com.example.learnacademy.service.AuthenticatedUserService;
import com.example.learnacademy.service.quiz.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor")
public class InstructorQuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @PostMapping("/tests")
    public TestResponse createTest(@RequestBody CreateTestRequest request, Authentication authentication) {
        return quizService.createTest(authenticatedUserService.getCurrentUserId(authentication), request);
    }

    @GetMapping("/tests")
    public List<Map<String, Object>> getTests(Authentication authentication) {
        return quizService.getInstructorTests(authenticatedUserService.getCurrentUserId(authentication));
    }

    @PostMapping("/tests/{testId}/sections")
    public SectionResponse createSection(
            @PathVariable Long testId,
            @RequestBody CreateSectionRequest request,
            Authentication authentication) {
        return quizService.createSection(authenticatedUserService.getCurrentUserId(authentication), testId, request);
    }

    @PostMapping("/sections/{sectionId}/questions")
    public QuestionResponse createQuestion(
            @PathVariable Long sectionId,
            @RequestBody CreateQuestionRequest request,
            Authentication authentication) {
        return quizService.createQuestion(authenticatedUserService.getCurrentUserId(authentication), sectionId, request);
    }

    @PostMapping("/questions/{questionId}/options")
    public OptionResponse createOption(
            @PathVariable Long questionId,
            @RequestBody CreateOptionRequest request,
            Authentication authentication) {
        return quizService.createOption(authenticatedUserService.getCurrentUserId(authentication), questionId, request);
    }

    @GetMapping("/tests/{testId}")
    public TestResponse getTest(@PathVariable Long testId, Authentication authentication) {
        return quizService.getInstructorTest(authenticatedUserService.getCurrentUserId(authentication), testId);
    }

    @PutMapping("/tests/{testId}")
    public Map<String, Object> updateTest(
            @PathVariable Long testId,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        return quizService.updateInstructorTest(authenticatedUserService.getCurrentUserId(authentication), testId, body);
    }

    @DeleteMapping("/tests/{testId}")
    public Map<String, Object> deleteTest(@PathVariable Long testId, Authentication authentication) {
        return quizService.deleteInstructorTest(authenticatedUserService.getCurrentUserId(authentication), testId);
    }

    @DeleteMapping("/tests")
    public Map<String, Object> bulkDeleteTests(
            @RequestBody BulkTestActionRequest request,
            Authentication authentication) {
        return quizService.deleteInstructorTests(
                authenticatedUserService.getCurrentUserId(authentication),
                request == null ? null : request.testIds()
        );
    }

    @PostMapping("/tests/delete")
    public Map<String, Object> bulkDeleteTestsPost(
            @RequestBody BulkTestActionRequest request,
            Authentication authentication) {
        return quizService.deleteInstructorTests(
                authenticatedUserService.getCurrentUserId(authentication),
                request == null ? null : request.testIds()
        );
    }

    @GetMapping("/test-report")
    public List<Map<String, Object>> testReport(Authentication authentication) {
        return quizService.getInstructorTestReports(authenticatedUserService.getCurrentUserId(authentication));
    }

    @DeleteMapping("/tests/{testId}/users/{enrolledUserId}")
    public Map<String, Object> removeUserFromTest(
            @PathVariable Long testId,
            @PathVariable Long enrolledUserId,
            Authentication authentication) {
        return quizService.removeUserFromInstructorTest(
                authenticatedUserService.getCurrentUserId(authentication),
                testId,
                enrolledUserId
        );
    }

    @PostMapping("/tests/export")
    public ResponseEntity<byte[]> exportTests(
            @RequestParam(required = false) String format,
            @RequestBody(required = false) TestExportRequest body,
            Authentication authentication) {
        QuizService.ExportFile exportFile = quizService.exportInstructorTests(
                authenticatedUserService.getCurrentUserId(authentication),
                body == null ? null : body.testIds(),
                resolveFormat(format, body == null ? null : body.format())
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFile.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, exportFile.contentType())
                .body(exportFile.content());
    }

    @PostMapping("/test-report/export")
    public ResponseEntity<byte[]> exportTestReport(
            @RequestParam(required = false) String format,
            @RequestBody(required = false) TestExportRequest body,
            Authentication authentication) {
        QuizService.ExportFile exportFile = quizService.exportInstructorTestReports(
                authenticatedUserService.getCurrentUserId(authentication),
                body == null ? null : body.testIds(),
                resolveFormat(format, body == null ? null : body.format())
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFile.fileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, exportFile.contentType())
                .body(exportFile.content());
    }

    private String resolveFormat(String requestParamFormat, String bodyFormat) {
        if (requestParamFormat != null && !requestParamFormat.isBlank()) {
            return requestParamFormat;
        }
        if (bodyFormat != null && !bodyFormat.isBlank()) {
            return bodyFormat;
        }
        return null;
    }
}
