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
import com.example.learnacademy.dto.quiz.QuizDtos.SectionAnalysisResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.SectionQuestionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.SectionResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.TabSwitchResponse;
import com.example.learnacademy.dto.quiz.QuizDtos.TestAnalysisResponse;
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
import com.example.learnacademy.model.quiz.TestEnrollment;
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
import com.example.learnacademy.repository.quiz.TestEnrollmentRepository;
import com.example.learnacademy.repository.quiz.TestSectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private TestEnrollmentRepository testEnrollmentRepository;

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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableTests(Long userId) {
        Set<Long> enrolledTestIds = testEnrollmentRepository.findByUserId(userId).stream()
                .map(enrollment -> enrollment.getTest().getId())
                .collect(Collectors.toSet());

        return quizTestRepository.findAll().stream()
                .filter(test -> !enrolledTestIds.contains(test.getId()))
                .map(test -> buildDashboardTestItem(test, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyTests(Long userId) {
        return testEnrollmentRepository.findByUserId(userId).stream()
                .map(TestEnrollment::getTest)
                .map(test -> buildDashboardTestItem(test, true))
                .toList();
    }

    @Transactional
    public void enrollInTest(Long userId, Long testId) {
        User user = getUserOrThrow(userId);
        QuizTest test = quizTestRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));

        if (testEnrollmentRepository.findByUserIdAndTestId(userId, testId).isPresent()) {
            return;
        }

        TestEnrollment enrollment = new TestEnrollment();
        enrollment.setUser(user);
        enrollment.setTest(test);
        testEnrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getInstructorTests(Long instructorId) {
        return quizTestRepository.findAll().stream()
                .filter(test -> Objects.equals(test.getCreatedBy().getId(), instructorId))
                .map(this::buildInstructorTestSummary)
                .toList();
    }

    @Transactional
    public Map<String, Object> updateInstructorTest(Long instructorId, Long testId, Map<String, Object> body) {
        QuizTest test = getOwnedTestOrThrow(instructorId, testId);

        if (body.containsKey("title")) {
            test.setTitle(String.valueOf(body.get("title")));
        }
        if (body.containsKey("description")) {
            test.setDescription(body.get("description") == null ? null : String.valueOf(body.get("description")));
        }
        if (body.containsKey("timeLimit")) {
            test.setTimeLimit(Integer.valueOf(String.valueOf(body.get("timeLimit"))));
        }
        if (body.containsKey("negativeMarkingEnabled") || body.containsKey("hasNegativeMarking")) {
            Object value = body.containsKey("negativeMarkingEnabled")
                    ? body.get("negativeMarkingEnabled")
                    : body.get("hasNegativeMarking");
            test.setNegativeMarkingEnabled(Boolean.valueOf(String.valueOf(value)));
        }
        if (body.containsKey("negativeMarkValue")) {
            test.setNegativeMarkValue(new BigDecimal(String.valueOf(body.get("negativeMarkValue"))));
        }
        if (body.containsKey("strictModeEnabled") || body.containsKey("isStrictMode")) {
            Object value = body.containsKey("strictModeEnabled")
                    ? body.get("strictModeEnabled")
                    : body.get("isStrictMode");
            test.setStrictModeEnabled(Boolean.valueOf(String.valueOf(value)));
        }
        if (body.containsKey("maxTabSwitch") || body.containsKey("maxTabSwitches")) {
            Object value = body.containsKey("maxTabSwitch")
                    ? body.get("maxTabSwitch")
                    : body.get("maxTabSwitches");
            test.setMaxTabSwitch(Integer.valueOf(String.valueOf(value)));
        }

        return buildInstructorTestSummary(quizTestRepository.save(test));
    }

    @Transactional
    public Map<String, Object> deleteInstructorTest(Long instructorId, Long testId) {
        QuizTest test = getOwnedTestOrThrow(instructorId, testId);
        quizTestRepository.delete(test);
        return Map.of("message", "Test deleted successfully", "deletedTestId", testId);
    }

    @Transactional
    public Map<String, Object> deleteInstructorTests(Long instructorId, List<Long> testIds) {
        if (testIds == null || testIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "testIds are required");
        }
        List<QuizTest> ownedTests = getOwnedTestsForAction(instructorId, testIds);
        List<Long> deletedTestIds = ownedTests.stream()
                .map(QuizTest::getId)
                .toList();

        quizTestRepository.deleteAll(ownedTests);

        return Map.of(
                "message", "Tests deleted successfully",
                "deletedTestIds", deletedTestIds,
                "deletedCount", deletedTestIds.size()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> instructorTestDashboard(Long instructorId) {
        List<QuizTest> tests = quizTestRepository.findAll().stream()
                .filter(test -> Objects.equals(test.getCreatedBy().getId(), instructorId))
                .toList();

        int totalTests = tests.size();
        int totalTestEnrollments = tests.stream()
                .mapToInt(test -> (int) testEnrollmentRepository.countByTestId(test.getId()))
                .sum();

        List<TestAttempt> attempts = tests.stream()
                .flatMap(test -> testAttemptRepository.findByTestIdAndStatusOrderByScoreDescStartedAtAsc(
                                test.getId(), AttemptStatus.SUBMITTED)
                        .stream())
                .toList();

        BigDecimal averageScore = attempts.isEmpty()
                ? BigDecimal.ZERO
                : attempts.stream()
                .map(TestAttempt::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(attempts.size()), 2, RoundingMode.HALF_UP);

        List<Map<String, Object>> testAnalytics = tests.stream()
                .map(this::buildInstructorTestSummary)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalTests", totalTests);
        result.put("totalTestEnrollments", totalTestEnrollments);
        result.put("totalSubmittedAttempts", attempts.size());
        result.put("averageTestScore", averageScore);
        result.put("testAnalytics", testAnalytics);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getInstructorTestReports(Long instructorId) {
        return quizTestRepository.findAll().stream()
                .filter(test -> Objects.equals(test.getCreatedBy().getId(), instructorId))
                .map(this::buildInstructorTestReport)
                .toList();
    }

    @Transactional
    public Map<String, Object> removeUserFromInstructorTest(Long instructorId, Long testId, Long enrolledUserId) {
        QuizTest test = getOwnedTestOrThrow(instructorId, testId);
        List<TestAttempt> attempts = testAttemptRepository.findByTestIdAndUserId(testId, enrolledUserId);
        testAttemptRepository.deleteAll(attempts);
        testEnrollmentRepository.deleteByUserIdAndTestId(enrolledUserId, testId);

        return Map.of(
                "message", "User removed from test successfully",
                "testId", test.getId(),
                "removedUserId", enrolledUserId
        );
    }

    @Transactional(readOnly = true)
    public ExportFile exportInstructorTests(Long instructorId, List<Long> testIds, String format) {
        List<Map<String, Object>> rows = getOwnedTestsForExport(instructorId, testIds).stream()
                .map(this::buildInstructorTestSummary)
                .toList();
        return exportTestRows(rows, format, "tests");
    }

    @Transactional(readOnly = true)
    public ExportFile exportInstructorTestReports(Long instructorId, List<Long> testIds, String format) {
        List<Map<String, Object>> reports = getOwnedTestsForExport(instructorId, testIds).stream()
                .map(this::buildInstructorTestReport)
                .toList();
        return exportTestReportRows(reports, format, "test-report");
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
        TestAnalysisResponse analysis = buildTestAnalysis(test, sections);

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
                sectionResponses,
                analysis
        );
    }

    private TestAnalysisResponse buildTestAnalysis(QuizTest test, List<TestSection> sections) {
        List<TestAttempt> attempts = testAttemptRepository.findByTestId(test.getId());
        long totalEnrollments = testEnrollmentRepository.countByTestId(test.getId());
        long totalUsersStarted = attempts.stream()
                .map(attempt -> attempt.getUser().getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        List<TestAttempt> submittedAttempts = attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.SUBMITTED
                        || attempt.getStatus() == AttemptStatus.AUTO_SUBMITTED)
                .toList();

        long totalUsersSubmitted = submittedAttempts.stream()
                .map(attempt -> attempt.getUser().getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        BigDecimal averageScore = submittedAttempts.isEmpty()
                ? BigDecimal.ZERO
                : submittedAttempts.stream()
                .map(TestAttempt::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(submittedAttempts.size()), 2, RoundingMode.HALF_UP);

        BigDecimal highestScore = submittedAttempts.stream()
                .map(TestAttempt::getScore)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        LinkedHashMap<String, Long> questionTypeBreakdown = new LinkedHashMap<>();
        List<SectionAnalysisResponse> sectionAnalysis = new ArrayList<>();
        int totalQuestions = 0;
        BigDecimal totalQuestionMarks = BigDecimal.ZERO;

        for (TestSection section : sections) {
            List<Question> questions = questionRepository.findBySectionIdOrderByQuestionOrderAsc(section.getId());
            totalQuestions += questions.size();

            BigDecimal sectionTotalMarks = questions.stream()
                    .map(Question::getMarks)
                    .map(this::safeNumber)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalQuestionMarks = totalQuestionMarks.add(sectionTotalMarks);

            for (Question question : questions) {
                String type = question.getQuestionType() == null ? "UNKNOWN" : question.getQuestionType().name();
                questionTypeBreakdown.merge(type, 1L, Long::sum);
            }

            sectionAnalysis.add(new SectionAnalysisResponse(
                    section.getId(),
                    section.getTitle(),
                    section.getSectionOrder(),
                    questions.size(),
                    sectionTotalMarks
            ));
        }

        return new TestAnalysisResponse(
                sections.size(),
                totalQuestions,
                totalQuestionMarks,
                totalEnrollments,
                totalUsersStarted,
                totalUsersSubmitted,
                attempts.size(),
                submittedAttempts.size(),
                averageScore,
                highestScore,
                questionTypeBreakdown,
                sectionAnalysis
        );
    }

    private Map<String, Object> buildDashboardTestItem(QuizTest test, boolean enrolled) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", test.getId());
        item.put("title", test.getTitle());
        item.put("description", test.getDescription());
        item.put("timeLimit", test.getTimeLimit());
        item.put("totalMarks", test.getTotalMarks());
        item.put("courseId", test.getCourse() == null ? null : test.getCourse().getId());
        item.put("type", "test");
        item.put("enrolled", enrolled);
        item.put("strictModeEnabled", test.getStrictModeEnabled());
        item.put("negativeMarkingEnabled", test.getNegativeMarkingEnabled());
        item.put("createdAt", test.getCreatedAt());
        return item;
    }

    private Map<String, Object> buildInstructorTestSummary(QuizTest test) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", test.getId());
        item.put("courseId", test.getCourse() == null ? null : test.getCourse().getId());
        item.put("title", test.getTitle());
        item.put("description", test.getDescription());
        item.put("timeLimit", test.getTimeLimit());
        item.put("totalMarks", test.getTotalMarks());
        item.put("negativeMarkingEnabled", test.getNegativeMarkingEnabled());
        item.put("negativeMarkValue", test.getNegativeMarkValue());
        item.put("strictModeEnabled", test.getStrictModeEnabled());
        item.put("maxTabSwitch", test.getMaxTabSwitch());
        item.put("createdAt", test.getCreatedAt());
        item.put("type", "test");
        item.put("enrolledUsers", testEnrollmentRepository.countByTestId(test.getId()));
        return item;
    }

    private Map<String, Object> buildInstructorTestReport(QuizTest test) {
        List<TestEnrollment> enrollments = testEnrollmentRepository.findByTestIdIn(List.of(test.getId()));
        List<Map<String, Object>> enrolledUsers = enrollments.stream()
                .map(enrollment -> {
                    List<TestAttempt> attempts = testAttemptRepository.findByTestIdAndUserId(test.getId(), enrollment.getUser().getId());
                    BigDecimal bestScore = attempts.stream()
                            .map(TestAttempt::getScore)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);
                    TestAttempt bestAttempt = attempts.stream()
                            .max(Comparator
                                    .comparing(TestAttempt::getScore, Comparator.nullsFirst(BigDecimal::compareTo))
                                    .thenComparing(TestAttempt::getSubmittedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                                    .thenComparing(TestAttempt::getStartedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                            .orElse(null);
                    TestAttempt latestAttempt = attempts.stream()
                            .max(Comparator.comparing(TestAttempt::getStartedAt))
                            .orElse(null);

                    Map<String, Object> user = new LinkedHashMap<>();
                    user.put("userId", enrollment.getUser().getId());
                    user.put("userName", buildUserName(enrollment.getUser()));
                    user.put("userEmail", enrollment.getUser().getEmail());
                    user.put("enrolledAt", enrollment.getEnrolledAt());
                    user.put("attemptsCount", attempts.size());
                    user.put("bestScore", bestScore);
                    user.put("latestAttemptStatus", latestAttempt == null ? null : latestAttempt.getStatus().name());
                    user.put("sectionWiseScore", buildSectionWiseScoreSummary(bestAttempt));
                    return user;
                })
                .toList();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("testId", test.getId());
        report.put("testTitle", test.getTitle());
        report.put("courseId", test.getCourse() == null ? null : test.getCourse().getId());
        report.put("type", "test");
        report.put("totalEnrolledUsers", enrollments.size());
        report.put("enrolledUsers", enrolledUsers);
        return report;
    }

    private List<QuizTest> getOwnedTestsForExport(Long instructorId, List<Long> testIds) {
        return getOwnedTestsForAction(instructorId, testIds);
    }

    private List<QuizTest> getOwnedTestsForAction(Long instructorId, List<Long> testIds) {
        List<QuizTest> ownedTests = quizTestRepository.findAll().stream()
                .filter(test -> Objects.equals(test.getCreatedBy().getId(), instructorId))
                .toList();

        if (testIds == null || testIds.isEmpty()) {
            return ownedTests;
        }

        Set<Long> requested = Set.copyOf(testIds);
        List<QuizTest> filtered = ownedTests.stream()
                .filter(test -> requested.contains(test.getId()))
                .toList();

        if (filtered.size() != requested.size()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "One or more tests do not belong to this instructor");
        }
        return filtered;
    }

    private ExportFile exportTestRows(List<Map<String, Object>> rows, String format, String baseName) {
        List<String> headers = List.of(
                "Id", "Title", "CourseId", "TimeLimit", "TotalMarks",
                "NegativeMarking", "NegativeMarkValue", "StrictMode", "MaxTabSwitch",
                "EnrolledUsers", "CreatedAt"
        );

        List<List<Object>> dataRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<Object> dataRow = new ArrayList<>();
            dataRow.add(row.get("id"));
            dataRow.add(row.get("title"));
            dataRow.add(row.get("courseId"));
            dataRow.add(row.get("timeLimit"));
            dataRow.add(row.get("totalMarks"));
            dataRow.add(row.get("negativeMarkingEnabled"));
            dataRow.add(row.get("negativeMarkValue"));
            dataRow.add(row.get("strictModeEnabled"));
            dataRow.add(row.get("maxTabSwitch"));
            dataRow.add(row.get("enrolledUsers"));
            dataRow.add(row.get("createdAt"));
            dataRows.add(dataRow);
        }

        return exportTabularData(headers, dataRows, format, baseName, "Instructor Tests");
    }

    private ExportFile exportTestReportRows(List<Map<String, Object>> reports, String format, String baseName) {
        List<String> headers = List.of(
                "TestId", "TestTitle", "UserId", "UserName", "UserEmail",
                "EnrolledAt", "AttemptsCount", "BestScore", "LatestAttemptStatus", "SectionWiseScore"
        );

        List<List<Object>> dataRows = new ArrayList<>();
        for (Map<String, Object> report : reports) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = (List<Map<String, Object>>) report.get("enrolledUsers");
            for (Map<String, Object> user : users) {
                List<Object> row = new ArrayList<>();
                row.add(report.get("testId"));
                row.add(report.get("testTitle"));
                row.add(user.get("userId"));
                row.add(user.get("userName"));
                row.add(user.get("userEmail"));
                row.add(user.get("enrolledAt"));
                row.add(user.get("attemptsCount"));
                row.add(user.get("bestScore"));
                row.add(user.get("latestAttemptStatus"));
                row.add(user.get("sectionWiseScore"));
                dataRows.add(row);
            }
        }

        return exportTabularData(headers, dataRows, format, baseName, "Test Report");
    }

    private ExportFile exportTabularData(
            List<String> headers,
            List<List<Object>> rows,
            String format,
            String baseName,
            String title
    ) {
        String normalized = format == null ? "" : format.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "csv" -> new ExportFile(baseName + ".csv", "text/csv", buildCsv(headers, rows));
            case "xlsx" -> new ExportFile(
                    baseName + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    buildXlsx(headers, rows)
            );
            case "pdf" -> new ExportFile(baseName + ".pdf", "application/pdf", buildPdf(title, headers, rows));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported format. Use csv, xlsx, or pdf");
        };
    }

    private byte[] buildCsv(List<String> headers, List<List<Object>> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", headers.stream().map(this::csvValue).toList())).append('\n');
        for (List<Object> row : rows) {
            csv.append(String.join(",", row.stream().map(this::csvValue).toList())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildXlsx(List<String> headers, List<List<Object>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Export");
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (List<Object> rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < rowData.size(); i++) {
                    row.createCell(i).setCellValue(rowData.get(i) == null ? "" : String.valueOf(rowData.get(i)));
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate XLSX export", e);
        }
    }

    private byte[] buildPdf(String title, List<String> headers, List<List<Object>> rows) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            stream.beginText();
            stream.setFont(boldFont, 14);
            stream.newLineAtOffset(40, 800);
            stream.showText(title);
            stream.setFont(font, 9);
            stream.newLineAtOffset(0, -20);

            List<String> lines = new ArrayList<>();
            lines.add(String.join(" | ", headers));
            for (List<Object> row : rows) {
                lines.add(String.join(" | ", row.stream().map(value -> value == null ? "" : String.valueOf(value)).toList()));
            }

            float y = 780;
            for (String line : lines) {
                for (String wrapped : wrapText(line, 95)) {
                    if (y <= 50) {
                        stream.endText();
                        stream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        stream = new PDPageContentStream(document, page);
                        stream.beginText();
                        stream.setFont(font, 9);
                        stream.newLineAtOffset(40, 800);
                        y = 800;
                    }
                    stream.showText(wrapped);
                    stream.newLineAtOffset(0, -12);
                    y -= 12;
                }
            }

            stream.endText();
            stream.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate PDF export", e);
        }
    }

    private List<String> wrapText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return List.of("");
        }

        List<String> lines = new ArrayList<>();
        String remaining = text;
        while (remaining.length() > maxLength) {
            int breakPoint = remaining.lastIndexOf(' ', maxLength);
            if (breakPoint <= 0) {
                breakPoint = maxLength;
            }
            lines.add(remaining.substring(0, breakPoint));
            remaining = remaining.substring(breakPoint).trim();
        }
        lines.add(remaining);
        return lines;
    }

    private String buildSectionWiseScoreSummary(TestAttempt attempt) {
        if (attempt == null) {
            return "";
        }

        List<TestSection> sections = testSectionRepository.findByTestIdOrderBySectionOrderAsc(attempt.getTest().getId());
        Map<Long, AttemptAnswer> answersByQuestionId = attemptAnswerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), answer -> answer, (a, b) -> b));

        List<String> sectionScores = new ArrayList<>();
        for (TestSection section : sections) {
            BigDecimal sectionScore = BigDecimal.ZERO;
            BigDecimal sectionTotal = BigDecimal.ZERO;

            for (Question question : questionRepository.findBySectionIdOrderByQuestionOrderAsc(section.getId())) {
                sectionTotal = sectionTotal.add(safeNumber(question.getMarks()));
                AttemptAnswer answer = answersByQuestionId.get(question.getId());
                if (!isAttempted(answer)) {
                    continue;
                }
                if (isCorrectAnswer(question, answer)) {
                    sectionScore = sectionScore.add(safeNumber(question.getMarks()));
                } else if (Boolean.TRUE.equals(attempt.getTest().getNegativeMarkingEnabled())) {
                    sectionScore = sectionScore.subtract(safeNumber(attempt.getTest().getNegativeMarkValue()));
                }
            }

            sectionScores.add(section.getTitle() + ": " + decimalText(sectionScore) + "/" + decimalText(sectionTotal));
        }

        return String.join(" | ", sectionScores);
    }

    private String decimalText(BigDecimal value) {
        BigDecimal safeValue = safeNumber(value).stripTrailingZeros();
        return safeValue.scale() < 0 ? safeValue.setScale(0).toPlainString() : safeValue.toPlainString();
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

    private String csvValue(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    public record ExportFile(String fileName, String contentType, byte[] content) {
    }
}
