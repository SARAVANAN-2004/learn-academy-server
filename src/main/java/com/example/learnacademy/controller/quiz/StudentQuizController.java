package com.example.learnacademy.controller.quiz;

import com.example.learnacademy.dto.quiz.QuizDtos.AnswerSaveResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.AttemptStartResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.CompleteSectionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.CompleteSectionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.LeaderboardResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.MarkReviewRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.ResultResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.SaveAnswerRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.SectionQuestionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.TabSwitchResponse;
import com.example.learnacademy.service.AuthenticatedUserService;
import com.example.learnacademy.service.quiz.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tests")
public class StudentQuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @PostMapping("/{testId}/start")
    public AttemptStartResponse startTest(@PathVariable Long testId, Authentication authentication) {
        return quizService.startTest(authenticatedUserService.getCurrentUserId(authentication), testId);
    }

    @GetMapping("/{testId}/sections/{sectionId}")
    public SectionQuestionResponse getSection(
            @PathVariable Long testId,
            @PathVariable Long sectionId,
            @RequestParam Long attemptId,
            @RequestParam(defaultValue = "false") boolean randomizeQuestions,
            @RequestParam(defaultValue = "false") boolean randomizeOptions,
            Authentication authentication) {
        return quizService.getSectionQuestions(
                authenticatedUserService.getCurrentUserId(authentication),
                testId,
                sectionId,
                attemptId,
                randomizeQuestions,
                randomizeOptions
        );
    }

    @PostMapping("/attempts/{attemptId}/answer")
    public AnswerSaveResponse saveAnswer(
            @PathVariable Long attemptId,
            @RequestBody SaveAnswerRequest request,
            Authentication authentication) {
        return quizService.saveAnswer(authenticatedUserService.getCurrentUserId(authentication), attemptId, request);
    }

    @PostMapping("/attempts/{attemptId}/mark-review")
    public AnswerSaveResponse markReview(
            @PathVariable Long attemptId,
            @RequestBody MarkReviewRequest request,
            Authentication authentication) {
        return quizService.markForReview(authenticatedUserService.getCurrentUserId(authentication), attemptId, request);
    }

    @PostMapping("/attempts/{attemptId}/tab-switch")
    public TabSwitchResponse registerTabSwitch(@PathVariable Long attemptId, Authentication authentication) {
        return quizService.registerTabSwitch(authenticatedUserService.getCurrentUserId(authentication), attemptId);
    }

    @PostMapping("/attempts/{attemptId}/complete-section")
    public CompleteSectionResponse completeSection(
            @PathVariable Long attemptId,
            @RequestBody CompleteSectionRequest request,
            Authentication authentication) {
        return quizService.completeSection(authenticatedUserService.getCurrentUserId(authentication), attemptId, request);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResultResponse submit(@PathVariable Long attemptId, Authentication authentication) {
        return quizService.submitTest(authenticatedUserService.getCurrentUserId(authentication), attemptId);
    }

    @GetMapping("/{testId}/result/{attemptId}")
    public ResultResponse getResult(
            @PathVariable Long testId,
            @PathVariable Long attemptId,
            Authentication authentication) {
        return quizService.getResult(authenticatedUserService.getCurrentUserId(authentication), testId, attemptId);
    }

    @GetMapping("/{testId}/leaderboard")
    public LeaderboardResponse getLeaderboard(@PathVariable Long testId) {
        return quizService.getLeaderboard(testId);
    }
}
