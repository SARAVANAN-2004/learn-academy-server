package com.example.learnacademy.model.quiz;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private TestSection section;

    @Column(name = "question_text", nullable = false, columnDefinition = "text")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(nullable = false)
    private BigDecimal marks = BigDecimal.ONE;

    @Column(name = "correct_answer", columnDefinition = "text")
    private String correctAnswer;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionOption> options = new ArrayList<>();

    public Long getId() { return id; }
    public TestSection getSection() { return section; }
    public String getQuestionText() { return questionText; }
    public QuestionType getQuestionType() { return questionType; }
    public BigDecimal getMarks() { return marks; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
    public Integer getQuestionOrder() { return questionOrder; }
    public List<QuestionOption> getOptions() { return options; }
    public void setId(Long id) { this.id = id; }
    public void setSection(TestSection section) { this.section = section; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
    public void setMarks(BigDecimal marks) { this.marks = marks; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
    public void setOptions(List<QuestionOption> options) { this.options = options; }
}
