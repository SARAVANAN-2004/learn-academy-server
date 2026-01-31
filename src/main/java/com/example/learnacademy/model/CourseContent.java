package com.example.learnacademy.model;

import jakarta.persistence.*;

@Entity
@Table(name = "course_contents")
public class CourseContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "course_id")
    private Integer courseId;

    @Column(columnDefinition = "jsonb")
    private String content;

    // ===== Getters =====

    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public String getContent() {
        return content;
    }

    // ===== Setters =====

    public void setId(Integer id) {
        this.id = id;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
