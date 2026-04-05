package com.example.learnacademy.model.quiz;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sections")
public class TestSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private QuizTest test;

    @Column(nullable = false)
    private String title;

    @Column(name = "section_order", nullable = false)
    private Integer sectionOrder;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<Question> questions = new ArrayList<>();

    public Long getId() { return id; }
    public QuizTest getTest() { return test; }
    public String getTitle() { return title; }
    public Integer getSectionOrder() { return sectionOrder; }
    public List<Question> getQuestions() { return questions; }
    public void setId(Long id) { this.id = id; }
    public void setTest(QuizTest test) { this.test = test; }
    public void setTitle(String title) { this.title = title; }
    public void setSectionOrder(Integer sectionOrder) { this.sectionOrder = sectionOrder; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}
