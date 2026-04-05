package com.example.learnacademy.model.quiz;

import com.example.learnacademy.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_attempts")
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private QuizTest test;

    @Column(nullable = false)
    private BigDecimal score = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "tab_switch_count", nullable = false)
    private Integer tabSwitchCount = 0;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttemptAnswer> answers = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public QuizTest getTest() { return test; }
    public BigDecimal getScore() { return score; }
    public AttemptStatus getStatus() { return status; }
    public Integer getTabSwitchCount() { return tabSwitchCount; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public List<AttemptAnswer> getAnswers() { return answers; }
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setTest(QuizTest test) { this.test = test; }
    public void setScore(BigDecimal score) { this.score = score; }
    public void setStatus(AttemptStatus status) { this.status = status; }
    public void setTabSwitchCount(Integer tabSwitchCount) { this.tabSwitchCount = tabSwitchCount; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public void setAnswers(List<AttemptAnswer> answers) { this.answers = answers; }
}
