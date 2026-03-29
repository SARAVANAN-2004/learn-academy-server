package com.example.learnacademy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id", "lesson_id"})
)
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "lesson_id")
    private String lessonId;

    private Boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getLessonId() {
        return lessonId;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}