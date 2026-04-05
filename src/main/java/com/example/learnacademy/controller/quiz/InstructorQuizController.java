package com.example.learnacademy.controller.quiz;

import com.example.learnacademy.dto.quiz.QuizDtos.CreateOptionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.CreateQuestionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.CreateSectionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.CreateTestRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.OptionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.QuestionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.SectionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.TestResponse;
import com.example.learnacademy.service.AuthenticatedUserService;
import com.example.learnacademy.service.quiz.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
