package com.example.learnacademy.service.quiz;

import com.example.learnacademy.dto.quiz.QuizDtos.AnswerSaveResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.AttemptStartResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.CompleteSectionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.CompleteSectionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.CreateOptionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.CreateQuestionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.CreateSectionRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.CreateTestRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.LeaderboardResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.LeaderboardRowResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.MarkReviewRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.OptionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.PaletteItemResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.QuestionAttemptResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.QuestionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.ResultResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.ResultSectionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.SaveAnswerRequest;
import com.example.learnacademy.dto.quiz.QuizDtos.SectionQuestionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.SectionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.TabSwitchResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.TestResponse;
import com.example.learnacademy.model.Course;
import com.example.learnacademy.model.User;
import com.example.learnacademy.model.quiz.AttemptAnswer;
import com.example.learnacademy.model.quiz.AttemptStatus;
import com.example.learnacademy.model.quiz.LeaderboardEntry;
import com.example.learnacademy.model.quiz.Question;
import com.example.learnacademy.model.quiz.QuestionOption;
import com.example.learnacademy.model.quiz.QuestionType;
import com.example.learnacademy.model.quiz.QuizTest;
import com.example.learnacademy.model.quiz.SectionProgress;
import com.example.learnacademy.model.quiz.TestAttempt;
import com.example.learnacademy.model.quiz.TestSection;
import com.example.learnacademy.repository.CourseRepository;
import com.example.learnacademy.repository.UserRepository;
import com.example.learnacademy.repository.quiz.AttemptAnswerRepository;
import com.example.learnacademy.repository.quiz.LeaderboardRepository;
import com.example.learnacademy.repository.quiz.QuestionOptionRepository;
import com.example.learnacademy.repository.quiz.QuestionRepository;
import com.example.learnacademy.repository.quiz.QuizTestRepository;
import com.example.learnacademy.repository.quiz.SectionProgressRepository;
import com.example.learnacademy.repository.quiz.TestAttemptRepository;
import com.example.learnacademy.repository.quiz.TestSectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class QuizService {

    @Autowired
    private QuizTestRepository quizTestRepository;

    @Autowired
    private TestSectionRepository testSectionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionOptionRepository questionOptionRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private AttemptAnswerRepository attemptAnswerRepository;

    @Autowired
    private SectionProgressRepository sectionProgressRepository;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public TestResponse createTest(Long instructorId, CreateTestRequest request) {
        validateCreateTestRequest(request);

        User creator = getUserOrThrow(instructorId);
        Course course = null;

        if (request.courseId() != null) {
            course = courseRepository.findByIdAndUserId(request.courseId(), instructorId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        }

        QuizTest test = new QuizTest();
        test.setCourse(course);
        test.setTitle(request.title().trim());
        test.setDescription(request.description());
        test.setTimeLimit(request.timeLimit());
        test.setTotalMarks(safeNumber(request.totalMarks()));
        test.setNegativeMarkingEnabled(Boolean.TRUE.equals(request.negativeMarkingEnabled()));
        test.setNegativeMarkValue(Boolean.TRUE.equals(request.negativeMarkingEnabled())
                ? safeNumber(request.negativeMarkValue())
                : BigDecimal.ZERO);
        test.setStrictModeEnabled(Boolean.TRUE.equals(request.strictModeEnabled()));
        test.setMaxTabSwitch(request.maxTabSwitch() == null ? 0 : Math.max(0, request.maxTabSwitch()));
        test.setCreatedBy(creator);

        return toTestResponse(quizTestRepository.save(test));
    }

    @Transactional
    public SectionResponse createSection(Long instructorId, Long testId, CreateSectionRequest request) {
        if (request == null || isBlank(request.title()) || request.sectionOrder() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title and sectionOrder are required");
        }

        QuizTest test = getOwnedTestOrThrow(instructorId, testId);

        TestSection section = new TestSection();
        section.setTest(test);
        section.setTitle(request.title().trim());
        section.setSectionOrder(request.sectionOrder());

        return toSectionResponse(testSectionRepository.save(section), false);
    }

    @Transactional
    public QuestionResponse createQuestion(Long instructorId, Long sectionId, CreateQuestionRequest request) {
        if (request == null || isBlank(request.questionText()) || request.questionType() == null
                || request.marks() == null || request.questionOrder() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "questionText, questionType, marks and questionOrder are required"
            );
        }

        TestSection section = getOwnedSectionOrThrow(instructorId, sectionId);

        Question question = new Question();
        question.setSection(section);
        question.setQuestionText(request.questionText().trim());
        question.setQuestionType(request.questionType());
        question.setMarks(request.marks());
        question.setCorrectAnswer(request.correctAnswer());
        question.setExplanation(request.explanation());
        question.setQuestionOrder(request.questionOrder());

        Question saved = questionRepository.save(question);
        recalculateTotalMarks(section.getTest().getId());
        return toQuestionResponse(saved, true);
    }

    @Transactional
    public OptionResponse createOption(Long instructorId, Long questionId, CreateOptionRequest request) {
        if (request == null || isBlank(request.optionText())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "optionText is required");
        }

        Question question = getOwnedQuestionOrThrow(instructorId, questionId);
        if (question.getQuestionType() != QuestionType.MCQ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Options can only be added to MCQ questions");
        }

        QuestionOption option = new QuestionOption();
        option.setQuestion(question);
        option.setOptionText(request.optionText().trim());
        option.setIsCorrect(Boolean.TRUE.equals(request.isCorrect()));

        return toOptionResponse(questionOptionRepository.save(option));
    }

    @Transactional(readOnly = true)
    public TestResponse getInstructorTest(Long instructorId, Long testId) {
        return toTestResponse(getOwnedTestOrThrow(instructorId, testId));
    }

    @Transactional
    public AttemptStartResponse startTest(Long userId, Long testId) {
        User user = getUserOrThrow(userId);
        QuizTest test = quizTestRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));

        List<TestSection> sections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(testId);
        if (sections.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test has no sections");
        }

        TestAttempt attempt = new TestAttempt();
        attempt.setUser(user);
        attempt.setTest(test);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setScore(BigDecimal.ZERO);
        attempt.setTabSwitchCount(0);
        TestAttempt savedAttempt = testAttemptRepository.save(attempt);

        for (TestSection section : sections) {
            SectionProgress progress = new SectionProgress();
            progress.setAttempt(savedAttempt);
            progress.setSection(section);
            progress.setCompleted(Boolean.FALSE);
            sectionProgressRepository.save(progress);
        }

        return new AttemptStartResponse(
                savedAttempt.getId(),
                savedAttempt.getStatus().name(),
                savedAttempt.getStartedAt(),
                test.getTimeLimit(),
                sections.get(0).getId(),
                (long) sections.size(),
                questionRepository.countBySectionTestId(testId)
        );
    }

    @Transactional
    public SectionQuestionResponse getSectionQuestions(
            Long userId,
            Long testId,
            Long sectionId,
            Long attemptId,
            boolean randomizeQuestions,
            boolean randomizeOptions
    ) {
        TestAttempt attempt = getOwnedAttemptOrThrow(userId, attemptId);
        if (!Objects.equals(attempt.getTest().getId(), testId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt does not belong to this test");
        }
        ensureAttemptWithinTime(attempt);

        TestSection section = testSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        if (!Objects.equals(section.getTest().getId(), testId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section does not belong to this test");
        }

        List<Question> orderedQuestions = questionRepository.findBySectionIdOrderByQuestionOrderAsc(sectionId);
        Map<Long, AttemptAnswer> answersByQuestion = attemptAnswerRepository.findByAttemptId(attemptId).stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), answer -> answer));
        SectionProgress progress = getSectionProgressOrThrow(attemptId, sectionId);

        List<Question> responseQuestions = new ArrayList<>(orderedQuestions);
        if (randomizeQuestions) {
            Collections.shuffle(responseQuestions);
        }

        List<QuestionAttemptResponse> questionResponses = responseQuestions.stream()
                .map(question -> toQuestionAttemptResponse(
                        question,
                        answersByQuestion.get(question.getId()),
                        randomizeOptions
                ))
                .toList();

        List<PaletteItemResponse> palette = orderedQuestions.stream()
                .map(question -> new PaletteItemResponse(
                        question.getId(),
                        determinePaletteStatus(answersByQuestion.get(question.getId()))
                ))
                .toList();

        List<TestSection> allSections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(testId);
        Long nextSectionId = findNextSectionId(allSections, sectionId);
        Long previousSectionId = findPreviousSectionId(allSections, sectionId);

        return new SectionQuestionResponse(
                attempt.getId(),
                testId,
                sectionId,
                section.getTitle(),
                progress.getCompleted(),
                nextSectionId,
                previousSectionId,
                remainingTimeSeconds(attempt),
                questionResponses,
                palette
        );
    }

    @Transactional
    public AnswerSaveResponse saveAnswer(Long userId, Long attemptId, SaveAnswerRequest request) {
        if (request == null || request.questionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "questionId is required");
        }

        TestAttempt attempt = validateActiveAttemptForMutation(userId, attemptId);
        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        ensureSectionEditable(attemptId, question.getSection().getId());

        AttemptAnswer answer = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, request.questionId())
                .orElseGet(() -> {
                    AttemptAnswer created = new AttemptAnswer();
                    created.setAttempt(attempt);
                    created.setQuestion(question);
                    return created;
                });

        updateAnswerFields(answer, question, request.selectedOptionId(), request.textAnswer(), request.markedForReview(), request.answered());
        AttemptAnswer saved = attemptAnswerRepository.save(answer);

        return new AnswerSaveResponse(
                attemptId,
                question.getId(),
                saved.getIsMarkedForReview(),
                saved.getIsAnswered(),
                saved.getSavedAt(),
                determinePaletteStatus(saved)
        );
    }

    @Transactional
    public AnswerSaveResponse markForReview(Long userId, Long attemptId, MarkReviewRequest request) {
        if (request == null || request.questionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "questionId is required");
        }

        TestAttempt attempt = validateActiveAttemptForMutation(userId, attemptId);
        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        ensureSectionEditable(attemptId, question.getSection().getId());

        AttemptAnswer answer = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, request.questionId())
                .orElseGet(() -> {
                    AttemptAnswer created = new AttemptAnswer();
                    created.setAttempt(attempt);
                    created.setQuestion(question);
                    return created;
                });

        answer.setIsMarkedForReview(Boolean.TRUE.equals(request.markedForReview()));
        AttemptAnswer saved = attemptAnswerRepository.save(answer);

        return new AnswerSaveResponse(
                attemptId,
                question.getId(),
                saved.getIsMarkedForReview(),
                saved.getIsAnswered(),
                saved.getSavedAt(),
                determinePaletteStatus(saved)
        );
    }

    @Transactional
    public TabSwitchResponse registerTabSwitch(Long userId, Long attemptId) {
        TestAttempt attempt = validateActiveAttemptForMutation(userId, attemptId);
        attempt.setTabSwitchCount(attempt.getTabSwitchCount() + 1);

        boolean autoSubmitted = Boolean.TRUE.equals(attempt.getTest().getStrictModeEnabled())
                && attempt.getTabSwitchCount() > attempt.getTest().getMaxTabSwitch();

        if (autoSubmitted) {
            TestAttempt submitted = submitAttemptInternal(attempt, true);
            return new TabSwitchResponse(
                    submitted.getId(),
                    submitted.getTabSwitchCount(),
                    true,
                    submitted.getStatus().name()
            );
        }

        TestAttempt saved = testAttemptRepository.save(attempt);
        return new TabSwitchResponse(saved.getId(), saved.getTabSwitchCount(), false, saved.getStatus().name());
    }

    @Transactional
    public CompleteSectionResponse completeSection(Long userId, Long attemptId, CompleteSectionRequest request) {
        if (request == null || request.sectionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sectionId is required");
        }

        TestAttempt attempt = validateActiveAttemptForMutation(userId, attemptId);
        SectionProgress progress = getSectionProgressOrThrow(attemptId, request.sectionId());
        progress.setCompleted(Boolean.TRUE);
        sectionProgressRepository.save(progress);

        Long nextSectionId = findNextSectionId(
                testSectionRepository.findByTestIdOrderBySectionOrderAsc(attempt.getTest().getId()),
                request.sectionId()
        );

        return new CompleteSectionResponse(attemptId, request.sectionId(), true, nextSectionId);
    }

    @Transactional
    public ResultResponse submitTest(Long userId, Long attemptId) {
        TestAttempt attempt = getOwnedAttemptOrThrow(userId, attemptId);
        TestAttempt submitted = submitAttemptInternal(attempt, false);
        return buildResultResponse(submitted);
    }

    @Transactional(readOnly = true)
    public ResultResponse getResult(Long userId, Long testId, Long attemptId) {
        TestAttempt attempt = getOwnedAttemptOrThrow(userId, attemptId);
        if (!Objects.equals(attempt.getTest().getId(), testId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt does not belong to this test");
        }
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test is not submitted yet");
        }
        return buildResultResponse(attempt);
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(Long testId) {
        quizTestRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));

        List<LeaderboardRowResponse> rows = leaderboardRepository.findByTestIdOrderByScoreDescRankAsc(testId).stream()
                .map(entry -> new LeaderboardRowResponse(
                        entry.getUser().getId(),
                        buildUserName(entry.getUser()),
                        entry.getUser().getEmail(),
                        entry.getScore(),
                        entry.getRank()
                ))
                .toList();

        return new LeaderboardResponse(testId, rows);
    }

    private void validateCreateTestRequest(CreateTestRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (isBlank(request.title())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (request.timeLimit() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timeLimit is required");
        }
    }

    private QuizTest getOwnedTestOrThrow(Long instructorId, Long testId) {
        QuizTest test = quizTestRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));
        if (!Objects.equals(test.getCreatedBy().getId(), instructorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Test does not belong to this instructor");
        }
        return test;
    }

    private TestSection getOwnedSectionOrThrow(Long instructorId, Long sectionId) {
        TestSection section = testSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        if (!Objects.equals(section.getTest().getCreatedBy().getId(), instructorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Section does not belong to this instructor");
        }
        return section;
    }

    private Question getOwnedQuestionOrThrow(Long instructorId, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!Objects.equals(question.getSection().getTest().getCreatedBy().getId(), instructorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Question does not belong to this instructor");
        }
        return question;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private TestAttempt getOwnedAttemptOrThrow(Long userId, Long attemptId) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));
        if (!Objects.equals(attempt.getUser().getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Attempt does not belong to this user");
        }
        return attempt;
    }

    private TestAttempt validateActiveAttemptForMutation(Long userId, Long attemptId) {
        TestAttempt attempt = getOwnedAttemptOrThrow(userId, attemptId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt is already submitted");
        }
        ensureAttemptWithinTime(attempt);
        return attempt;
    }

    private void ensureAttemptWithinTime(TestAttempt attempt) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return;
        }

        if (remainingTimeSeconds(attempt) <= 0) {
            submitAttemptInternal(attempt, true);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test time is over. Attempt auto-submitted.");
        }
    }

    private long remainingTimeSeconds(TestAttempt attempt) {
        LocalDateTime expiry = attempt.getStartedAt().plusMinutes(attempt.getTest().getTimeLimit());
        return Math.max(0, Duration.between(LocalDateTime.now(), expiry).getSeconds());
    }

    private void ensureSectionEditable(Long attemptId, Long sectionId) {
        SectionProgress progress = getSectionProgressOrThrow(attemptId, sectionId);
        if (Boolean.TRUE.equals(progress.getCompleted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section is already completed and locked");
        }
    }

    private SectionProgress getSectionProgressOrThrow(Long attemptId, Long sectionId) {
        return sectionProgressRepository.findByAttemptIdAndSectionId(attemptId, sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section progress not found"));
    }

    private void updateAnswerFields(
            AttemptAnswer answer,
            Question question,
            Long selectedOptionId,
            String textAnswer,
            Boolean markedForReview,
            Boolean answered
    ) {
        if (selectedOptionId != null) {
            QuestionOption option = questionOptionRepository.findByIdAndQuestionId(selectedOptionId, question.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid option for question"));
            answer.setSelectedOption(option);
        } else {
            answer.setSelectedOption(null);
        }

        answer.setTextAnswer(textAnswer);
        answer.setIsMarkedForReview(Boolean.TRUE.equals(markedForReview));

        boolean isAnswered = Boolean.TRUE.equals(answered);
        if (question.getQuestionType() == QuestionType.MCQ) {
            isAnswered = isAnswered && answer.getSelectedOption() != null;
        } else {
            isAnswered = isAnswered && !isBlank(textAnswer);
        }
        answer.setIsAnswered(isAnswered);
    }

    private TestAttempt submitAttemptInternal(TestAttempt attempt, boolean autoSubmitted) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return attempt;
        }

        QuizTest test = attempt.getTest();
        List<TestSection> sections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(test.getId());
        Map<Long, AttemptAnswer> answersByQuestionId = attemptAnswerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), answer -> answer, (a, b) -> b));

        BigDecimal score = BigDecimal.ZERO;

        for (TestSection section : sections) {
            for (Question question : questionRepository.findBySectionIdOrderByQuestionOrderAsc(section.getId())) {
                AttemptAnswer answer = answersByQuestionId.get(question.getId());
                if (!isAttempted(answer)) {
                    continue;
                }
                if (isCorrectAnswer(question, answer)) {
                    score = score.add(question.getMarks());
                } else if (Boolean.TRUE.equals(test.getNegativeMarkingEnabled())) {
                    score = score.subtract(safeNumber(test.getNegativeMarkValue()));
                }
            }
        }

        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }

        attempt.setScore(score);
        attempt.setStatus(autoSubmitted ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());

        TestAttempt saved = testAttemptRepository.save(attempt);
        rebuildLeaderboard(saved.getTest().getId(), saved.getUser(), score);
        return saved;
    }

    private ResultResponse buildResultResponse(TestAttempt attempt) {
        List<TestSection> sections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(attempt.getTest().getId());
        Map<Long, AttemptAnswer> answersByQuestionId = attemptAnswerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), answer -> answer, (a, b) -> b));

        int totalQuestions = 0;
        int attempted = 0;
        int correct = 0;
        int wrong = 0;
        List<ResultSectionResponse> sectionResponses = new ArrayList<>();

        for (TestSection section : sections) {
            int sectionTotal = 0;
            int sectionAttempted = 0;
            int sectionCorrect = 0;
            int sectionWrong = 0;

            for (Question question : questionRepository.findBySectionIdOrderByQuestionOrderAsc(section.getId())) {
                totalQuestions++;
                sectionTotal++;
                AttemptAnswer answer = answersByQuestionId.get(question.getId());
                if (!isAttempted(answer)) {
                    continue;
                }
                attempted++;
                sectionAttempted++;
                if (isCorrectAnswer(question, answer)) {
                    correct++;
                    sectionCorrect++;
                } else {
                    wrong++;
                    sectionWrong++;
                }
            }

            sectionResponses.add(new ResultSectionResponse(
                    section.getId(),
                    section.getTitle(),
                    sectionTotal,
                    sectionAttempted,
                    sectionCorrect,
                    sectionWrong
            ));
        }

        BigDecimal percentage = totalQuestions == 0 || attempt.getTest().getTotalMarks().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : attempt.getScore()
                .multiply(BigDecimal.valueOf(100))
                .divide(attempt.getTest().getTotalMarks(), 2, RoundingMode.HALF_UP);

        return new ResultResponse(
                attempt.getId(),
                attempt.getTest().getId(),
                attempt.getStatus().name(),
                totalQuestions,
                attempted,
                correct,
                wrong,
                attempt.getScore(),
                percentage,
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                sectionResponses
        );
    }

    private boolean isAttempted(AttemptAnswer answer) {
        if (answer == null || !Boolean.TRUE.equals(answer.getIsAnswered())) {
            return false;
        }
        return answer.getSelectedOption() != null || !isBlank(answer.getTextAnswer());
    }

    private boolean isCorrectAnswer(Question question, AttemptAnswer answer) {
        if (answer == null) {
            return false;
        }

        return switch (question.getQuestionType()) {
            case MCQ -> answer.getSelectedOption() != null && Boolean.TRUE.equals(answer.getSelectedOption().getIsCorrect());
            case TRUE_FALSE -> normalize(answer.getTextAnswer()).equals(normalize(question.getCorrectAnswer()));
            case FILL -> normalize(answer.getTextAnswer()).equals(normalize(question.getCorrectAnswer()));
        };
    }

    private void rebuildLeaderboard(Long testId, User user, BigDecimal score) {
        LeaderboardEntry entry = leaderboardRepository.findByTestIdAndUserId(testId, user.getId())
                .orElseGet(() -> {
                    LeaderboardEntry created = new LeaderboardEntry();
                    created.setTest(quizTestRepository.findById(testId).orElseThrow());
                    created.setUser(user);
                    created.setRank(0); // temporary non-null value
                    return created;
                });

        entry.setScore(score);
        if (entry.getRank() == null) {
            entry.setRank(0);
        }
        leaderboardRepository.save(entry);

        List<LeaderboardEntry> sorted = leaderboardRepository.findByTestIdOrderByScoreDescRankAsc(testId).stream()
                .sorted(Comparator.comparing(LeaderboardEntry::getScore).reversed()
                        .thenComparing(LeaderboardEntry::getId))
                .toList();

        int rank = 1;
        for (LeaderboardEntry leaderboardEntry : sorted) {
            leaderboardEntry.setRank(rank++);
            leaderboardRepository.save(leaderboardEntry);
        }
    }


    private void recalculateTotalMarks(Long testId) {
        QuizTest test = quizTestRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));

        BigDecimal total = testSectionRepository.findByTestIdOrderBySectionOrderAsc(testId).stream()
                .flatMap(section -> questionRepository.findBySectionIdOrderByQuestionOrderAsc(section.getId()).stream())
                .map(Question::getMarks)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        test.setTotalMarks(total);
        quizTestRepository.save(test);
    }

    private TestResponse toTestResponse(QuizTest test) {
        List<TestSection> sections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(test.getId());
        List<SectionResponse> sectionResponses = sections.stream()
                .map(section -> toSectionResponse(section, true))
                .toList();

        return new TestResponse(
                test.getId(),
                test.getCourse() == null ? null : test.getCourse().getId(),
                test.getTitle(),
                test.getDescription(),
                test.getTimeLimit(),
                test.getTotalMarks(),
                test.getNegativeMarkingEnabled(),
                test.getNegativeMarkValue(),
                test.getStrictModeEnabled(),
                test.getMaxTabSwitch(),
                test.getCreatedBy().getId(),
                test.getCreatedAt(),
                sectionResponses
        );
    }

    private SectionResponse toSectionResponse(TestSection section, boolean includeQuestions) {
        List<QuestionResponse> questions = includeQuestions
                ? questionRepository.findBySectionIdOrderByQuestionOrderAsc(section.getId()).stream()
                .map(question -> toQuestionResponse(question, true))
                .toList()
                : List.of();

        return new SectionResponse(section.getId(), section.getTitle(), section.getSectionOrder(), questions);
    }

    private QuestionResponse toQuestionResponse(Question question, boolean includeOptions) {
        List<OptionResponse> options = includeOptions
                ? questionOptionRepository.findByQuestionId(question.getId()).stream()
                .map(this::toOptionResponse)
                .toList()
                : List.of();

        return new QuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getMarks(),
                question.getExplanation(),
                question.getQuestionOrder(),
                options
        );
    }

    private OptionResponse toOptionResponse(QuestionOption option) {
        return new OptionResponse(option.getId(), option.getOptionText());
    }

    private QuestionAttemptResponse toQuestionAttemptResponse(
            Question question,
            AttemptAnswer answer,
            boolean randomizeOptions
    ) {
        List<QuestionOption> options = new ArrayList<>(questionOptionRepository.findByQuestionId(question.getId()));
        if (randomizeOptions) {
            Collections.shuffle(options);
        }

        return new QuestionAttemptResponse(
                question.getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getMarks(),
                question.getQuestionOrder(),
                answer == null ? null : answer.getTextAnswer(),
                answer == null || answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId(),
                answer != null && Boolean.TRUE.equals(answer.getIsMarkedForReview()),
                answer != null && Boolean.TRUE.equals(answer.getIsAnswered()),
                options.stream().map(this::toOptionResponse).toList()
        );
    }

    private String determinePaletteStatus(AttemptAnswer answer) {
        if (answer == null || !Boolean.TRUE.equals(answer.getIsAnswered())) {
            return Boolean.TRUE.equals(answer != null ? answer.getIsMarkedForReview() : Boolean.FALSE)
                    ? "MARKED_FOR_REVIEW"
                    : "NOT_ANSWERED";
        }

        if (Boolean.TRUE.equals(answer.getIsMarkedForReview())) {
            return "MARKED_FOR_REVIEW";
        }
        return "ANSWERED";
    }

    private Long findNextSectionId(List<TestSection> sections, Long currentSectionId) {
        for (int i = 0; i < sections.size(); i++) {
            if (Objects.equals(sections.get(i).getId(), currentSectionId) && i + 1 < sections.size()) {
                return sections.get(i + 1).getId();
            }
        }
        return null;
    }

    private Long findPreviousSectionId(List<TestSection> sections, Long currentSectionId) {
        for (int i = 0; i < sections.size(); i++) {
            if (Objects.equals(sections.get(i).getId(), currentSectionId) && i - 1 >= 0) {
                return sections.get(i - 1).getId();
            }
        }
        return null;
    }

    private BigDecimal safeNumber(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String buildUserName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isBlank() ? user.getEmail() : full;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
