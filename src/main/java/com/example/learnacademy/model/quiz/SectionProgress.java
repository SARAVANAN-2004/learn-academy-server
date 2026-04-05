package com.example.learnacademy.model.quiz;

import jakarta.persistence.*;

@Entity
@Table(name = "section_progress")
public class SectionProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private TestSection section;

    @Column(nullable = false)
    private Boolean completed = Boolean.FALSE;

    public Long getId() { return id; }
    public TestAttempt getAttempt() { return attempt; }
    public TestSection getSection() { return section; }
    public Boolean getCompleted() { return completed; }
    public void setId(Long id) { this.id = id; }
    public void setAttempt(TestAttempt attempt) { this.attempt = attempt; }
    public void setSection(TestSection section) { this.section = section; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
}
