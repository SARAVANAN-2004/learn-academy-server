package com.example.learnacademy.model.quiz;

import com.example.learnacademy.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "leaderboard")
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private QuizTest test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal score = BigDecimal.ZERO;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    public Long getId() { return id; }
    public QuizTest getTest() { return test; }
    public User getUser() { return user; }
    public BigDecimal getScore() { return score; }
    public Integer getRank() { return rank; }
    public void setId(Long id) { this.id = id; }
    public void setTest(QuizTest test) { this.test = test; }
    public void setUser(User user) { this.user = user; }
    public void setScore(BigDecimal score) { this.score = score; }
    public void setRank(Integer rank) { this.rank = rank; }
}
