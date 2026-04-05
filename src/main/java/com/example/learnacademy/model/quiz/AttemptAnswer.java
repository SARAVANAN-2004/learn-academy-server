package com.example.learnacademy.model.quiz;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "answers")
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuestionOption selectedOption;

    @Column(name = "text_answer", columnDefinition = "text")
    private String textAnswer;

    @Column(name = "is_marked_for_review", nullable = false)
    private Boolean isMarkedForReview = Boolean.FALSE;

    @Column(name = "is_answered", nullable = false)
    private Boolean isAnswered = Boolean.FALSE;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @PrePersist
    @PreUpdate
    public void preSave() {
        savedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public TestAttempt getAttempt() { return attempt; }
    public Question getQuestion() { return question; }
    public QuestionOption getSelectedOption() { return selectedOption; }
    public String getTextAnswer() { return textAnswer; }
    public Boolean getIsMarkedForReview() { return isMarkedForReview; }
    public Boolean getIsAnswered() { return isAnswered; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setId(Long id) { this.id = id; }
    public void setAttempt(TestAttempt attempt) { this.attempt = attempt; }
    public void setQuestion(Question question) { this.question = question; }
    public void setSelectedOption(QuestionOption selectedOption) { this.selectedOption = selectedOption; }
    public void setTextAnswer(String textAnswer) { this.textAnswer = textAnswer; }
    public void setIsMarkedForReview(Boolean markedForReview) { isMarkedForReview = markedForReview; }
    public void setIsAnswered(Boolean answered) { isAnswered = answered; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
}
