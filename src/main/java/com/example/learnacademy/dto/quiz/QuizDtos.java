package com.example.learnacademy.dto.quiz;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.example.learnacademy.model.quiz.QuestionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class QuizDtos {

    private QuizDtos() {
    }

    public record CreateTestRequest(
            Long courseId,
            String title,
            String description,
            Integer timeLimit,
            BigDecimal totalMarks,
            @JsonAlias("hasNegativeMarking")
            Boolean negativeMarkingEnabled,
            BigDecimal negativeMarkValue,
            @JsonAlias("isStrictMode")
            Boolean strictModeEnabled,
            @JsonAlias("maxTabSwitches")
            Integer maxTabSwitch
    ) {
    }

    public record CreateSectionRequest(
            String title,
            Integer sectionOrder
    ) {
    }

    public record CreateQuestionRequest(
            String questionText,
            QuestionType questionType,
            BigDecimal marks,
            String correctAnswer,
            String explanation,
            Integer questionOrder
    ) {
    }

    public record CreateOptionRequest(
            String optionText,
            Boolean isCorrect
    ) {
    }

    public record SaveAnswerRequest(
            Long questionId,
            Long selectedOptionId,
            String textAnswer,
            Boolean markedForReview,
            Boolean answered
    ) {
    }

    public record MarkReviewRequest(
            Long questionId,
            Boolean markedForReview
    ) {
    }

    public record CompleteSectionRequest(
            Long sectionId
    ) {
    }

    public record MessageResponse(
            String message
    ) {
    }

    public record TestResponse(
            Long id,
            Long courseId,
            String title,
            String description,
            Integer timeLimit,
            BigDecimal totalMarks,
            Boolean negativeMarkingEnabled,
            BigDecimal negativeMarkValue,
            Boolean strictModeEnabled,
            Integer maxTabSwitch,
            Long createdBy,
            LocalDateTime createdAt,
            List<SectionResponse> sections
    ) {
    }

    public record SectionResponse(
            Long id,
            String title,
            Integer sectionOrder,
            List<QuestionResponse> questions
    ) {
    }

    public record QuestionResponse(
            Long id,
            String questionText,
            QuestionType questionType,
            BigDecimal marks,
            String explanation,
            Integer questionOrder,
            List<OptionResponse> options
    ) {
    }

    public record OptionResponse(
            Long id,
            String optionText
    ) {
    }

    public record AttemptStartResponse(
            Long attemptId,
            String status,
            LocalDateTime startedAt,
            Integer timeLimitMinutes,
            Long firstSectionId,
            Long totalSections,
            Long totalQuestions
    ) {
    }

    public record PaletteItemResponse(
            Long questionId,
            String status
    ) {
    }

    public record SectionQuestionResponse(
            Long attemptId,
            Long testId,
            Long sectionId,
            String sectionTitle,
            Boolean locked,
            Long nextSectionId,
            Long previousSectionId,
            Long remainingTimeSeconds,
            List<QuestionAttemptResponse> questions,
            List<PaletteItemResponse> palette
    ) {
    }

    public record QuestionAttemptResponse(
            Long id,
            String questionText,
            QuestionType questionType,
            BigDecimal marks,
            Integer questionOrder,
            String savedTextAnswer,
            Long savedSelectedOptionId,
            Boolean markedForReview,
            Boolean answered,
            List<OptionResponse> options
    ) {
    }

    public record AnswerSaveResponse(
            Long attemptId,
            Long questionId,
            Boolean markedForReview,
            Boolean answered,
            LocalDateTime savedAt,
            String paletteStatus
    ) {
    }

    public record TabSwitchResponse(
            Long attemptId,
            Integer tabSwitchCount,
            Boolean autoSubmitted,
            String status
    ) {
    }

    public record CompleteSectionResponse(
            Long attemptId,
            Long sectionId,
            Boolean completed,
            Long nextSectionId
    ) {
    }

    public record ResultSectionResponse(
            Long sectionId,
            String sectionTitle,
            Integer totalQuestions,
            Integer attempted,
            Integer correct,
            Integer wrong
    ) {
    }

    public record ResultResponse(
            Long attemptId,
            Long testId,
            String status,
            Integer totalQuestions,
            Integer attempted,
            Integer correct,
            Integer wrong,
            BigDecimal score,
            BigDecimal percentage,
            LocalDateTime startedAt,
            LocalDateTime submittedAt,
            List<ResultSectionResponse> sections
    ) {
    }

    public record LeaderboardRowResponse(
            Long userId,
            String userName,
            String userEmail,
            BigDecimal score,
            Integer rank
    ) {
    }

    public record LeaderboardResponse(
            Long testId,
            List<LeaderboardRowResponse> leaderboard
    ) {
    }
}
