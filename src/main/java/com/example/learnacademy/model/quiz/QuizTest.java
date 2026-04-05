package com.example.learnacademy.model.quiz;

import com.example.learnacademy.model.Course;
import com.example.learnacademy.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tests")
public class QuizTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "time_limit", nullable = false)
    private Integer timeLimit;

    @Column(name = "total_marks", nullable = false)
    private BigDecimal totalMarks = BigDecimal.ZERO;

    @Column(name = "negative_marking_enabled", nullable = false)
    private Boolean negativeMarkingEnabled = Boolean.FALSE;

    @Column(name = "negative_mark_value", nullable = false)
    private BigDecimal negativeMarkValue = BigDecimal.ZERO;

    @Column(name = "strict_mode_enabled", nullable = false)
    private Boolean strictModeEnabled = Boolean.FALSE;

    @Column(name = "max_tab_switch", nullable = false)
    private Integer maxTabSwitch = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sectionOrder ASC")
    private List<TestSection> sections = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getTimeLimit() { return timeLimit; }
    public BigDecimal getTotalMarks() { return totalMarks; }
    public Boolean getNegativeMarkingEnabled() { return negativeMarkingEnabled; }
    public BigDecimal getNegativeMarkValue() { return negativeMarkValue; }
    public Boolean getStrictModeEnabled() { return strictModeEnabled; }
    public Integer getMaxTabSwitch() { return maxTabSwitch; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<TestSection> getSections() { return sections; }
    public void setId(Long id) { this.id = id; }
    public void setCourse(Course course) { this.course = course; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }
    public void setTotalMarks(BigDecimal totalMarks) { this.totalMarks = totalMarks; }
    public void setNegativeMarkingEnabled(Boolean negativeMarkingEnabled) { this.negativeMarkingEnabled = negativeMarkingEnabled; }
    public void setNegativeMarkValue(BigDecimal negativeMarkValue) { this.negativeMarkValue = negativeMarkValue; }
    public void setStrictModeEnabled(Boolean strictModeEnabled) { this.strictModeEnabled = strictModeEnabled; }
    public void setMaxTabSwitch(Integer maxTabSwitch) { this.maxTabSwitch = maxTabSwitch; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setSections(List<TestSection> sections) { this.sections = sections; }
}
