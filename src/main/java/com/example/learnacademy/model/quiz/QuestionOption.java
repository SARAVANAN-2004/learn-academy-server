package com.example.learnacademy.model.quiz;

import jakarta.persistence.*;

@Entity
@Table(name = "options")
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_text", nullable = false, columnDefinition = "text")
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = Boolean.FALSE;

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public String getOptionText() { return optionText; }
    public Boolean getIsCorrect() { return isCorrect; }
    public void setId(Long id) { this.id = id; }
    public void setQuestion(Question question) { this.question = question; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
    public void setIsCorrect(Boolean correct) { isCorrect = correct; }
}
