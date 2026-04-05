package com.example.learnacademy.controller;

import com.example.learnacademy.model.Course;
import com.example.learnacademy.model.User;
import com.example.learnacademy.repository.UserRepository;
import com.example.learnacademy.service.CourseService;
import com.example.learnacademy.service.quiz.QuizService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
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
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CourseController {

    @Autowired
    private CourseService service;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private QuizService quizService;


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
    public Map<String, Object> dashboard(
            @RequestParam(required = false) String type,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        List<Course> courses = service.getAllCourses(userId);
        List<Map<String, Object>> tests = quizService.getAvailableTests(userId);
        List<Map<String, Object>> items = new ArrayList<>();

        if (type == null || type.equalsIgnoreCase("course")) {
            items.addAll(courses.stream().map(this::toCourseDashboardItem).toList());
        }
        if (type == null || type.equalsIgnoreCase("test")) {
            items.addAll(tests);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("userId", userId);
        res.put("courses", courses);
        res.put("tests", tests);
        res.put("items", items);
        res.put("type", type == null ? "all" : type.toLowerCase());

        return res;
    }

    @GetMapping("/mylearning")
    public Map<String, Object> myLearning(
            @RequestParam(required = false) String type,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        List<Course> courses = service.myLearning(userId);
        List<Map<String, Object>> tests = quizService.getMyTests(userId);
        List<Map<String, Object>> items = new ArrayList<>();

        if (type == null || type.equalsIgnoreCase("course")) {
            items.addAll(courses.stream().map(this::toCourseLearningItem).toList());
        }
        if (type == null || type.equalsIgnoreCase("test")) {
            items.addAll(tests);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("userId", userId);
        res.put("courses", courses);
        res.put("tests", tests);
        res.put("items", items);
        res.put("type", type == null ? "all" : type.toLowerCase());
        return res;
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
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        String type = body.get("type") == null ? "course" : String.valueOf(body.get("type")).toLowerCase();

        if ("test".equals(type)) {
            Long testId = Long.valueOf(String.valueOf(body.get("testId") != null ? body.get("testId") : body.get("id")));
            quizService.enrollInTest(userId, testId);
        } else {
            Long courseId = Long.valueOf(String.valueOf(body.get("courseId") != null ? body.get("courseId") : body.get("id")));
            service.enroll(userId, courseId);
        }

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

        Map<String, Object> courseDashboard = service.instructorDashboard(userId);
        Map<String, Object> testDashboard = quizService.instructorTestDashboard(userId);

        Map<String, Object> result = new LinkedHashMap<>(courseDashboard);
        result.putAll(testDashboard);
        return result;
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
    public Object instructorCourseReport(
            @RequestParam(required = false) String type,
            Authentication authentication) {
        Long userId = getUserId(authentication);

        if ("course".equalsIgnoreCase(type)) {
            return service.getInstructorCourseReports(userId);
        }
        if ("test".equalsIgnoreCase(type)) {
            return quizService.getInstructorTestReports(userId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courses", service.getInstructorCourseReports(userId));
        result.put("tests", quizService.getInstructorTestReports(userId));
        result.put("type", "all");
        return result;
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
        List<Long> courseIds = body.get("courseIds");
        List<Long> testIds = body.get("testIds");

        if ((courseIds == null || courseIds.isEmpty()) && (testIds == null || testIds.isEmpty())) {
            CourseService.ExportFile exportFile = service.exportInstructorCourseReports(userId, courseIds, format);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFile.fileName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, exportFile.contentType())
                    .body(exportFile.content());
        }

        if ((courseIds == null || courseIds.isEmpty()) && testIds != null && !testIds.isEmpty()) {
            QuizService.ExportFile exportFile = quizService.exportInstructorTestReports(userId, testIds, format);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFile.fileName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, exportFile.contentType())
                    .body(exportFile.content());
        }

        if ((testIds == null || testIds.isEmpty()) && courseIds != null && !courseIds.isEmpty()) {
            CourseService.ExportFile exportFile = service.exportInstructorCourseReports(userId, courseIds, format);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFile.fileName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, exportFile.contentType())
                    .body(exportFile.content());
        }

        byte[] mergedFile = buildCombinedReportExport(
                service.getInstructorCourseReports(userId),
                quizService.getInstructorTestReports(userId),
                courseIds,
                testIds,
                format
        );
        String extension = format.toLowerCase(Locale.ROOT);
        String contentType = switch (extension) {
            case "csv" -> "text/csv";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"combined-report." + extension + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(mergedFile);
    }

    private byte[] buildCombinedReportExport(
            List<Map<String, Object>> courseReports,
            List<Map<String, Object>> testReports,
            List<Long> courseIds,
            List<Long> testIds,
            String format
    ) {
        List<List<Object>> rows = new ArrayList<>();
        List<String> headers = List.of("Type", "ItemId", "Title", "UserId", "UserName", "UserEmail", "Meta1", "Meta2", "Meta3");

        for (Map<String, Object> report : courseReports) {
            if (courseIds != null && !courseIds.isEmpty() && !courseIds.contains(((Number) report.get("courseId")).longValue())) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = (List<Map<String, Object>>) report.get("enrolledUsers");
            for (Map<String, Object> user : users) {
                rows.add(List.of(
                        "course",
                        report.get("courseId"),
                        report.get("courseName"),
                        user.get("userId"),
                        user.get("userName"),
                        user.get("userEmail"),
                        "Completed: " + user.get("totalCompletedLessons"),
                        "EnrolledAt: " + user.get("enrolledAt"),
                        "Sections: " + user.get("sections")
                ));
            }
        }

        for (Map<String, Object> report : testReports) {
            if (testIds != null && !testIds.isEmpty() && !testIds.contains(((Number) report.get("testId")).longValue())) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> users = (List<Map<String, Object>>) report.get("enrolledUsers");
            for (Map<String, Object> user : users) {
                rows.add(List.of(
                        "test",
                        report.get("testId"),
                        report.get("testTitle"),
                        user.get("userId"),
                        user.get("userName"),
                        user.get("userEmail"),
                        "BestScore: " + user.get("bestScore"),
                        "Attempts: " + user.get("attemptsCount"),
                        "Status: " + user.get("latestAttemptStatus")
                ));
            }
        }

        return switch (format.toLowerCase(Locale.ROOT)) {
            case "csv" -> buildCsv(headers, rows);
            case "xlsx" -> buildXlsx(headers, rows);
            case "pdf" -> buildPdf("Combined Report", headers, rows);
            default -> throw new RuntimeException("Unsupported format");
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
            XSSFSheet sheet = workbook.createSheet("Combined Report");
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
            throw new RuntimeException("Unable to generate XLSX export");
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
            throw new RuntimeException("Unable to generate PDF export");
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

    private String csvValue(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private Map<String, Object> toCourseDashboardItem(Course course) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", course.getId());
        item.put("title", course.getTitle());
        item.put("description", course.getDescription());
        item.put("category", course.getCategory());
        item.put("imageUrl", course.getImageUrl());
        item.put("price", course.getPrice());
        item.put("rating", course.getRating());
        item.put("type", "course");
        item.put("enrolled", false);
        return item;
    }

    private Map<String, Object> toCourseLearningItem(Course course) {
        Map<String, Object> item = toCourseDashboardItem(course);
        item.put("enrolled", true);
        return item;
    }
}
